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

package com.iqkv.foundation.billingservice.infrastructure.config;

import jakarta.validation.constraints.NotNull;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for payment gateway selection.
 *
 * <p>Determines which payment gateway adapter is active at runtime.
 * Currently only {@link GatewayType#STRIPE} is implemented.
 *
 * <p>Example configuration:
 * <pre>
 * iqkv:
 *   payment:
 *     gateway:
 *       type: STRIPE
 * </pre>
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.payment.gateway")
public record PaymentGatewayConfigurationProperties(
    @NotNull GatewayType type
) {
}
