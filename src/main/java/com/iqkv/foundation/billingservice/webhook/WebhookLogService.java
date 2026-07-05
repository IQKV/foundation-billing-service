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

package com.iqkv.foundation.billingservice.webhook;

import java.util.List;
import java.util.UUID;

import com.iqkv.foundation.billingservice.infrastructure.persistence.WebhookLogMapper;
import com.iqkv.foundation.billingservice.shared.exception.ResourceNotFoundException;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubject;
import com.iqkv.foundation.billingservice.subscription.SubscriptionSubjectResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for webhook log queries (admin-only and tenant-owner).
 */
@Service
public class WebhookLogService {

  private final WebhookLogMapper webhookLogMapper;
  private final SubscriptionSubjectResolver subscriptionSubjectResolver;

  public WebhookLogService(final WebhookLogMapper webhookLogMapper, final SubscriptionSubjectResolver subscriptionSubjectResolver) {
    this.webhookLogMapper = webhookLogMapper;
    this.subscriptionSubjectResolver = subscriptionSubjectResolver;
  }

  public WebhookLogDtos.AdminWebhookLogResponse getById(UUID id) {
    return webhookLogMapper.findById(id)
        .map(WebhookLogDtoMapper::toAdminResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Webhook log not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public WebhookLogDtos.PagedWebhookLogResponse listWebhookLogs(WebhookLogDtos.WebhookLogListQuery query) {
    final int offset = query.page() * query.size();

    final List<WebhookLogDtos.AdminWebhookLogResponse> content = webhookLogMapper
        .findAll(query.size(), offset, query.sortBy(), query.sortDir(), query.search(), query.status(), query.tenantKey())
        .stream()
        .map(WebhookLogDtoMapper::toAdminResponse)
        .toList();

    final long total = webhookLogMapper.countAll(query.search(), query.status(), query.tenantKey());
    final int totalPages = (int) Math.ceil((double) total / query.size());

    return new WebhookLogDtos.PagedWebhookLogResponse(content, query.page(), query.size(), total, totalPages);
  }

  public WebhookLogDtos.AdminWebhookLogResponse getByIdForSubject(UUID id, String tenantKey, UUID userId) {
    final SubscriptionSubject subject = subscriptionSubjectResolver.resolveSubject(tenantKey, userId);
    final String subjectKey = subject.key();
    return webhookLogMapper.findById(id)
        .filter(log -> subjectKey.equals(log.getTenantKey()))
        .map(WebhookLogDtoMapper::toAdminResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Webhook log not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public WebhookLogDtos.PagedWebhookLogResponse listWebhookLogsForSubject(
      WebhookLogDtos.WebhookLogListQuery query, String tenantKey, UUID userId) {
    final SubscriptionSubject subject = subscriptionSubjectResolver.resolveSubject(tenantKey, userId);
    final String subjectKey = subject.key();
    final int offset = query.page() * query.size();

    final List<WebhookLogDtos.AdminWebhookLogResponse> content = webhookLogMapper
        .findAll(query.size(), offset, query.sortBy(), query.sortDir(), query.search(), query.status(), subjectKey)
        .stream()
        .map(WebhookLogDtoMapper::toAdminResponse)
        .toList();

    final long total = webhookLogMapper.countAll(query.search(), query.status(), subjectKey);
    final int totalPages = (int) Math.ceil((double) total / query.size());

    return new WebhookLogDtos.PagedWebhookLogResponse(content, query.page(), query.size(), total, totalPages);
  }
}
