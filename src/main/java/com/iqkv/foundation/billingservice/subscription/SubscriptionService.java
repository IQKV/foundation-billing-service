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

package com.iqkv.foundation.billingservice.subscription;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Application service for subscription queries.
 *
 * <p>Subscription state is a local cache populated by {@code PaymentWebhookRestResource}
 * on Stripe webhook events — no payment gateway round-trips are made here.
 *
 * <p>Subject-scoped queries use {@link SubscriptionSubjectResolver} to determine whether
 * subscriptions are keyed by tenant (multi-tenant mode) or by user (single-tenant mode).
 */
@Service
public class SubscriptionService {

  private final SubscriptionMapper subscriptionMapper;
  private final SubscriptionSubjectResolver subjectResolver;

  public SubscriptionService(final SubscriptionMapper subscriptionMapper,
                             final SubscriptionSubjectResolver subjectResolver) {
    this.subscriptionMapper = subscriptionMapper;
    this.subjectResolver = subjectResolver;
  }

  /**
   * Returns the most recent active subscription for the given tenant.
   *
   * @throws ResourceNotFoundException if no active subscription exists
   */
  public Subscription getActiveByTenantKey(final String tenantKey) {
    return subscriptionMapper.findActiveByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for tenantKey=" + tenantKey));
  }

  /**
   * Returns all subscription records for the given tenant, ordered by {@code created_at DESC}.
   */
  public List<Subscription> getAllByTenantKey(final String tenantKey) {
    return subscriptionMapper.findAllByTenantKey(tenantKey);
  }

  /**
   * Returns the active subscription for the resolved subject (tenant or user depending on mode).
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   * @throws ResourceNotFoundException if no active subscription exists for the subject
   */
  public Subscription getActiveBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key())
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for subject type=" + subject.type() + " key=" + subject.key()));
  }

  /**
   * Returns all subscriptions for the resolved subject (tenant or user depending on mode),
   * ordered by {@code created_at DESC}.
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   */
  public List<Subscription> getAllBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findBySubject(subject.type().name(), subject.key());
  }
}
