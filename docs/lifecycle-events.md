# Billing Service Lifecycle Events & Email Notifications

The Billing service publishes lifecycle events to RabbitMQ for downstream consumption by other platform services. Additionally, it publishes email notification events for async email delivery.

## Lifecycle Events

All lifecycle events are published to the `iqkv.events` exchange with specific routing keys for platform integration.

## Email Notifications

The billing service also publishes email notification events that are consumed by a separate notification service for async email delivery.

### Email Notification Flow

1. **Event Trigger**: Billing lifecycle events (webhooks, tenant provisioning)
2. **Notification Publishing**: `NotificationEvent` published to `iqkv.events` exchange
3. **Routing Key**: `notification.billing.email`
4. **Async Processing**: Notification service consumes and sends emails
5. **Email Templates**: Each notification type maps to specific email templates

### Email Types

- **`SUBSCRIPTION_ACTIVATED`**: New subscription created or tenant provisioned
- **`SUBSCRIPTION_UPDATED`**: Subscription plan/quantity changed
- **`SUBSCRIPTION_CANCELLED`**: Subscription cancelled
- **`TRIAL_ENDING`**: Trial ending in 3 days (scheduled job)
- **`PAYMENT_OVERDUE`**: Payment past due (scheduled job)
- **`INVOICE_PAID`**: Payment successful
- **`PAYMENT_FAILED`**: Payment failed
- **`BILLING_UPDATED`**: Billing settings changed
- **`ACCOUNT_SUSPENDED`**: Account suspended

### Email Recipients

- **Multi-tenant mode**: `billingSettings.billingEmail`
- **Single-tenant mode**: `userBillingSettings.billingEmail`
- **Fallback**: Owner email from tenant creation

## Event Types

### 1. Subscription Created (`subscription.created`)

**Routing Key**: `subscription.created`  
**Triggered**: When a new Stripe subscription is created via webhook  
**Event Class**: `SubscriptionEvent`

```json
{
    "tenantKey": "xk7f2b9a",
    "externalSubscriptionId": "sub_1234567890",
    "eventType": "SUBSCRIPTION_CREATED",
    "occurredAt": "2026-04-26T10:30:00Z",
    "subjectType": "TENANT",
    "subjectKey": "xk7f2b9a"
}
```

### 2. Subscription Cancelled (`subscription.cancelled`)

**Routing Key**: `subscription.cancelled`  
**Triggered**: When a Stripe subscription is deleted/cancelled via webhook  
**Event Class**: `SubscriptionEvent`

```json
{
    "tenantKey": "xk7f2b9a",
    "externalSubscriptionId": "sub_1234567890",
    "eventType": "SUBSCRIPTION_CANCELLED",
    "occurredAt": "2026-04-26T10:30:00Z",
    "subjectType": "TENANT",
    "subjectKey": "xk7f2b9a"
}
```

### 3. Invoice Paid (`invoice.paid`)

**Routing Key**: `invoice.paid`  
**Triggered**: When a Stripe invoice payment succeeds via webhook  
**Event Class**: `InvoiceEvent`

```json
{
    "tenantKey": "xk7f2b9a",
    "externalInvoiceId": "in_1234567890",
    "externalCustomerId": "cus_1234567890",
    "externalSubscriptionId": "sub_1234567890",
    "eventType": "INVOICE_PAID",
    "amountPaid": 2999,
    "currency": "USD",
    "occurredAt": "2026-04-26T10:30:00Z",
    "subjectType": "TENANT",
    "subjectKey": "xk7f2b9a"
}
```

### 4. Payment Failed (`payment.failed`)

**Routing Key**: `payment.failed`  
**Triggered**: When a Stripe invoice payment fails via webhook  
**Event Class**: `PaymentEvent`

```json
{
    "tenantKey": "xk7f2b9a",
    "externalInvoiceId": "in_1234567890",
    "externalCustomerId": "cus_1234567890",
    "externalSubscriptionId": "sub_1234567890",
    "eventType": "PAYMENT_FAILED",
    "amountDue": 2999,
    "currency": "USD",
    "failureReason": "Your card was declined.",
    "occurredAt": "2026-04-26T10:30:00Z",
    "subjectType": "TENANT",
    "subjectKey": "xk7f2b9a"
}
```

## Email Notification Events

### Notification Event Structure

**Routing Key**: `notification.billing.email`  
**Event Class**: `NotificationEvent`

```json
{
    "recipientEmail": "billing@company.com",
    "locale": "en",
    "type": "SUBSCRIPTION_ACTIVATED",
    "payload": {
        "companyName": "Acme Corp",
        "planId": "pro-monthly",
        "externalSubscriptionId": "sub_1234567890",
        "amountPaid": 2999,
        "currency": "USD"
    },
    "occurredAt": "2026-04-26T10:30:00Z"
}
```

### Email Notification Types

1. **`SUBSCRIPTION_ACTIVATED`**: Sent when subscription created or tenant provisioned
2. **`SUBSCRIPTION_UPDATED`**: Sent when subscription plan/quantity changes
3. **`SUBSCRIPTION_CANCELLED`**: Sent when subscription cancelled
4. **`TRIAL_ENDING`**: Sent 3 days before trial expires (scheduled)
5. **`PAYMENT_OVERDUE`**: Sent for past due payments (scheduled)
6. **`INVOICE_PAID`**: Sent when payment succeeds
7. **`PAYMENT_FAILED`**: Sent when payment fails
8. **`BILLING_UPDATED`**: Sent when billing settings change
9. **`ACCOUNT_SUSPENDED`**: Sent when account is suspended

## Subject Types

Events include `subjectType` and `subjectKey` fields to identify the subscription owner:

- **Multi-tenant mode**: `subjectType = "TENANT"`, `subjectKey = tenantKey`
- **Single-tenant mode**: `subjectType = "USER"`, `subjectKey = userId`

This allows downstream services to handle entitlements consistently regardless of deployment mode.

## Event Publishing

Events are published via the `MessagingService` class with the following methods:

```java
// Subscription events
messagingService.publishSubscriptionCreated(tenantKey, subscriptionId, subjectType, subjectKey);
messagingService.publishSubscriptionCancelled(tenantKey, subscriptionId, subjectType, subjectKey);

// Invoice events
messagingService.publishInvoicePaid(tenantKey, invoiceId, customerId, subscriptionId,
                                   amountPaid, currency, subjectType, subjectKey);

// Payment events
messagingService.publishPaymentFailed(tenantKey, invoiceId, customerId, subscriptionId,
                                     amountDue, currency, failureReason, subjectType, subjectKey);
```

## Webhook Processing

All events are triggered by Stripe webhooks processed in `WebhookProcessingService`:

1. **Idempotency**: Each webhook is logged in `webhook_log` table to prevent duplicate processing
2. **Subject Resolution**: Uses `SubscriptionSubjectResolver` to determine correct subject scope
3. **Event Publishing**: Publishes lifecycle events after successful webhook processing
4. **Error Handling**: Failed webhooks are marked as `FAILED` status with error details

## Downstream Consumption

Services consuming these events should:

1. **Subscribe to relevant routing keys** in RabbitMQ configuration
2. **Handle idempotency** - events may be delivered multiple times
3. **Use subject fields** for entitlement evaluation instead of hardcoded tenant assumptions
4. **Implement dead letter handling** for failed message processing

## Implementation Status

✅ **Completed**:

- All four lifecycle events implemented
- Subject-aware event publishing
- Webhook-driven event triggers
- Idempotent processing
- Error handling and logging
- **Async email notification system**
- **Email notification events for all billing lifecycle changes**
- **Multi-tenant and single-tenant email resolution**
- **Configurable email templates and localization support**

The lifecycle events and email notification implementation is now complete and ready for production use.
