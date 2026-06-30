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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTOs for the subscription API surface.
 * All types are immutable records; internal fields (e.g. {@code externalCustomerId}) are excluded.
 */
public final class SubscriptionDtos {

  private SubscriptionDtos() {
  }

  // ─── Self-service DTOs (used by SubscriptionRestResource) ────────────────

  /**
   * Read-only view of a cached subscription record.
   * Excludes internal fields (e.g. {@code externalCustomerId}) from the API surface.
   */
  public record SubscriptionResponse(
      UUID id,
      String tenantKey,
      String externalSubscriptionId,
      String externalCustomerId,
      String status,
      String planId,
      Long quantity,
      Instant trialStart,
      Instant trialEnd,
      boolean isInTrial,
      Long trialDaysLeft,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      boolean cancelAtPeriodEnd,
      Instant canceledAt,
      String gatewayType,
      String externalOrderId) {
  }

  /**
   * Request body for creating a checkout session.
   */
  public record CreateCheckoutSessionRequest(
      String planCode,
      String successUrl,
      String cancelUrl,
      Integer trialPeriodDays,
      Long quantity,
      Boolean allowPromotionCodes
  ) {
  }

  /**
   * Response containing the checkout session URL.
   */
  public record CheckoutSessionResponse(String checkoutUrl) {
  }

  /**
   * Request body for updating a subscription.
   */
  public record UpdateSubscriptionRequest(
      String planCode,
      Long quantity,
      String prorationBehavior
  ) {
  }

  /**
   * Request body for adjusting the seat count on a PER_SEAT subscription.
   *
   * @param seatCount        new total seat count (must be ≥ 1 and ≤ plan's maxUsers when > 0)
   * @param prorationBehavior Stripe proration mode: {@code "create_prorations"} (default),
   *                          {@code "none"}, or {@code "always_invoice"}
   */
  public record AdjustSeatsRequest(
      @jakarta.validation.constraints.NotNull
      @jakarta.validation.constraints.Min(1)
      Long seatCount,
      String prorationBehavior
  ) {
  }

  /**
   * Request body for creating a refund.
   */
  public record CreateRefundRequest(
      String paymentId,
      Long amount,
      String reason
  ) {
  }

  /**
   * Response containing the refund ID.
   */
  public record RefundResponse(String refundId) {
  }

  /**
   * Rich refund response returned by admin endpoints.
   */
  public record AdminRefundResponse(
      UUID id,
      String tenantKey,
      String externalRefundId,
      String externalPaymentId,
      String externalCustomerId,
      Long amount,
      String currency,
      String status,
      Instant occurredAt,
      String gatewayType,
      java.time.LocalDateTime createdAt,
      java.time.LocalDateTime updatedAt
  ) {
  }

  /**
   * Paginated list of refunds returned by the admin list endpoint.
   */
  public record PagedRefundResponse(
      java.util.List<AdminRefundResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages
  ) {
  }

  /**
   * Query parameters for the admin refund list endpoint.
   */
  public record RefundListQuery(
      @jakarta.validation.constraints.Min(0) Integer page,
      @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) Integer size,
      String sortBy,
      String sortDir,
      String tenantKey
  ) {
    public RefundListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                           final String tenantKey) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "occurredAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.tenantKey = tenantKey;
    }
  }

  // ─── Admin DTOs (used by SubscriptionAdminRestResource) ──────────────────

  /**
   * Rich subscription response returned by admin endpoints.
   * Includes all fields from the {@code Subscription} entity except internal payment gateway IDs.
   */
  public record AdminSubscriptionResponse(
      UUID id,
      String tenantKey,
      String externalSubscriptionId,
      String externalCustomerId,
      String status,
      String planId,
      Long quantity,
      Instant trialStart,
      Instant trialEnd,
      boolean isInTrial,
      Long trialDaysLeft,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      boolean cancelAtPeriodEnd,
      Instant canceledAt,
      String subjectType,
      String subjectKey,
      String gatewayType,
      String externalOrderId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
  }

  /**
   * Request body for partial subscription update (PATCH semantics).
   * Any field may be {@code null} to indicate no change.
   */
  public record AdminUpdateSubscriptionRequest(
      String status,
      Long quantity,
      String planId,
      Instant trialStart,
      Instant trialEnd,
      Instant currentPeriodEnd,
      Boolean cancelAtPeriodEnd
  ) {
  }

  /**
   * Total subscription count returned by the admin count endpoint.
   */
  public record SubscriptionCountResponse(long total) {
  }

  /**
   * Paginated list of subscriptions returned by the admin list endpoint.
   */
  public record PagedSubscriptionResponse(
      java.util.List<AdminSubscriptionResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages
  ) {
  }

  /**
   * Query parameters for the admin subscription list endpoint.
   *
   * <p>Bound from HTTP query string via {@code @ModelAttribute} in the controller.
   * All filter/sort fields are optional — absent values fall back to safe defaults
   * in the service layer.
   *
   * @param page      zero-based page index (default 0)
   * @param size      page size 1–100 (default 20)
   * @param sortBy    sort field: tenantKey | planId | status | createdAt | updatedAt
   * @param sortDir   sort direction: asc | desc
   * @param search    free-text search on tenantKey and planId (case-insensitive)
   * @param status    exact status filter: active | canceled | past_due | trialing | unpaid
   * @param tenantKey filter by specific tenant
   */
  public record SubscriptionListQuery(
      @jakarta.validation.constraints.Min(0) Integer page,
      @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status,
      String tenantKey
  ) {
    /**
     * Canonical defaults applied when the controller binds an empty query string.
     */
    public SubscriptionListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                                 final String search, final String status, final String tenantKey) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "createdAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
      this.tenantKey = tenantKey;
    }
  }
}
