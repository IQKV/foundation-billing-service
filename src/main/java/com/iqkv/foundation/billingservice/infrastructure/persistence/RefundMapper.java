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

package com.iqkv.foundation.billingservice.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.iqkv.foundation.billingservice.subscription.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundMapper {

  /**
   * Insert or update a refund record.
   * Conflicts on {@code external_refund_id} trigger an update of mutable fields.
   */
  void upsert(Refund refund);

  /**
   * Returns all refund records for the given tenant, ordered by {@code occurred_at DESC}.
   */
  List<Refund> findAllByTenantKey(String tenantKey);

  /**
   * Returns all refund records for the given subject (type + key),
   * ordered by {@code occurred_at DESC}.
   */
  List<Refund> findAllBySubject(@Param("subjectType") String subjectType,
                                @Param("subjectKey") String subjectKey);

  /**
   * Returns refund by external refund ID.
   */
  Optional<Refund> findByExternalRefundId(String externalRefundId);

  /**
   * Returns refund by ID.
   */
  Optional<Refund> findById(@Param("id") java.util.UUID id);

  /**
   * Returns a paginated, sorted, and optionally filtered list of refunds.
   */
  List<Refund> findAll(@Param("limit") int limit,
                       @Param("offset") int offset,
                       @Param("sortBy") String sortBy,
                       @Param("sortDir") String sortDir,
                       @Param("tenantKey") String tenantKey);

  /**
   * Returns the total count of refunds matching the optional filters.
   */
  long countAll(@Param("tenantKey") String tenantKey);
}
