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

package com.iqkv.foundation.billingservice.plan;

import com.iqkv.foundation.billingservice.shared.exception.BillingServiceException;
import com.iqkv.foundation.billingservice.subscription.SubjectType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a plan's {@code scope} does not match the active subscription subject type.
 *
 * <p>For example, assigning a {@code TENANT}-scoped plan to a {@code USER} subject
 * (or vice versa) in the wrong rollout mode will trigger this exception.
 *
 * <p>Mapped to HTTP 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PlanScopeMismatchException extends BillingServiceException {

  public PlanScopeMismatchException(final String planCode, final SubjectType subjectType) {
    super("Plan '" + planCode + "' scope does not match subject type '" + subjectType.name() + "'");
  }
}
