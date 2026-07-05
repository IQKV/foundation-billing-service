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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for webhook log API surface.
 */
public final class WebhookLogDtos {

  private WebhookLogDtos() {
  }

  /**
   * Rich webhook log response returned by admin endpoints.
   */
  public record AdminWebhookLogResponse(
      UUID id,
      String externalEventId,
      String eventType,
      String tenantKey,
      String status,
      String errorMessage,
      Instant receivedAt,
      Instant processedAt
  ) {
  }

  /**
   * Paginated list of webhook logs returned by the admin list endpoint.
   */
  public record PagedWebhookLogResponse(
      List<AdminWebhookLogResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages
  ) {
  }

  /**
   * Query parameters for the admin webhook log list endpoint.
   */
  public record WebhookLogListQuery(
      @jakarta.validation.constraints.Min(0) Integer page,
      @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) Integer size,
      String sortBy,
      String sortDir,
      String search,
      String status,
      String tenantKey
  ) {
    public WebhookLogListQuery(final Integer page, final Integer size, final String sortBy, final String sortDir,
                               final String search, final String status, final String tenantKey) {
      this.page = page != null ? page : 0;
      this.size = size != null ? size : 20;
      this.sortBy = sortBy != null ? sortBy : "receivedAt";
      this.sortDir = sortDir != null ? sortDir : "desc";
      this.search = search;
      this.status = status;
      this.tenantKey = tenantKey;
    }
  }
}
