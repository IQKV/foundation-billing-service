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

package com.iqkv.foundation.billingservice.subscription;

/**
 * Maps {@link Subscription} domain objects to API response DTOs.
 */
public final class SubscriptionDtoMapper {

  private SubscriptionDtoMapper() {}

  public static SubscriptionDtos.SubscriptionResponse toResponse(final Subscription subscription) {
    return new SubscriptionDtos.SubscriptionResponse(
        subscription.getId(),
        subscription.getTenantKey(),
        subscription.getExternalSubscriptionId(),
        subscription.getStatus(),
        subscription.getPlanId(),
        subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd(),
        subscription.isCancelAtPeriodEnd(),
        subscription.getCanceledAt()
    );
  }
}
