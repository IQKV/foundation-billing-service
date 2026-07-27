/*
 * Copyright 2026 iQKV Foundation Team.
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
 * Gateway-agnostic command for creating a refund.
 *
 * @param paymentId          external payment ID (PaymentIntent ID or Charge ID) (required)
 * @param externalCustomerId external customer ID (optional, used for ownership verification)
 * @param amount             amount to refund in cents (optional, null for full refund)
 * @param reason             reason for refund (optional)
 * @param metadata           additional metadata to attach to the refund
 */
public record CreateRefundCommand(
    String paymentId,
    String externalCustomerId,
    Long amount,
    String reason,
    Map<String, String> metadata
) {
  public CreateRefundCommand {
    if (paymentId == null || paymentId.isBlank()) {
      throw new IllegalArgumentException("Payment ID is required");
    }
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }

  public CreateRefundCommand(final String paymentId, final Long amount, final String reason,
                             final Map<String, String> metadata) {
    this(paymentId, null, amount, reason, metadata);
  }
}
