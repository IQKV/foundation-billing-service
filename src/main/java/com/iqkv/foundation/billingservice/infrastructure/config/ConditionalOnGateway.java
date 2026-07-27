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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import org.springframework.context.annotation.Conditional;

/**
 * Meta-annotation that activates a bean only when the configured payment gateway matches
 * the specified {@link GatewayType}.
 *
 * <p>Evaluated against {@code iqkv.payment.gateway.type} at application context creation.
 * Defaults to {@code STRIPE} when the property is absent.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}Component
 * {@literal @}ConditionalOnGateway(GatewayType.STRIPE)
 * public class StripeGatewayAdapter implements PaymentGatewayPort { ... }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(GatewayCondition.class)
public @interface ConditionalOnGateway {

  /**
   * The gateway type required for this bean to be registered.
   */
  GatewayType value();
}
