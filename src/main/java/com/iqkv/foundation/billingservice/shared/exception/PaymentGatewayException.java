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

package com.iqkv.foundation.billingservice.shared.exception;

/**
 * Thrown when a call to the payment gateway (Stripe) fails.
 * Mapped to HTTP 502 Bad Gateway by the global exception handler.
 */
public class PaymentGatewayException extends BillingServiceException {

  public PaymentGatewayException(String message) {
    super(message);
  }

  public PaymentGatewayException(String message, Throwable cause) {
    super(message, cause);
  }
}
