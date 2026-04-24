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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for tenant subscription queries.
 *
 * <p>Subscription data is a local cache of Stripe state — no payment gateway
 * round-trips are made. Requires {@code TENANT_OWNER} authority.
 */
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
public class SubscriptionRestResource {

  private final SubscriptionService subscriptionService;

  public SubscriptionRestResource(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  /**
   * Returns the active subscription for the given tenant.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with the active subscription, or 404 if none exists
   */
  @GetMapping("/{tenantKey}/active")
  public ResponseEntity<SubscriptionResponse> getActive(@PathVariable String tenantKey) {
    final var subscription = subscriptionService.getActiveByTenantKey(tenantKey);
    return ResponseEntity.ok(SubscriptionResponse.from(subscription));
  }

  /**
   * Returns all subscriptions for the given tenant, ordered by {@code created_at DESC}.
   *
   * @param tenantKey the tenant identifier
   * @return 200 with the list (may be empty)
   */
  @GetMapping("/{tenantKey}")
  public ResponseEntity<List<SubscriptionResponse>> getAll(@PathVariable String tenantKey) {
    final var subscriptions = subscriptionService.getAllByTenantKey(tenantKey)
        .stream()
        .map(SubscriptionResponse::from)
        .toList();
    return ResponseEntity.ok(subscriptions);
  }
}
