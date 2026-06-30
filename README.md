> ## 🤔 What is this service all about?
>
> - Billing and subscription management microservice for the IQ Key Value platform.
> - Payment gateway abstraction layer — supports multiple providers (Stripe implemented) via Strategy pattern; subscriptions, invoices, and the dashboard are managed on the gateway's side.
> - Make the project easy to maintain with **8 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...

---

# 💳 IQ Key Value Billing Service

Billing and subscription management microservice. Provides a gateway-agnostic payment abstraction layer — handles tenant-to-customer mapping, webhook ingestion, and lifecycle event publishing. No custom billing logic lives here.

## About

The Billing service owns the payment gateway integration layer for the platform:

- **Automatic customer provisioning** — listens for `tenant.provisioned` events on RabbitMQ and creates a payment gateway customer per tenant; the `external_customer_id` is stored in `billing_settings`
- **Billing settings** — each tenant has a 1:1 `billing_settings` record that is the single source of truth for payment gateway customer metadata; decoupled from IAM users by design
- **Billing email** — a separate `billing_email` field allows finance teams to receive invoices without a system account
- **Tax ID / VAT/GST** — stored in `billing_settings` for compliant B2B invoices
- **Webhook processing** — payment gateway webhooks are ingested and processed idempotently; duplicate delivery is safe
- **Lifecycle events** — publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `invoice.created`, `invoice.finalized`, `invoice.updated`, `payment.failed`, and `refund.created` to the platform event bus
- **Multi-gateway strategy** — `PaymentGatewayPort` interface decouples business logic from gateway SDKs; Stripe and Lemon Squeezy are implemented
- **Plan catalog** — plan definitions with typed `PlanFeatures` (`maxUsers`, `maxProjects` as typed quota fields; extensible `features` map keyed by feature code such as `priority_support`; `trialPeriodDays` to define free trial length in days, 0 means no trial) defined in YAML configuration. Each plan carries a `pricingModel` field — `FLAT` (fixed price per period, default) or `PER_SEAT` (price × seat count; `maxUsers` acts as the seat ceiling). `BillingSeedRunner` synchronizes plans with the Stripe catalog at application startup. Plan management is config-driven — there is no REST API for creating, updating, or deactivating plans. `PlanFeatureRegistry` serves an in-memory feature map loaded at startup for zero-latency entitlement evaluation and the internal plans endpoint (`/internal/plans`); features are the single source of truth for platform-wide access control
- **Observability** — instrumented with Micrometer for Prometheus metrics; includes a custom Grafana dashboard for business KPIs (revenue, subscriptions, webhook health)

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/billing`

### Billing Settings

| Method  | Path                           | Auth               | Description                                        |
| ------- | ------------------------------ | ------------------ | -------------------------------------------------- |
| `GET`   | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Get billing settings for a tenant                  |
| `POST`  | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Create billing settings for a tenant               |
| `PATCH` | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Update billing settings (syncs to payment gateway) |
| `POST`  | `/settings/{tenantKey}/portal` | JWT `TENANT_OWNER` | Create a Stripe Customer Portal session            |

### Billing Settings (platform admin)

| Method   | Path                                          | Auth                 | Description                          |
| -------- | --------------------------------------------- | -------------------- | ------------------------------------ |
| `GET`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Get billing settings for a tenant    |
| `POST`   | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Create billing settings for a tenant |
| `PUT`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Replace billing settings             |
| `PATCH`  | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Partially update billing settings    |
| `DELETE` | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Delete billing settings              |

### Subscriptions

| Method  | Path                                                | Auth                           | Description                                 |
| ------- | --------------------------------------------------- | ------------------------------ | ------------------------------------------- |
| `GET`   | `/subscriptions/{tenantKey}/active`                 | JWT `TENANT_OWNER`             | Get active subscription for a tenant        |
| `GET`   | `/subscriptions/{tenantKey}`                        | JWT `TENANT_OWNER`             | Get all subscriptions for a tenant          |
| `POST`  | `/subscriptions/{tenantKey}/checkout`               | JWT `TENANT_OWNER`             | Create a Checkout Session for subscription  |
| `POST`  | `/subscriptions/{tenantKey}/{subscriptionId}`       | JWT `TENANT_OWNER`             | Update an existing subscription             |
| `PATCH` | `/subscriptions/{tenantKey}/{subscriptionId}/seats` | JWT `TENANT_OWNER`             | Adjust seat count (PER_SEAT plans only)     |
| `GET`   | `/subscriptions/me/active`                          | JWT `TENANT_OWNER` or `MEMBER` | Get active subscription for current subject |
| `GET`   | `/subscriptions/me`                                 | JWT `TENANT_OWNER` or `MEMBER` | Get all subscriptions for current subject   |

### Payments

| Method | Path                            | Auth               | Description                  |
| ------ | ------------------------------- | ------------------ | ---------------------------- |
| `POST` | `/payments/{tenantKey}/refund`  | JWT `TENANT_OWNER` | Create a refund for a tenant |
| `GET`  | `/payments/{tenantKey}/refunds` | JWT `TENANT_OWNER` | List refunds for a tenant    |

### Subscriptions (platform admin)

| Method   | Path                                   | Auth                 | Description                                 |
| -------- | -------------------------------------- | -------------------- | ------------------------------------------- |
| `GET`    | `/admin/subscriptions`                 | JWT `PLATFORM_ADMIN` | List subscriptions (paginated, filterable)  |
| `GET`    | `/admin/subscriptions/count`           | JWT `PLATFORM_ADMIN` | Count all subscriptions                     |
| `GET`    | `/admin/subscriptions/{id}`            | JWT `PLATFORM_ADMIN` | Get subscription by ID                      |
| `PATCH`  | `/admin/subscriptions/{id}`            | JWT `PLATFORM_ADMIN` | Partially update subscription               |
| `POST`   | `/admin/subscriptions/{id}/cancel`     | JWT `PLATFORM_ADMIN` | Cancel subscription via payment gateway     |
| `POST`   | `/admin/subscriptions/{id}/pause`      | JWT `PLATFORM_ADMIN` | Pause subscription via payment gateway      |
| `POST`   | `/admin/subscriptions/{id}/reactivate` | JWT `PLATFORM_ADMIN` | Reactivate subscription via payment gateway |
| `DELETE` | `/admin/subscriptions/{id}`            | JWT `PLATFORM_ADMIN` | Delete subscription                         |

### Refunds (platform admin)

| Method | Path                  | Auth                 | Description                            |
| ------ | --------------------- | -------------------- | -------------------------------------- |
| `GET`  | `/admin/refunds`      | JWT `PLATFORM_ADMIN` | List all refunds (paginated, filtered) |
| `GET`  | `/admin/refunds/{id}` | JWT `PLATFORM_ADMIN` | Get refund by ID                       |

### Plan Catalog

| Method | Path                | Auth                    | Description           |
| ------ | ------------------- | ----------------------- | --------------------- |
| `GET`  | `/plans`            | JWT (any authenticated) | List all active plans |
| `GET`  | `/plans/{planCode}` | JWT (any authenticated) | Get plan by planCode  |

### Plan Catalog (platform admin)

| Method | Path                      | Auth                 | Description                         |
| ------ | ------------------------- | -------------------- | ----------------------------------- |
| `GET`  | `/admin/plans`            | JWT `PLATFORM_ADMIN` | List all plans (including inactive) |
| `GET`  | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Get plan by planCode                |

> Plans are defined in `application.yml` under `iqkv.billing.stripe.schema.products` and synchronized with Stripe at startup by `BillingSeedRunner`. There is no REST API for creating, updating, or deactivating plans — all catalog changes go through configuration and deployment.

### Entitlements

| Method | Path               | Auth                           | Description                                                              |
| ------ | ------------------ | ------------------------------ | ------------------------------------------------------------------------ |
| `GET`  | `/entitlements/me` | JWT `TENANT_OWNER` or `MEMBER` | Active plan, subscription status, and typed features for current subject |

**Entitlement evaluation is tightly coupled with plan features** — the response includes the complete `PlanFeatures` record for the tenant's active plan, enabling fine-grained access control decisions in client applications. Returns `404` when no active subscription exists. Resolves subject by rollout mode (tenant in multi-tenant, user in single-tenant).

**Example Response (with trial):**

```json
{
    "planCode": "pro-monthly",
    "status": "trialing",
    "isInTrial": true,
    "trialDaysLeft": 7,
    "currentPeriodEnd": "2026-07-15T00:00:00Z",
    "features": {
        "maxUsers": 50,
        "maxProjects": 0,
        "features": {
            "priority_support": {
                "code": "priority_support",
                "title": "Priority Support",
                "value": "true",
                "description": "Access to priority support channel"
            }
        }
    }
}
```

**Example Response (without trial):**

```json
{
    "planCode": "pro-monthly",
    "status": "active",
    "isInTrial": false,
    "trialDaysLeft": null,
    "currentPeriodEnd": "2026-07-15T00:00:00Z",
    "features": {
        "maxUsers": 50,
        "maxProjects": 0,
        "features": {
            "priority_support": {
                "code": "priority_support",
                "title": "Priority Support",
                "value": "true",
                "description": "Access to priority support channel"
            }
        }
    }
}
```

### Plan Catalog (internal service-to-service)

| Method | Path                     | Auth | Description                                                            |
| ------ | ------------------------ | ---- | ---------------------------------------------------------------------- |
| `GET`  | `/internal/plans`        | None | Full plan feature catalog — used by gateway/service `PlanCatalogCache` |
| `GET`  | `/internal/plans/public` | None | Full plan catalog details for public pricing pages                     |

**Public within internal network** — no authentication required since the response contains only non-sensitive plan feature data (same as any public pricing page). Not exposed via a public gateway route. Backed by in-memory `PlanFeatureRegistry` — no DB reads.

## Events & Messaging

The Billing service publishes payment-related events to RabbitMQ for downstream consumption.

**Exchange**: `iqkv.events` (Topic)

### Subscription Events (`subscription.*`)

| Routing Key              | Event Type               | Description                              |
| :----------------------- | :----------------------- | :--------------------------------------- |
| `subscription.created`   | `SUBSCRIPTION_CREATED`   | New subscription activated for a tenant. |
| `subscription.cancelled` | `SUBSCRIPTION_CANCELLED` | Subscription ended.                      |

### Invoice Events (`invoice.*`)

| Routing Key         | Event Type          | Description                              |
| :------------------ | :------------------ | :--------------------------------------- |
| `invoice.created`   | `INVOICE_CREATED`   | Draft invoice generated.                 |
| `invoice.finalized` | `INVOICE_FINALIZED` | Invoice finalized and ready for payment. |
| `invoice.paid`      | `INVOICE_PAID`      | Payment successfully received.           |
| `invoice.updated`   | `INVOICE_UPDATED`   | Invoice metadata changed.                |

### Payment Events (`payment.*`)

| Routing Key      | Event Type       | Description                                      |
| :--------------- | :--------------- | :----------------------------------------------- |
| `payment.failed` | `PAYMENT_FAILED` | Payment transaction failed (e.g. card declined). |

### Refund Events (`refund.*`)

| Routing Key      | Event Type       | Description       |
| :--------------- | :--------------- | :---------------- |
| `refund.created` | `REFUND_CREATED` | Refund initiated. |

### Notifications (`notification.billing.*`)

| Routing Key                  | Description                                              |
| :--------------------------- | :------------------------------------------------------- |
| `notification.billing.email` | Billing-specific emails (Invoice Paid, Payment Overdue). |

### Webhooks

| Method | Path                      | Auth                    | Description                                      |
| ------ | ------------------------- | ----------------------- | ------------------------------------------------ |
| `POST` | `/webhooks/stripe`        | Stripe signature        | Receive and process Stripe webhook events        |
| `POST` | `/webhooks/lemon-squeezy` | Lemon Squeezy signature | Receive and process Lemon Squeezy webhook events |

## Tech Stack

- Java 25 / Spring Boot 4.1
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase for schema migrations
- RabbitMQ for async event consumption and publishing
- Stripe Java SDK (active gateway adapter)
- ShedLock 7.x for distributed scheduled jobs
- Micrometer + Prometheus

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 11.0.8 (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/IQKV/foundation-billing-service.git
cd foundation-billing-service

# Install git hooks
pnpm install

# Copy environment variables
cp .env.example .env.local
# Edit .env.local — defaults work for local Docker setup

# Start infrastructure (PostgreSQL, RabbitMQ, MailHog, IAM-service)
docker compose up -d

# Run the service (locally via IDE or CLI)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
# → MailHog:  http://localhost:8025
# → IAM API:  http://localhost:8082
```

## Environment Variables

| Variable                       | Default                         | Description                                                  |
| ------------------------------ | ------------------------------- | ------------------------------------------------------------ |
| `PAYMENT_GATEWAY_TYPE`         | `STRIPE`                        | Active payment gateway (`STRIPE` or `LEMON_SQUEEZY`)         |
| `DB_HOST`                      | `localhost`                     | PostgreSQL host                                              |
| `DB_PORT`                      | `5432`                          | PostgreSQL port                                              |
| `DB_NAME`                      | `billing`                       | Database name                                                |
| `DB_USERNAME`                  | `billing`                       | Database user                                                |
| `DB_PASSWORD`                  | `billing`                       | Database password                                            |
| `RABBITMQ_HOST`                | `localhost`                     | RabbitMQ host                                                |
| `RABBITMQ_PORT`                | `5672`                          | RabbitMQ AMQP port                                           |
| `RABBITMQ_USERNAME`            | `billing`                       | RabbitMQ user                                                |
| `RABBITMQ_PASSWORD`            | `billing`                       | RabbitMQ password                                            |
| `STRIPE_SECRET_KEY`            | `sk_test_placeholder`           | Stripe secret key (required when gateway=STRIPE)             |
| `STRIPE_WEBHOOK_SECRET`        | `whsec_placeholder`             | Stripe webhook signing secret                                |
| `STRIPE_PORTAL_RETURN_URL`     | `http://localhost:3000/billing` | Stripe portal return URL                                     |
| `LEMON_SQUEEZY_API_KEY`        | `ls_test_placeholder`           | Lemon Squeezy API key (required when gateway=LEMON_SQUEEZY)  |
| `LEMON_SQUEEZY_WEBHOOK_SECRET` | `whsec_placeholder`             | Lemon Squeezy webhook signing secret                         |
| `LEMON_SQUEEZY_STORE_ID`       | `12345`                         | Lemon Squeezy store ID (required when gateway=LEMON_SQUEEZY) |

Copy `.env.example` to `.env.local` (or `.env.uat` / `.env.prd`) and fill in production values.

## Maven Commands

```bash
# Build and test (skip Checkstyle during development)
./mvnw clean verify -Dcheckstyle.skip=true

# Run tests only
./mvnw test -Dcheckstyle.skip=true

# Explicit Checkstyle check
./mvnw checkstyle:check

# Coverage report → target/site/jacoco/index.html
./mvnw jacoco:report

# Production build
./mvnw clean package -Pproduction
```

## Docker

```bash
# Build image
docker build -t iqkv/foundation-billing-service:latest .

# Run full stack (Billing Service + IAM Service + Infrastructure)
docker compose -f compose.container.yaml up -d
```

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:25-jdk-alpine`, the runtime stage uses `eclipse-temurin:25-jre-alpine` with a non-root `appuser` and layered JAR extraction for optimal cache reuse.

Note: The root `compose.yaml` is for development purposes only and is self-contained. It starts all required external services (PostgreSQL with pre-initialized `billing` and `iam` databases, RabbitMQ, MailHog, and the IAM Service) but excludes the Billing Service itself, which should be run locally in your IDE for a better development experience.

## Monitoring

| Endpoint                   | Description                 |
| -------------------------- | --------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes |
| `GET /actuator/metrics`    | Application metrics         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint  |
| `GET /swagger-ui.html`     | API documentation           |

The service is instrumented with custom business metrics:

- **Revenue & Payments**: `billing_revenue_total`, `billing_payments_total` (success/failure)
- **Subscriptions**: `billing_subscriptions_active_count`, `billing_subscriptions_total` (lifecycle), `billing_seat_adjustments_total` (per-seat changes)
- **Webhook Health**: `billing_webhooks_total`, `billing_webhooks_processing_duration_seconds`
- **System Health**: `billing_emails_sent_total`, `billing_entitlements_check_total`

A Grafana dashboard (`docker/grafana/provisioning/dashboards/BillingService.json`) is provided to visualize these KPIs alongside standard JVM metrics.

## Project Structure

```
src/main/java/com/iqkv/foundation/billingservice/
├── gateway/            # Payment gateway abstraction (Strategy pattern + Hexagonal Architecture)
│   ├── port/           # PaymentGatewayPort interface
│   ├── event/          # Gateway-agnostic webhook event models
│   ├── command/        # Gateway-agnostic command models
│   └── adapter/        # Gateway implementations
│       ├── stripe/     # Stripe implementation of PaymentGatewayPort
│       └── lemon-squeezy/ # Lemon Squeezy implementation of PaymentGatewayPort
├── plan/               # Plan catalog — CRUD, typed PlanFeatures, PlanFeatureRegistry, eligibility policy
├── subscription/       # Subscription cache, subject resolution, entitlement evaluation, entitlements endpoint
├── settings/           # Tenant billing settings (contact email, tax ID, external customer ID)
├── userbilling/        # Per-user billing settings (single-tenant mode)
├── webhook/            # Webhook ingestion, idempotent gateway-agnostic processing, event log
├── tenancy/            # Tenant context extraction from X-Tenant-ID header
├── shared/             # Common exceptions, utilities
└── infrastructure/     # Spring config, security, MyBatis, RabbitMQ setup
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
