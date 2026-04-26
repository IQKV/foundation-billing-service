# Billing Service Email Notifications

Complete coverage of all billing lifecycle email notifications.

## 📧 **Email Notification Coverage**

### ✅ **Implemented Email Types**

| **Email Type**           | **Trigger**                              | **When Sent**         | **Payload Data**                             |
| ------------------------ | ---------------------------------------- | --------------------- | -------------------------------------------- |
| `SUBSCRIPTION_ACTIVATED` | Subscription created, Tenant provisioned | Webhook + Event       | companyName, planId, subscriptionId          |
| `SUBSCRIPTION_UPDATED`   | Subscription modified                    | Webhook               | companyName, planId, status, subscriptionId  |
| `SUBSCRIPTION_CANCELLED` | Subscription deleted                     | Webhook               | companyName, subscriptionId                  |
| `TRIAL_ENDING`           | Trial expires in 3 days                  | Scheduled (9 AM UTC)  | companyName, planId, trialEndDate            |
| `PAYMENT_OVERDUE`        | Payment past due                         | Scheduled (10 AM UTC) | companyName, planId, overdueDate             |
| `INVOICE_PAID`           | Payment succeeds                         | Webhook               | companyName, invoiceId, amountPaid, currency |
| `PAYMENT_FAILED`         | Payment fails                            | Webhook               | companyName, invoiceId, amountDue, currency  |
| `BILLING_UPDATED`        | Settings changed                         | REST API              | companyName, tenantKey, updatedAt            |
| `ACCOUNT_SUSPENDED`      | Tenant suspended                         | Event                 | companyName, tenantKey, suspendedAt          |

### 🎯 **Email Triggers**

#### **Webhook-Driven (Real-time)**

- Stripe subscription created → `SUBSCRIPTION_ACTIVATED`
- Stripe subscription updated → `SUBSCRIPTION_UPDATED`
- Stripe subscription deleted → `SUBSCRIPTION_CANCELLED`
- Stripe invoice paid → `INVOICE_PAID`
- Stripe payment failed → `PAYMENT_FAILED`

#### **Event-Driven (Async)**

- Tenant provisioned → `SUBSCRIPTION_ACTIVATED`
- Tenant suspended → `ACCOUNT_SUSPENDED`

#### **API-Driven (User Action)**

- Billing settings updated → `BILLING_UPDATED`

#### **Scheduled Jobs (Proactive)**

- Daily 9 AM UTC → `TRIAL_ENDING` (3 days before expiry)
- Daily 10 AM UTC → `PAYMENT_OVERDUE` (past due payments)

## 🔧 **Technical Implementation**

### **Email Flow Architecture**

```
Trigger → NotificationEvent → RabbitMQ → Notification Service → Email Sent
```

### **Email Resolution Logic**

1. **Multi-tenant mode**: `billingSettings.billingEmail`
2. **Single-tenant mode**: `userBillingSettings.billingEmail`
3. **Fallback**: Owner email from tenant creation
4. **No email**: Skip notification (logged as warning)

### **Event Structure**

```json
{
    "recipientEmail": "billing@company.com",
    "locale": "en",
    "type": "SUBSCRIPTION_ACTIVATED",
    "payload": {
        "companyName": "Acme Corp",
        "planId": "pro-monthly",
        "externalSubscriptionId": "sub_1234567890"
    },
    "occurredAt": "2026-04-26T10:30:00Z"
}
```

### **Routing Configuration**

- **Exchange**: `iqkv.events`
- **Routing Key**: `notification.billing.email`
- **Queue**: `iqkv.billing.notifications`
- **Consumer**: External notification service

## 📋 **Business Value**

### **Customer Experience**

- **Onboarding**: Welcome emails for new subscriptions
- **Transparency**: Real-time payment and billing updates
- **Retention**: Proactive trial ending notifications
- **Support**: Clear communication on account issues

### **Revenue Protection**

- **Dunning**: Automated overdue payment reminders
- **Conversion**: Trial ending notifications drive upgrades
- **Compliance**: Audit trail of all billing communications

### **Operational Efficiency**

- **Automation**: No manual email sending required
- **Scalability**: Async processing handles high volume
- **Reliability**: Idempotent processing prevents duplicates
- **Monitoring**: Comprehensive logging and error handling

## 🚀 **Production Readiness**

### ✅ **Completed Features**

- All 9 email notification types implemented
- Webhook-driven real-time notifications
- Scheduled proactive notifications
- Multi-tenant and single-tenant support
- Comprehensive error handling and logging
- Idempotent processing
- Rich email payloads with personalization data

### 🔮 **Future Enhancements**

- Email template customization per tenant
- Notification preferences (frequency, types)
- Email delivery status tracking
- A/B testing for email content
- Multi-language email templates
- SMS notifications for critical events

The billing service now provides **complete email notification coverage** for all billing lifecycle events, ensuring customers are always informed about their subscription status, payments, and account changes.
