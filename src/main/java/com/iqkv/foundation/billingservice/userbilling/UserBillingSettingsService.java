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

package com.iqkv.foundation.billingservice.userbilling;

import java.util.UUID;

/**
 * Service for managing per-user billing settings in single-tenant mode.
 *
 * <p>In {@code SINGLE_TENANT} mode, billing is scoped to individual users rather than tenants.
 * This service ensures each user has a corresponding Stripe customer and a
 * {@code user_billing_settings} row before any subscription action is performed.
 */
public interface UserBillingSettingsService {

  /**
   * Returns existing user billing settings, or creates them (including a Stripe customer)
   * if none exist yet. Idempotent — safe to call multiple times for the same user.
   *
   * @param userId the user ID from the JWT subject claim
   * @return the existing or newly created {@link UserBillingSettings}
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException
   *         if Stripe customer creation fails
   */
  UserBillingSettings getOrCreateUserBillingSettings(UUID userId);

  /**
   * Creates a customer portal session for the given user.
   *
   * @param userId the user ID
   * @return the URL of the portal session
   * @throws com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException
   *         if no settings or external customer ID exist for the user
   */
  String createPortalSession(UUID userId);
}
