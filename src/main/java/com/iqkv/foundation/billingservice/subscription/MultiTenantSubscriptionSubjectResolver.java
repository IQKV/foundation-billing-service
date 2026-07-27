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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * {@link SubscriptionSubjectResolver} for {@code MULTI_TENANT} mode.
 * Subscriptions are scoped to the tenant: {@code subject_type = TENANT}, {@code subject_key = tenantKey}.
 */
@Primary
@Component
@ConditionalOnProperty(name = "iqkv.platform.rollout-mode", havingValue = "MULTI_TENANT", matchIfMissing = true)
public class MultiTenantSubscriptionSubjectResolver implements SubscriptionSubjectResolver {

  @Override
  public SubscriptionSubject resolveSubject(final String tenantKey, final UUID userId) {
    return new SubscriptionSubject(SubjectType.TENANT, tenantKey);
  }
}
