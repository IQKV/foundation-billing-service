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

package com.iqkv.foundation.billingservice.infrastructure.config;

import jakarta.validation.constraints.NotBlank;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Lemon Squeezy payment gateway.
 *
 * <p>Bound from {@code iqkv.lemon-squeezy.*} in YAML. The record is only registered
 * when the active gateway is {@link GatewayType#LEMON_SQUEEZY} (enforced by
 * {@link ConditionalOnGateway}).
 *
 * <p>Required environment variables when using Lemon Squeezy:
 * <ul>
 *   <li>{@code LEMON_SQUEEZY_API_KEY} — API key from the LS dashboard</li>
 *   <li>{@code LEMON_SQUEEZY_STORE_ID} — numeric store ID from the LS dashboard</li>
 *   <li>{@code LEMON_SQUEEZY_WEBHOOK_SECRET} — signing secret configured per webhook endpoint</li>
 *   <li>{@code LEMON_SQUEEZY_PORTAL_RETURN_URL} — redirect URL after the customer leaves the portal</li>
 * </ul>
 */
@Validated
@ConditionalOnGateway(GatewayType.LEMON_SQUEEZY)
@ConfigurationProperties(prefix = "iqkv.lemon-squeezy")
public record LemonSqueezyConfigurationProperties(
    @NotBlank String apiKey,
    @NotBlank String storeId,
    @NotBlank String webhookSecret,
    @NotBlank String portalReturnUrl
) {
}
