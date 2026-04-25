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
 * Represents the resolved subject of a subscription operation.
 *
 * <p>In {@code MULTI_TENANT} mode the subject is the tenant:
 * {@code type = TENANT}, {@code key = tenantKey}.
 *
 * <p>In {@code SINGLE_TENANT} mode the subject is the individual user:
 * {@code type = USER}, {@code key = userId.toString()}.
 *
 * @param type the subject type (TENANT or USER)
 * @param key  the subject identifier (tenantKey or userId string)
 */
public record SubscriptionSubject(SubjectType type, String key) {
}
