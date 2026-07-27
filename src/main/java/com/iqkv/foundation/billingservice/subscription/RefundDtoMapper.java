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

/**
 * Maps {@link Refund} domain objects to API response DTOs.
 */
public final class RefundDtoMapper {

  private RefundDtoMapper() {
  }

  /**
   * Maps a {@link Refund} to the rich {@link SubscriptionDtos.AdminRefundResponse}.
   * Used by {@code RefundAdminRestResource} (PLATFORM_ADMIN surface).
   */
  public static SubscriptionDtos.AdminRefundResponse toAdminResponse(final Refund refund) {
    return new SubscriptionDtos.AdminRefundResponse(
        refund.getId(),
        refund.getTenantKey(),
        refund.getExternalRefundId(),
        refund.getExternalPaymentId(),
        refund.getExternalCustomerId(),
        refund.getAmount(),
        refund.getCurrency(),
        refund.getStatus(),
        refund.getOccurredAt(),
        refund.getGatewayType(),
        refund.getSubjectType(),
        refund.getSubjectKey(),
        refund.getCreatedAt(),
        refund.getUpdatedAt()
    );
  }
}
