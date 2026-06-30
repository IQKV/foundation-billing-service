# Foundation Billing Service 💳

Billing and subscription management microservice for the Key Value Platform. Provides a gateway-agnostic payment abstraction layer — handles tenant-to-customer mapping, plan catalog management, webhook ingestion, and lifecycle event publishing. No custom billing logic lives here.

## About

The Billing service owns the payment gateway integration layer for the platform:

- **Automatic customer provisioning** — listens for `tenant.created` and `tenant.provisioned` events on RabbitMQ and creates a payment gateway customer per tenant; the `external_customer_id` is stored in `billing_settings`
- **Multi-gateway strategy** — `PaymentGatewayPort` interface (Strategy pattern + Hexagonal Architecture) decouples business logic from gateway SDKs; Stripe and Lemon Squeezy are implemented, additional gateways are reserved
- **Plan catalog** — plan definitions with typed `PlanFeatures` (scoped to `MULTI_TENANT` or `SINGLE_TENANT` mode); quota fields (`maxUsers`, `maxProjects`) are typed `int` fields; `trialPeriodDays` to define free trial length in days (0 means no trial); display features (e.g. `priority_support`) are held in an extensible `Map<String, PlanFeature>` keyed by feature code — adding a new feature requires only a YAML change. Each plan carries a `pricingModel` field — `FLAT` (fixed price per period, default) or `PER_SEAT` (price multiplied by seat count; `maxUsers` acts as the seat ceiling; checkout validates and routes quantity automatically). `BillingSeedRunner` synchronizes plans with the Stripe catalog (external Product and Price ID creation) at application startup. Plan management is config-driven — there is no REST API for creating, updating, or deactivating plans. The static feature registry is loaded into the in-memory `PlanFeatureRegistry` at startup and served via a public internal endpoint for gateway and downstream service caching; **entitlement evaluation is tightly coupled with plan features** — the entitlements endpoint returns complete feature data enabling client-side access control; `PlanEligibilityPolicy` validates scope against active rollout mode
- **Billing settings** — each tenant has a 1:1 `billing_settings` record that is the single source of truth for payment gateway customer metadata; decoupled from IAM users by design
- **Billing email** — a separate `billing_email` field allows finance teams to receive invoices without a system account
- **Tax ID / VAT/GST** — stored in `billing_settings` for compliant B2B invoices
- **Webhook processing** — payment gateway webhooks are ingested and processed idempotently via a gateway-agnostic orchestrator; duplicate delivery is safe
- **Lifecycle events** — publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `invoice.created`, `invoice.finalized`, `invoice.updated`, `payment.failed`, and `refund.created` to the platform event bus
- **Observability** — instrumented with Micrometer for Prometheus metrics; includes a custom Grafana dashboard for business KPIs (revenue, subscriptions, webhook health)
- **Email notifications** — publishes `notification.billing.email` events for async delivery by the notification service
- **Subject-aware subscriptions** — `subjectType` (`TENANT` | `USER`) and `subjectKey` support both multi-tenant and single-tenant entitlement evaluation

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Lifecycle Events & Email Notifications](./docs/lifecycle-events.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/billing`

### Billing Settings — `/api/v1/billing/settings`

| Method  | Path                           | Auth                               | Description                                        |
| ------- | ------------------------------ | ---------------------------------- | -------------------------------------------------- |
| `GET`   | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` + `X-Tenant-ID` | Get billing settings (contact email, tax ID, etc.) |
| `POST`  | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` + `X-Tenant-ID` | Create billing settings for a tenant               |
| `PATCH` | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` + `X-Tenant-ID` | Update billing settings (syncs to payment gateway) |
| `POST`  | `/settings/{tenantKey}/portal` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Create a Stripe Customer Portal session            |

### Billing Settings (platform admin) — `/api/v1/billing/admin/tenants/{tenantKey}/billing-settings`

| Method   | Path                                          | Auth                 | Description                          |
| -------- | --------------------------------------------- | -------------------- | ------------------------------------ |
| `GET`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Get billing settings for a tenant    |
| `POST`   | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Create billing settings for a tenant |
| `PUT`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Replace billing settings             |
| `PATCH`  | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Partially update billing settings    |
| `DELETE` | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Delete billing settings              |

### Subscriptions — `/api/v1/billing/subscriptions`

| Method  | Path                                                | Auth                                        | Description                                 |
| ------- | --------------------------------------------------- | ------------------------------------------- | ------------------------------------------- |
| `GET`   | `/subscriptions/{tenantKey}/active`                 | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get active subscription for tenant          |
| `GET`   | `/subscriptions/{tenantKey}`                        | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get all subscriptions for tenant            |
| `POST`  | `/subscriptions/{tenantKey}/checkout`               | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Create a Checkout Session for subscription  |
| `POST`  | `/subscriptions/{tenantKey}/{subscriptionId}`       | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Update an existing subscription             |
| `PATCH` | `/subscriptions/{tenantKey}/{subscriptionId}/seats` | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Adjust seat count (PER_SEAT plans only)     |
| `GET`   | `/subscriptions/me/active`                          | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get active subscription for current subject |
| `GET`   | `/subscriptions/me`                                 | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get all subscriptions for current subject   |

### Payments — `/api/v1/billing/payments`

| Method | Path                            | Auth                               | Description                  |
| ------ | ------------------------------- | ---------------------------------- | ---------------------------- |
| `POST` | `/payments/{tenantKey}/refund`  | JWT `TENANT_OWNER` + `X-Tenant-ID` | Create a refund for a tenant |
| `GET`  | `/payments/{tenantKey}/refunds` | JWT `TENANT_OWNER` + `X-Tenant-ID` | List refunds for a tenant    |

> Subscription data is a local cache — no gateway round-trips on read. Subject resolves to tenant (multi-tenant) or user (single-tenant) based on `ROLLOUT_MODE`.

### Subscriptions (platform admin) — `/api/v1/billing/admin/subscriptions`

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

### Refunds (platform admin) — `/api/v1/billing/admin/refunds`

| Method | Path                  | Auth                 | Description                            |
| ------ | --------------------- | -------------------- | -------------------------------------- |
| `GET`  | `/admin/refunds`      | JWT `PLATFORM_ADMIN` | List all refunds (paginated, filtered) |
| `GET`  | `/admin/refunds/{id}` | JWT `PLATFORM_ADMIN` | Get refund by ID                       |

### Plan Catalog — `/api/v1/billing/plans`

| Method | Path                | Auth                    | Description           |
| ------ | ------------------- | ----------------------- | --------------------- |
| `GET`  | `/plans`            | JWT (any authenticated) | List all active plans |
| `GET`  | `/plans/{planCode}` | JWT (any authenticated) | Get plan by planCode  |

### Plan Catalog (platform admin) — `/api/v1/billing/admin/plans`

| Method | Path                      | Auth                 | Description                         |
| ------ | ------------------------- | -------------------- | ----------------------------------- |
| `GET`  | `/admin/plans`            | JWT `PLATFORM_ADMIN` | List all plans (including inactive) |
| `GET`  | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Get plan by planCode                |

> Plans are defined in `application.yml` under `iqkv.billing.stripe.schema.products` and synchronized with Stripe at startup by `BillingSeedRunner`. There is no REST API for creating, updating, or deactivating plans — all catalog changes go through configuration and deployment.

### Entitlements — `/api/v1/billing/entitlements`

| Method | Path               | Auth                                        | Description                                                              |
| ------ | ------------------ | ------------------------------------------- | ------------------------------------------------------------------------ |
| `GET`  | `/entitlements/me` | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Active plan, subscription status, and typed features for current subject |

**Entitlement evaluation is tightly coupled with plan features** — this endpoint serves as the primary integration point between billing and platform access control. The response includes complete `PlanFeatures` data enabling downstream services to make fine-grained authorization decisions.

**Response Structure (with trial):**

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

**Response Structure (without trial):**

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

Returns `404` when no active subscription exists. Resolves subject by rollout mode.

### Plan Catalog (internal) — `/api/v1/billing/internal/plans`

| Method | Path                     | Auth | Description                                                            |
| ------ | ------------------------ | ---- | ---------------------------------------------------------------------- |
| `GET`  | `/internal/plans`        | None | Full plan feature catalog — used by gateway/service `PlanCatalogCache` |
| `GET`  | `/internal/plans/public` | None | Full plan catalog details for public pricing pages                     |

**Public within internal network** — no authentication required since the response contains only non-sensitive plan feature data (same as any public pricing page). Not exposed via a public gateway route. Backed by in-memory `PlanFeatureRegistry` — no DB reads.

**Response Structure:**

```json
[
    {
        "planCode": "basic-monthly",
        "displayName": "Basic Monthly",
        "description": "Entry-level plan for small teams.",
        "billingPeriod": "MONTHLY",
        "priceMinor": 1000,
        "currency": "USD",
        "scope": "TENANT",
        "active": true,
        "trialPeriodDays": 14,
        "pricingModel": "FLAT",
        "features": {
            "maxUsers": 5,
            "maxProjects": 3,
            "features": {}
        }
    },
    {
        "planCode": "pro-monthly",
        "displayName": "Pro Monthly",
        "description": "Pro plan for larger teams with priority support.",
        "billingPeriod": "MONTHLY",
        "priceMinor": 3000,
        "currency": "USD",
        "scope": "TENANT",
        "active": true,
        "trialPeriodDays": 0,
        "pricingModel": "FLAT",
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
]
```

### Webhooks — `/api/v1/billing/webhooks`

| Method | Path                      | Auth                    | Description                                      |
| ------ | ------------------------- | ----------------------- | ------------------------------------------------ |
| `POST` | `/webhooks/stripe`        | Stripe signature        | Receive and process Stripe webhook events        |
| `POST` | `/webhooks/lemon-squeezy` | Lemon Squeezy signature | Receive and process Lemon Squeezy webhook events |

> Auth legend: `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char tenantKey header; Stripe signature = `Stripe-Signature` header; Lemon Squeezy signature = `X-Signature` header (both verified against webhook secret).

## Events

### Consumed (from IAM via RabbitMQ)

| Routing Key          | Trigger                | Action                                                   |
| -------------------- | ---------------------- | -------------------------------------------------------- |
| `tenant.created`     | New tenant provisioned | Create payment gateway customer + initial billing record |
| `tenant.provisioned` | Tenant schema ready    | Activate billing for tenant                              |
| `tenant.suspended`   | Tenant suspended       | Suspend billing; send `ACCOUNT_SUSPENDED` email          |
| `tenant.deleted`     | Tenant deleted         | Clean up billing records                                 |

### Published (to RabbitMQ `iqkv.events` exchange)

| Routing Key                  | Trigger                                      |
| ---------------------------- | -------------------------------------------- |
| `subscription.created`       | Gateway subscription created                 |
| `subscription.cancelled`     | Gateway subscription cancelled               |
| `invoice.paid`               | Gateway invoice payment succeeded            |
| `invoice.created`            | Gateway invoice created                      |
| `invoice.finalized`          | Gateway invoice finalized                    |
| `invoice.updated`            | Gateway invoice updated                      |
| `payment.failed`             | Gateway invoice payment failed               |
| `refund.created`             | Gateway refund created                       |
| `notification.billing.email` | Any billing lifecycle change requiring email |

### Email Notifications (async, via notification service)

| Type                     | Trigger                                    |
| ------------------------ | ------------------------------------------ |
| `SUBSCRIPTION_ACTIVATED` | New subscription or tenant provisioned     |
| `SUBSCRIPTION_UPDATED`   | Plan or quantity changed                   |
| `SUBSCRIPTION_CANCELLED` | Subscription cancelled                     |
| `INVOICE_PAID`           | Payment succeeded                          |
| `PAYMENT_FAILED`         | Payment failed                             |
| `TRIAL_ENDING`           | Trial ends in 3 days (scheduled, 9 AM UTC) |
| `PAYMENT_OVERDUE`        | Payment past due (scheduled, 10 AM UTC)    |
| `BILLING_UPDATED`        | Billing settings changed                   |
| `ACCOUNT_SUSPENDED`      | Account suspended                          |

## Tech Stack

- Java 25 / Spring Boot 4.1
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase for schema migrations
- RabbitMQ (event consumption and publishing)
- Stripe Java SDK (active gateway adapter)
- ShedLock 7.x (trial-ending and overdue-payment scheduled jobs)
- Micrometer + Prometheus
- springdoc-openapi (Swagger UI)

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
# Edit .env.local — set STRIPE_SECRET_KEY and STRIPE_WEBHOOK_SECRET at minimum

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

| Variable                       | Default                         | Description                                                                 |
| ------------------------------ | ------------------------------- | --------------------------------------------------------------------------- |
| `PAYMENT_GATEWAY_TYPE`         | `STRIPE`                        | Active payment gateway adapter (`STRIPE` or `LEMON_SQUEEZY`)                |
| `ROLLOUT_MODE`                 | `MULTI_TENANT`                  | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT`                            |
| `DB_HOST`                      | `localhost`                     | PostgreSQL host                                                             |
| `DB_PORT`                      | `5432`                          | PostgreSQL port                                                             |
| `DB_NAME`                      | `billing`                       | Database name                                                               |
| `DB_USERNAME`                  | `billing`                       | Database user                                                               |
| `DB_PASSWORD`                  | `billing`                       | Database password                                                           |
| `RABBITMQ_HOST`                | `localhost`                     | RabbitMQ host                                                               |
| `RABBITMQ_PORT`                | `5672`                          | RabbitMQ AMQP port                                                          |
| `RABBITMQ_USERNAME`            | `billing`                       | RabbitMQ user                                                               |
| `RABBITMQ_PASSWORD`            | `billing`                       | RabbitMQ password                                                           |
| `STRIPE_SECRET_KEY`            | `sk_test_placeholder`           | Stripe secret key (required when `PAYMENT_GATEWAY_TYPE=STRIPE`)             |
| `STRIPE_WEBHOOK_SECRET`        | `whsec_placeholder`             | Stripe webhook signing secret                                               |
| `STRIPE_PORTAL_RETURN_URL`     | `http://localhost:3000/billing` | Stripe portal return URL                                                    |
| `LEMON_SQUEEZY_API_KEY`        | `ls_test_placeholder`           | Lemon Squeezy API key (required when `PAYMENT_GATEWAY_TYPE=LEMON_SQUEEZY`)  |
| `LEMON_SQUEEZY_WEBHOOK_SECRET` | `whsec_placeholder`             | Lemon Squeezy webhook signing secret                                        |
| `LEMON_SQUEEZY_STORE_ID`       | `12345`                         | Lemon Squeezy store ID (required when `PAYMENT_GATEWAY_TYPE=LEMON_SQUEEZY`) |
| `JWT_PUBLIC_KEY_PATH`          | `classpath:keys/public.pem`     | RS256 public key (from IAM)                                                 |
| `MAIL_HOST`                    | `localhost`                     | SMTP host                                                                   |
| `MAIL_PORT`                    | `587`                           | SMTP port                                                                   |
| `MAIL_FROM`                    | `noreply@iqkv.com`              | Sender address                                                              |
| `APP_BASE_URL`                 | `http://localhost:3000`         | Frontend base URL (used in email links)                                     |
| `DEFAULT_BILLING_EMAIL`        | _(empty)_                       | Fallback billing contact for single-tenant mode                             |
| `MESSAGING_ENABLED`            | `true`                          | Toggle RabbitMQ publishing (set `false` for local dev)                      |

> Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd` and fill in values per environment.

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
│   ├── event/          # Gateway-agnostic webhook event models (sealed interface)
│   ├── command/        # Gateway-agnostic command models
│   └── adapter/        # Gateway implementations
│       ├── stripe/     # Stripe implementation of PaymentGatewayPort
│       └── lemon-squeezy/ # Lemon Squeezy implementation of PaymentGatewayPort
├── plan/               # Plan catalog — CRUD, eligibility policy, scope validation
├── subscription/       # Subscription queries, subject resolution, entitlement evaluation
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

---

## 🧩 Boilerplate Architecture

- **Persistence**: MyBatis with XML mappers + PostgreSQL; Liquibase manages schema migrations; `subscriptions` table carries `subject_type` (`TENANT` | `USER`) and `subject_key` for mode-aware entitlement evaluation; `user_billing_settings` supports per-user billing in single-tenant mode
- **Messaging**: RabbitMQ consumer for tenant lifecycle events (`TENANT_CREATED`, `TENANT_PROVISIONED`, `TENANT_SUSPENDED`, `TENANT_DELETED`); publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `payment.failed`, and `notification.billing.email`; `ownerEmail` in `TENANT_CREATED` is optional — `BillingContactResolver` applies a fallback chain (event → `DEFAULT_BILLING_EMAIL` → null)
- **Security**: Spring Security + JWT RS256 validation (tokens issued by IAM, validated locally via public key)
- **Platform rollout mode**: Controlled via `ROLLOUT_MODE` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; `SubscriptionSubjectResolver` selects `TENANT` or `USER` subject scope based on active mode; service fails readiness on invalid/missing mode
- **Single-tenant mode**: `UserBillingSettingsServiceImpl` handles per-user billing settings; `SingleTenantSubscriptionSubjectResolver` scopes subscriptions to `subject_type=USER`; `BillingContactResolver` uses `DEFAULT_BILLING_EMAIL` fallback when `ownerEmail` is absent
- **Payment gateway abstraction**: Strategy pattern via `PaymentGatewayPort` — `createCustomer()` and `verifyAndParseWebhookEvent()` are the two gateway operations; `StripeGatewayAdapter` is the sole Stripe SDK consumer; `WebhookProcessingService` operates entirely on gateway-agnostic `GatewayWebhookEvent` sealed types; active gateway selected via `PAYMENT_GATEWAY_TYPE` env var
- **Plan catalog**: Config-driven only (`plan_catalog` table, `PLATFORM_ADMIN` authority for read access). `PlanFeatures` holds typed quota fields (`maxUsers`, `maxProjects`) and an extensible `Map<String, PlanFeature>` keyed by feature code (e.g. `priority_support`). Each `PlanFeature` carries `code`, `title`, `value`, and `description` — adding a new feature requires only a YAML change, no recompilation. Each plan now carries a `pricingModel` field (`FLAT` or `PER_SEAT`); `FLAT` is the default and all existing plans use it. For `PER_SEAT` plans, `SubscriptionService.resolveEffectiveQuantity` routes the correct Stripe line-item quantity at checkout and `validateSeatCount` enforces the `maxUsers` ceiling before the gateway call. `BillingSeedRunner` serializes features and `pricingModel` to the `plan_catalog` table on startup and synchronizes plans with the Stripe catalog (external Product and Price ID creation). There is no REST API for creating, updating, or deactivating plans — all catalog changes go through configuration and deployment. `PlanFeatureRegistry` holds an in-memory `planCode → PlanFeatures` map (and a parallel `planCode → PricingModel` map) used for zero-latency entitlement evaluation and the public internal plans endpoint (`/internal/plans`); **entitlement evaluation is tightly coupled with plan features** — the entitlements endpoint (`/entitlements/me`) returns complete feature data enabling fine-grained access control decisions; `PlanEligibilityPolicy` validates plan scope against active rollout mode; deactivation is a soft-delete managed via config change and redeployment
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (90% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
