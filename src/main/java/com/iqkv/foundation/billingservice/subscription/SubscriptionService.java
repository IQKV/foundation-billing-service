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

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Application service for subscription queries.
 *
 * <p>Subscription state is a local cache populated by {@code PaymentWebhookRestResource}
 * on Stripe webhook events — no payment gateway round-trips are made here.
 *
 * <p>Subject-scoped queries use {@link SubscriptionSubjectResolver} to determine whether
 * subscriptions are keyed by tenant (multi-tenant mode) or by user (single-tenant mode).
 */
@Service
public class SubscriptionService {

  private final SubscriptionMapper subscriptionMapper;
  private final SubscriptionSubjectResolver subjectResolver;

  public SubscriptionService(final SubscriptionMapper subscriptionMapper,
                             final SubscriptionSubjectResolver subjectResolver) {
    this.subscriptionMapper = subscriptionMapper;
    this.subjectResolver = subjectResolver;
  }

  // ─── Self-service ──────────────────────────────────────────────────────────

  /**
   * Returns the most recent active subscription for the given tenant.
   *
   * @throws ResourceNotFoundException if no active subscription exists
   */
  public Subscription getActiveByTenantKey(final String tenantKey) {
    return subscriptionMapper.findActiveByTenantKey(tenantKey)
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for tenantKey=" + tenantKey));
  }

  /**
   * Returns all subscription records for the given tenant, ordered by {@code created_at DESC}.
   */
  public List<Subscription> getAllByTenantKey(final String tenantKey) {
    return subscriptionMapper.findAllByTenantKey(tenantKey);
  }

  /**
   * Returns the active subscription for the resolved subject (tenant or user depending on mode).
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   * @throws ResourceNotFoundException if no active subscription exists for the subject
   */
  public Subscription getActiveBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findActiveBySubject(subject.type().name(), subject.key())
        .orElseThrow(() -> new ResourceNotFoundException(
            "No active subscription found for subject type=" + subject.type() + " key=" + subject.key()));
  }

  /**
   * Returns all subscriptions for the resolved subject (tenant or user depending on mode),
   * ordered by {@code created_at DESC}.
   *
   * @param tenantKey the tenant key from request context
   * @param userId    the user ID from JWT claims
   */
  public List<Subscription> getAllBySubject(final String tenantKey, final UUID userId) {
    final SubscriptionSubject subject = subjectResolver.resolveSubject(tenantKey, userId);
    return subscriptionMapper.findBySubject(subject.type().name(), subject.key());
  }

  // ─── Platform admin ────────────────────────────────────────────────────────

  /**
   * Returns a paginated, sorted, and optionally filtered list of subscriptions.
   *
   * @param query encapsulates pagination, sort, and optional filters
   * @return a {@link SubscriptionDtos.PagedSubscriptionResponse}
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.PagedSubscriptionResponse listSubscriptions(
      final SubscriptionDtos.SubscriptionListQuery query) {
    final String safeSortBy = java.util.Set.of("tenantKey", "planId", "status", "updatedAt", "createdAt")
        .contains(query.sortBy()) ? query.sortBy() : "createdAt";
    final String safeSortDir = "asc".equalsIgnoreCase(query.sortDir()) ? "asc" : "desc";

    final String safeSearch = (query.search() != null && !query.search().isBlank())
        ? query.search().strip() : null;
    final String safeStatus = (query.status() != null && !query.status().isBlank())
        ? query.status().strip() : null;
    final String safeTenantKey = (query.tenantKey() != null && !query.tenantKey().isBlank())
        ? query.tenantKey().strip() : null;

    final int offset = query.page() * query.size();
    final var subscriptions = subscriptionMapper.findAll(
            query.size(), offset, safeSortBy, safeSortDir, safeSearch, safeStatus, safeTenantKey)
        .stream()
        .map(SubscriptionDtoMapper::toAdminResponse)
        .toList();
    final long total = subscriptionMapper.countAll(safeSearch, safeStatus, safeTenantKey);
    final int totalPages = (int) Math.ceil((double) total / query.size());
    return new SubscriptionDtos.PagedSubscriptionResponse(subscriptions, query.page(), query.size(), total, totalPages);
  }

  /**
   * Retrieves a subscription by its UUID.
   *
   * @param id the subscription's unique identifier
   * @return the subscription as a {@link SubscriptionDtos.AdminSubscriptionResponse}
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public SubscriptionDtos.AdminSubscriptionResponse getSubscriptionById(final UUID id) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Partially updates a subscription (PATCH semantics).
   * Only non-null fields in the request are applied.
   *
   * @param id      the subscription's unique identifier
   * @param request partial update payload; any field may be {@code null}
   * @return the updated subscription as a {@link SubscriptionDtos.AdminSubscriptionResponse}
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional
  public SubscriptionDtos.AdminSubscriptionResponse patchSubscription(
      final UUID id,
      final SubscriptionDtos.AdminUpdateSubscriptionRequest request) {
    final Subscription subscription = subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));

    if (request.status() != null) {
      subscription.setStatus(request.status());
      if ("canceled".equalsIgnoreCase(request.status()) && subscription.getCanceledAt() == null) {
        subscription.setCanceledAt(java.time.Instant.now());
      }
    }

    if (request.currentPeriodEnd() != null) {
      subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
    }

    if (request.cancelAtPeriodEnd() != null) {
      subscription.setCancelAtPeriodEnd(request.cancelAtPeriodEnd());
    }

    subscription.setUpdatedAt(java.time.LocalDateTime.now());
    subscriptionMapper.update(subscription);

    return SubscriptionDtoMapper.toAdminResponse(subscription);
  }

  /**
   * Permanently deletes a subscription.
   *
   * @param id the subscription's unique identifier
   * @throws ResourceNotFoundException if no subscription exists with the given ID
   */
  @org.springframework.transaction.annotation.Transactional
  public void deleteSubscription(final UUID id) {
    subscriptionMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    subscriptionMapper.deleteById(id);
  }
}
