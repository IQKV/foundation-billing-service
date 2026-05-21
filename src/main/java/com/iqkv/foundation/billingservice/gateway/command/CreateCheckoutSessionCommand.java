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

package com.iqkv.foundation.billingservice.gateway.command;

import java.util.Map;

/**
 * Gateway-agnostic command for creating a checkout session.
 *
 * @param customerId          external customer ID (required)
 * @param priceId             external price ID (required)
 * @param successUrl          URL to redirect to after successful payment (required)
 * @param cancelUrl           URL to redirect to after cancelled payment (required)
 * @param trialPeriodDays     number of trial days (optional)
 * @param quantity            quantity of the item (optional)
 * @param allowPromotionCodes whether to allow promotion codes (optional)
 * @param metadata            additional metadata to attach to the session
 */
public record CreateCheckoutSessionCommand(
    String customerId,
    String priceId,
    String successUrl,
    String cancelUrl,
    Integer trialPeriodDays,
    Long quantity,
    Boolean allowPromotionCodes,
    Map<String, String> metadata
) {
  public CreateCheckoutSessionCommand {
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("Customer ID is required");
    }
    if (priceId == null || priceId.isBlank()) {
      throw new IllegalArgumentException("Price ID is required");
    }
    if (successUrl == null || successUrl.isBlank()) {
      throw new IllegalArgumentException("Success URL is required");
    }
    if (cancelUrl == null || cancelUrl.isBlank()) {
      throw new IllegalArgumentException("Cancel URL is required");
    }
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }
}
