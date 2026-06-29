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

package com.iqkv.foundation.billingservice.gateway.port;

import java.util.Optional;

import com.iqkv.foundation.billingservice.gateway.GatewayType;
import com.iqkv.foundation.billingservice.gateway.command.CreateCheckoutSessionCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateCustomerCommand;
import com.iqkv.foundation.billingservice.gateway.command.CreateRefundCommand;
import com.iqkv.foundation.billingservice.gateway.command.UpdateSubscriptionCommand;
import com.iqkv.foundation.billingservice.gateway.event.GatewayWebhookEvent;
import com.iqkv.foundation.billingservice.plan.Plan;

/**
 * Port interface for payment gateway operations (Strategy pattern).
 *
 * <p>Defines the contract for interacting with payment gateways in a gateway-agnostic manner.
 * Implementations provide gateway-specific logic for Stripe, Lemon Squeezy, etc.
 *
 * <p>This interface follows the Hexagonal Architecture pattern, where the domain layer
 * defines ports (interfaces) and the infrastructure layer provides adapters (implementations).
 *
 * <p><b>Lemon Squeezy Behavior Notes:</b>
 * <ul>
 *   <li>{@code createCustomer}: LS requires an email address; throws exception if email is missing</li>
 *   <li>{@code createCheckoutSession}: Uses LS variant ID (configured in plan catalog) as price ID</li>
 *   <li>{@code cancelSubscription(id, false)}: LS maps to DELETE subscription (immediate)</li>
 *   <li>{@code cancelSubscription(id, true)}: LS maps to PATCH with cancelled=true</li>
 *   <li>{@code pauseSubscription}: LS maps to PATCH with pause mode VOID</li>
 *   <li>{@code reactivateSubscription}: LS maps to PATCH with pause=null</li>
 *   <li>{@code syncProduct}: LS products/variants are managed in dashboard; this method only verifies existence</li>
 *   <li>{@code createPortalSession}: LS uses customer portal sessions API</li>
 * </ul>
 */
public interface PaymentGatewayPort {

  /**
   * Returns the gateway type identifier.
   *
   * @return the gateway type (e.g., STRIPE, PAYPAL)
   */
  GatewayType getGatewayType();

  /**
   * Creates a customer in the payment gateway.
   *
   * @param command the customer creation command
   * @return the external customer ID from the gateway
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if creation fails
   */
  String createCustomer(CreateCustomerCommand command);

  /**
   * Creates a checkout session for subscription creation.
   *
   * @param command the checkout session creation command
   * @return the URL of the checkout session
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if session creation fails
   */
  String createCheckoutSession(CreateCheckoutSessionCommand command);

  /**
   * Updates an existing subscription.
   *
   * @param command the subscription update command
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if update fails
   */
  void updateSubscription(UpdateSubscriptionCommand command);

  /**
   * Cancels an existing subscription.
   *
   * @param subscriptionId    external subscription ID
   * @param cancelAtPeriodEnd whether to cancel at the end of the period or immediately
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if cancellation fails
   */
  void cancelSubscription(String subscriptionId, boolean cancelAtPeriodEnd);

  /**
   * Pauses an existing subscription.
   *
   * @param subscriptionId external subscription ID
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if pausing fails
   */
  void pauseSubscription(String subscriptionId);

  /**
   * Reactivates a paused subscription.
   *
   * @param subscriptionId external subscription ID
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if reactivation fails
   */
  void reactivateSubscription(String subscriptionId);

  /**
   * Creates a refund for a payment.
   *
   * @param command the refund creation command
   * @return the external refund ID
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if refund fails
   */
  String createRefund(CreateRefundCommand command);

  /**
   * Synchronizes a plan with the payment gateway (creates or updates product and price).
   *
   * @param plan the plan to synchronize
   * @return the external price ID from the gateway
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if synchronization fails
   */
  String syncProduct(Plan plan);

  /**
   * Verifies the webhook signature and, if valid, parses the payload into a normalized domain event.
   *
   * <p>Combines signature verification and parsing into a single step so that gateway adapters
   * can use the already-verified event object for parsing (avoiding double deserialization).
   *
   * @param payload   the raw webhook payload
   * @param signature the signature header value from the webhook request
   * @return an optional containing the parsed event if the signature is valid and the event type
   *     is supported; empty if the event type is not handled
   * @throws com.iqkv.foundation.billingservice.shared.exception.WebhookProcessingException if the signature is invalid or the payload cannot be deserialized
   */
  Optional<GatewayWebhookEvent> verifyAndParseWebhookEvent(String payload, String signature);

  /**
   * Creates a customer portal session for the given customer.
   *
   * @param customerId the external customer ID
   * @param returnUrl  the URL to redirect to after the user leaves the portal
   * @return the URL of the portal session
   * @throws com.iqkv.foundation.billingservice.shared.exception.PaymentGatewayException if session creation fails
   */
  String createPortalSession(String customerId, String returnUrl);
}
