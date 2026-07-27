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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "iqkv.stripe")
public record StripeConfigurationProperties(
    // Stripe live/test secret key — must start with sk_live_ or sk_test_
    @NotBlank @Pattern(regexp = "^sk_(live|test)_.+", message = "Stripe secretKey must start with sk_live_ or sk_test_") String secretKey,
    // Stripe webhook signing secret — must start with whsec_
    @NotBlank @Pattern(regexp = "^whsec_.+", message = "Stripe webhookSecret must start with whsec_") String webhookSecret,
    // URL to redirect users back to after they leave the Stripe Customer Portal
    @NotBlank String portalReturnUrl
) {
}
