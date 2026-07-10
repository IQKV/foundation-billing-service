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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity for metering configuration attached to a plan.
 * Only applicable for plans with PricingModel.METERED.
 */
public class PlanMeteringConfig {
  private UUID id;
  private UUID planId;
  private String metricName;
  private AggregationType aggregationType;
  private String externalMeterId;
  private String tiersConfig;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public enum AggregationType {
    SUM,
    LAST,
    MAX,
    PER_SEAT
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getPlanId() {
    return planId;
  }

  public void setPlanId(UUID planId) {
    this.planId = planId;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public AggregationType getAggregationType() {
    return aggregationType;
  }

  public void setAggregationType(AggregationType aggregationType) {
    this.aggregationType = aggregationType;
  }

  public String getExternalMeterId() {
    return externalMeterId;
  }

  public void setExternalMeterId(String externalMeterId) {
    this.externalMeterId = externalMeterId;
  }

  public String getTiersConfig() {
    return tiersConfig;
  }

  public void setTiersConfig(String tiersConfig) {
    this.tiersConfig = tiersConfig;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
