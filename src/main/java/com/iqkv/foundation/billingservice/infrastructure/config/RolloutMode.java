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

/**
 * Defines the platform-wide operational mode.
 * This mode must be consistent across all core services (IAM, Billing, Gateway).
 */
public enum RolloutMode {
  /**
   * Multi-tenant mode: each user signup creates a new tenant.
   * Subscriptions are scoped to tenants.
   */
  MULTI_TENANT,

  /**
   * Single-tenant mode: all users join a pre-provisioned default tenant.
   * Subscriptions are scoped to individual users.
   */
  SINGLE_TENANT
}
