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

/**
 * Payment gateway abstraction layer (Strategy pattern + Hexagonal Architecture).
 *
 * <p>This package provides a gateway-agnostic interface for payment operations,
 * allowing the billing service to support multiple payment providers (Stripe, PayPal, etc.)
 * without coupling business logic to specific gateway implementations.
 *
 * <h2>Package Structure:</h2>
 * <ul>
 *   <li>{@code port} - Port interfaces defining gateway contracts</li>
 *   <li>{@code event} - Gateway-agnostic webhook event models</li>
 *   <li>{@code command} - Gateway-agnostic command models</li>
 * </ul>
 *
 * <h2>Design Pattern:</h2>
 * <p>Combines Strategy pattern (multiple gateway implementations) with Hexagonal Architecture
 * (ports and adapters). The domain layer depends on {@link com.iqkv.foundation.billingservice.gateway.port.PaymentGatewayPort},
 * while infrastructure provides concrete adapters (e.g., {@code StripeGatewayAdapter}).
 */

package com.iqkv.foundation.billingservice.gateway;
