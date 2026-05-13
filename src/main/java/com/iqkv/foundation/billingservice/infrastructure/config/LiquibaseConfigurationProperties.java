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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Liquibase settings for the custom {@link LiquibaseRunner} (Spring Boot Liquibase auto-config is excluded).
 *
 * <p>Aligns with {@code iqkv.liquibase.contexts} usage in foundation-iam-service: set {@code demo} on non-production
 * profiles to apply changelog changeSets tagged with {@code context="demo"}.
 */
@Validated
@ConfigurationProperties(prefix = "iqkv.liquibase")
public record LiquibaseConfigurationProperties(
    @NotBlank String changeLog,
    String contexts
) {
}
