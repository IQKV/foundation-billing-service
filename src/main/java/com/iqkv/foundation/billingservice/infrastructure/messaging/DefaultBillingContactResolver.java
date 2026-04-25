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

package com.iqkv.foundation.billingservice.infrastructure.messaging;

import com.iqkv.foundation.billingservice.infrastructure.config.BillingConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link BillingContactResolver}.
 *
 * <p>Applies the following fallback chain:
 * <ol>
 *   <li>{@code ownerEmail} from the event, if present and non-blank</li>
 *   <li>{@code iqkv.billing.default-contact-email} configuration property, if set and non-blank</li>
 *   <li>{@code null} — allows Stripe customer creation with no email</li>
 * </ol>
 */
@Component
public class DefaultBillingContactResolver implements BillingContactResolver {

  private final BillingConfigurationProperties billingProps;

  public DefaultBillingContactResolver(final BillingConfigurationProperties billingProps) {
    this.billingProps = billingProps;
  }

  @Override
  public String resolveBillingContact(final TenantEvent event) {
    // Priority 1: ownerEmail from the event
    final String ownerEmail = event.getOwnerEmail();
    if (ownerEmail != null && !ownerEmail.isBlank()) {
      return ownerEmail;
    }

    // Priority 2: configured default contact email
    final String defaultEmail = billingProps.defaultContactEmail();
    if (defaultEmail != null && !defaultEmail.isBlank()) {
      return defaultEmail;
    }

    // Priority 3: null — Stripe customer creation proceeds without email
    return null;
  }
}
