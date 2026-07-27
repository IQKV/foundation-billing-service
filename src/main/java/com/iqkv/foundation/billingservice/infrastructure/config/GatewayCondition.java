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

import java.util.Map;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring {@link Condition} that matches when the active payment gateway type equals
 * the value declared on {@link ConditionalOnGateway}.
 *
 * <p>Reads {@code iqkv.payment.gateway.type} from the environment.
 * Defaults to {@code STRIPE} when the property is absent.
 */
public class GatewayCondition implements Condition {

  @Override
  public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
    final Map<String, Object> attributes =
        metadata.getAnnotationAttributes(ConditionalOnGateway.class.getName());
    if (attributes == null) {
      return false;
    }

    final GatewayType required = (GatewayType) attributes.get("value");
    final String configured = context.getEnvironment()
        .getProperty("iqkv.payment.gateway.type", GatewayType.STRIPE.name());

    return required != null && required.name().equalsIgnoreCase(configured);
  }
}
