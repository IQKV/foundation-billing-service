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

import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.subscription.Subscription;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Application service for subscription queries.
 *
 * <p>Subscription state is a local cache populated by {@code PaymentWebhookRestResource}
 * on Stripe webhook events — no payment gateway round-trips are made here.
 */
@Service
public class SubscriptionService {

  private final SubscriptionMapper subscriptionMapper;

  public SubscriptionService(SubscriptionMapper subscriptionMapper) {
    this.subscriptionMapper = subscriptionMapper;
  }

  /**
   * Returns the most recent active subscription for the given tenant.
   *
   * @throws ResourceNotFoundException if no active subscription exists
   */
  public Subscription getActiveByTenantKey(String tenantKey) {
    return subscriptionMapper.findActiveByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for tenantKey=" + tenantKey));
  }

  /**
   * Returns all subscription records for the given tenant, ordered by {@code created_at DESC}.
   */
  public List<Subscription> getAllByTenantKey(String tenantKey) {
    return subscriptionMapper.findAllByTenantKey(tenantKey);
  }
}
