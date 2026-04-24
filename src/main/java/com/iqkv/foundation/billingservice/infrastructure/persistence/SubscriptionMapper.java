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

import java.util.List;
import java.util.Optional;

import com.iqkv.foundation.billingservice.subscription.Subscription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper {

  /**
   * Insert or update a subscription record.
   * Conflicts on {@code external_subscription_id} trigger an update of mutable fields.
   */
  void upsert(Subscription subscription);

  /**
   * Returns the most recent non-cancelled subscription for the given tenant,
   * or empty if none exists.
   */
  Optional<Subscription> findActiveByTenantKey(String tenantKey);

  /**
   * Returns all subscription records for the given tenant, ordered by {@code created_at DESC}.
   */
  List<Subscription> findAllByTenantKey(String tenantKey);
}
