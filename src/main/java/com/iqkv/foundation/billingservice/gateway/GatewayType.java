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

package com.iqkv.foundation.billingservice.gateway;

/**
 * Enumeration of supported payment gateway types.
 *
 * <p>Active gateway is selected via {@code iqkv.payment.gateway.type}.
 * Exactly one adapter bean is registered at startup based on this value.
 */
public enum GatewayType {
  /**
   * Stripe payment gateway (implemented).
   * Products and prices are created programmatically via the Stripe SDK.
   */
  STRIPE,

  /**
   * Lemon Squeezy payment gateway (implemented).
   * Products and variants are managed in the LS dashboard; variant IDs are
   * configured via {@code iqkv.billing.plan-catalog.products.<key>.externalVariantId}.
   */
  LEMON_SQUEEZY

  // Reserved for future implementation:
  // PAYPAL,
  // BRAINTREE
}
