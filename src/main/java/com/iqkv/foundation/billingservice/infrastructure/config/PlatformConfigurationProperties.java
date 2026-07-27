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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for platform-wide settings.
 * These settings must be consistent across all core services (IAM, Billing, Gateway).
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.platform")
public record PlatformConfigurationProperties(
    @NotNull RolloutMode rolloutMode
) {

  /**
   * Returns the rollout mode as a string value.
   * Useful for logging and external API responses.
   */
  public String getRolloutModeValue() {
    return rolloutMode.name();
  }

  public boolean isSingleTenant() {
    return rolloutMode == RolloutMode.SINGLE_TENANT;
  }

  public boolean isMultiTenant() {
    return rolloutMode == RolloutMode.MULTI_TENANT;
  }
}
