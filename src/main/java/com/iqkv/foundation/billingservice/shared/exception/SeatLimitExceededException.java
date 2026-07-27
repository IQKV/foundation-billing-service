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

package com.iqkv.foundation.billingservice.shared.exception;

/**
 * Thrown when a requested seat count exceeds the ceiling defined by the plan's
 * {@code PlanEntitlement.maxUsers} (0 = unlimited).
 *
 * <p>Maps to HTTP {@code 422 Unprocessable Entity} — the request is well-formed but
 * cannot be fulfilled within the plan's seat limit. The response body carries
 * {@code planCode}, {@code requested}, and {@code limit} so the UI can show a
 * targeted upgrade prompt.
 */
public class SeatLimitExceededException extends RuntimeException {

  private final String planCode;
  private final long requested;
  private final int limit;

  public SeatLimitExceededException(final String planCode, final long requested, final int limit) {
    super("Requested seat count " + requested + " exceeds the limit of " + limit
          + " for plan '" + planCode + "'. Upgrade your plan to add more seats.");
    this.planCode = planCode;
    this.requested = requested;
    this.limit = limit;
  }

  public String getPlanCode() {
    return planCode;
  }

  public long getRequested() {
    return requested;
  }

  public int getLimit() {
    return limit;
  }
}
