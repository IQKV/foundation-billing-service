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

package com.iqkv.foundation.billingservice.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.billingservice.webhook.WebhookLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WebhookLogMapper {

  int insert(WebhookLog log);

  int insertIfNotExists(WebhookLog log);

  boolean existsByExternalEventId(String externalEventId);

  void updateStatus(@Param("externalEventId") String externalEventId,
                    @Param("status") String status,
                    @Param("errorMessage") String errorMessage,
                    @Param("processedAt") Instant processedAt);

  Optional<WebhookLog> findById(@Param("id") UUID id);

  List<WebhookLog> findAll(@Param("limit") int limit,
                           @Param("offset") int offset,
                           @Param("sortBy") String sortBy,
                           @Param("sortDir") String sortDir,
                           @Param("search") String search,
                           @Param("status") String status,
                           @Param("tenantKey") String tenantKey);

  long countAll(@Param("search") String search,
                @Param("status") String status,
                @Param("tenantKey") String tenantKey);
}
