/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.billingservice.infrastructure.config;

import java.util.Collection;

import com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort;
import com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper;
import com.iqkv.foundation.billingservice.plan.Plan;
import com.iqkv.foundation.billingservice.plan.PlanFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seeds the plan catalog and synchronizes it with the active payment gateway on startup.
 *
 * <p>Iterates over products defined in {@code iqkv.billing.plan-catalog.products},
 * ensuring they exist in the local database (source of truth) and are synchronized
 * with the active payment gateway.
 *
 * <p>For Stripe: creates/updates products and prices programmatically via the Stripe SDK.
 * For Lemon Squeezy: performs a read-only variant verification; the {@code externalVariantId}
 * field from YAML is pre-populated into {@code plan.externalPriceId} before the call so
 * the LS adapter can look up the variant without creating anything.
 *
 * <p>The typed {@link PlanFeatures} from each product schema is serialized to JSON
 * and stored in the {@code feature_set} column for observability. It is never read
 * back from the DB for access decisions — the in-memory {@code PlanFeatureRegistry}
 * is the authoritative source at runtime.
 */
@Component
@Order(10) // Run after Liquibase migrations
public class BillingSeedRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BillingSeedRunner.class);

  private final BillingConfigurationProperties billingProps;
  private final PaymentGatewayPort paymentGatewayPort;
  private final PlanMapper planMapper;
  private final JsonMapper jsonMapper;

  public BillingSeedRunner(final BillingConfigurationProperties billingProps,
                           final PaymentGatewayPort paymentGatewayPort,
                           final PlanMapper planMapper,
                           final JsonMapper jsonMapper) {
    this.billingProps = billingProps;
    this.paymentGatewayPort = paymentGatewayPort;
    this.planMapper = planMapper;
    this.jsonMapper = jsonMapper;
  }

  @Override
  @Transactional
  public void run(final ApplicationArguments args) {
    final Collection<ProductSchema> products = billingProps.planCatalog().products().values();
    if (products.isEmpty()) {
      log.debug("No products found in configuration for seeding");
      return;
    }

    log.info("Seeding {} products from configuration to plan catalog", products.size());

    for (final ProductSchema schema : products) {
      try {
        syncProduct(schema);
      } catch (final Exception e) {
        log.error("Failed to seed/sync product {}: {}", schema.planCode(), e.getMessage(), e);
      }
    }
  }

  private void syncProduct(final ProductSchema schema) {
    final Plan plan = planMapper.findByPlanCode(schema.planCode())
        .orElseGet(() -> {
          final Plan newPlan = new Plan();
          newPlan.setPlanCode(schema.planCode());
          log.debug("Plan {} not found in local catalog, creating new entry", schema.planCode());
          return newPlan;
        });

    final PlanFeatures features = schema.features() != null ? schema.features() : PlanFeatures.NONE;

    plan.setDisplayName(schema.displayName());
    plan.setDescription(schema.description());
    plan.setBillingPeriod(schema.billingPeriod());
    plan.setPriceMinor(schema.priceMinor());
    plan.setCurrency(schema.currency());
    plan.setFeatureSet(serializeFeatures(features, schema.planCode()));
    plan.setScope(schema.scope());
    plan.setActive(schema.active() != null ? schema.active() : Boolean.TRUE);
    plan.setTrialPeriodDays(schema.trialPeriodDays() != null && schema.trialPeriodDays() > 0
        ? schema.trialPeriodDays() : null);
    plan.setPricingModel(schema.effectivePricingModel().name());

    // For Lemon Squeezy: pre-populate externalPriceId from the YAML-configured variant ID
    // so the LS adapter can verify it without creating a new variant.
    // For Stripe: externalPriceId is managed by the adapter itself (set to null here if blank).
    if (schema.externalVariantId() != null && !schema.externalVariantId().isBlank()
        && (plan.getExternalPriceId() == null || plan.getExternalPriceId().isBlank())) {
      plan.setExternalPriceId(schema.externalVariantId());
      log.debug("Pre-populated externalPriceId from externalVariantId for plan {}", schema.planCode());
    }

    final boolean isNew = plan.getId() == null;
    if (isNew) {
      planMapper.insert(plan);
    } else {
      planMapper.update(plan);
    }

    // Capture state before sync to detect changes made by the gateway adapter
    final String priceIdBefore = plan.getExternalPriceId();
    final String productIdBefore = plan.getExternalProductId();

    log.debug("Synchronizing plan {} with payment gateway", plan.getPlanCode());
    paymentGatewayPort.syncProduct(plan);

    // Only persist if the adapter actually changed the external IDs (avoids spurious UPDATE).
    // For LS the read-only adapter returns the existing IDs unchanged — no UPDATE issued.
    final boolean externalIdsChanged =
        !java.util.Objects.equals(plan.getExternalPriceId(), priceIdBefore)
        || !java.util.Objects.equals(plan.getExternalProductId(), productIdBefore);

    if (externalIdsChanged) {
      planMapper.update(plan);
      log.debug("Persisted updated external IDs for plan {}", plan.getPlanCode());
    }

    // Warn when LS gateway is active and a plan still has no variant ID configured
    if (plan.getExternalPriceId() == null || plan.getExternalPriceId().isBlank()) {
      log.warn("Plan '{}' has no externalPriceId after gateway sync. "
               + "If using Lemon Squeezy, set 'externalVariantId' in "
               + "iqkv.billing.plan-catalog.products.{} before going live.",
          plan.getPlanCode(), schema.planCode());
    }

    log.info("Successfully seeded and synchronized plan: {}", plan.getPlanCode());
  }

  private String serializeFeatures(final PlanFeatures features, final String planCode) {
    try {
      return jsonMapper.writeValueAsString(features);
    } catch (final Exception e) {
      log.warn("Failed to serialize PlanFeatures for plan {}, storing null: {}", planCode, e.getMessage());
      return null;
    }
  }
}
