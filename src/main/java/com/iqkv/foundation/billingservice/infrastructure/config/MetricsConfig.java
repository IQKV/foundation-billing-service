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

package com.iqkv.foundation.billingservice.infrastructure.config;

import com.iqkv.foundation.billingservice.infrastructure.persistence.SubscriptionMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class MetricsConfig {

  private final SubscriptionMapper subscriptionMapper;
  private final MeterRegistry meterRegistry;

  public MetricsConfig(final SubscriptionMapper subscriptionMapper, final MeterRegistry meterRegistry) {
    this.subscriptionMapper = subscriptionMapper;
    this.meterRegistry = meterRegistry;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void registerMetrics() {
    Gauge.builder("billing_subscriptions_active_count", () -> subscriptionMapper.countAll(null, "active", null))
        .description("Current number of active subscriptions")
        .register(meterRegistry);
  }
}
