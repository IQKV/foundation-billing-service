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

package com.iqkv.foundation.billingservice.infrastructure.security;

/**
 * JWT claim name constants used by the billing service.
 *
 * <p>These claims are issued by the IAM service ({@code foundation-iam-service}) and consumed
 * here for tenant ownership enforcement and user identification.
 *
 * <p>Only the claims actually read by this service are declared here.
 * The authoritative full list lives in {@code JwtClaimNames} inside {@code foundation-iam-service}.
 */
public final class JwtClaimNames {

  /**
   * Tenant key (8-character NanoID).
   * Used to enforce that a caller can only access resources belonging to their own tenant.
   * Absent on platform-admin tokens — those operate cross-tenant.
   */
  public static final String TENANT_ID = "tenant_id";

  /**
   * Unique user identifier (UUID string).
   * Used to identify the subscription subject when creating or querying subscriptions.
   */
  public static final String USER_ID = "user_id";

  /**
   * Granted authority strings, e.g. {@code ["ROLE_USER", "TENANT_OWNER"]}.
   * Mapped to Spring Security {@code GrantedAuthority} instances by the
   * {@code JwtAuthenticationConverter}.
   */
  public static final String AUTHORITIES = "authorities";

  private JwtClaimNames() {
  }
}
