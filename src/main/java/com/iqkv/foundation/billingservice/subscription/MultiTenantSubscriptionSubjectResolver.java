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

package com.iqkv.foundation.billingservice.subscription;

import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.config.PlatformConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * {@link SubscriptionSubjectResolver} for {@code MULTI_TENANT} mode.
 *
 * <p>Subscriptions are scoped to the organization tenant by default:
 * {@code subject_type = TENANT}, {@code subject_key = tenantKey}.
 *
 * <p>Exception: when the caller's token is scoped to the hidden platform workspace
 * ({@code iqkv.platform.platform-workspace-key}), the user has no organization and is billed
 * as an individual: {@code subject_type = USER}, {@code subject_key = userId}. This enables
 * personal subscriptions (GitHub-style) in multi-tenant deployments.
 */
@Primary
@Component
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "MULTI_TENANT", matchIfMissing = true)
public class MultiTenantSubscriptionSubjectResolver implements SubscriptionSubjectResolver {

  private final String platformWorkspaceKey;

  public MultiTenantSubscriptionSubjectResolver(final PlatformConfigurationProperties platformProperties) {
    this.platformWorkspaceKey = platformProperties.platformWorkspaceKey();
  }

  @Override
  public SubscriptionSubject resolveSubject(final String tenantKey, final UUID userId) {
    if (userId != null && platformWorkspaceKey.equals(tenantKey)) {
      return new SubscriptionSubject(SubjectType.USER, userId.toString());
    }
    return new SubscriptionSubject(SubjectType.TENANT, tenantKey);
  }
}
