# Foundation Billing Service 💳

Billing and subscription management microservice for the Key Value Platform. Provides a gateway-agnostic payment abstraction layer — handles tenant-to-customer mapping, plan catalog management, webhook ingestion, and lifecycle event publishing. No custom billing logic lives here.

## About

The Billing service owns the payment gateway integration layer for the platform:

- **Automatic customer provisioning** — listens for `tenant.created` and `tenant.provisioned` events on RabbitMQ and creates a payment gateway customer per tenant; the `external_customer_id` is stored in `billing_settings`
- **Multi-gateway strategy** — `PaymentGatewayPort` interface (Strategy pattern + Hexagonal Architecture) decouples business logic from gateway SDKs; Stripe is the active implementation, additional gateways are reserved
- **Plan catalog** — platfom admin-managed plan definitions scoped to `MULTI_TENANT` or `SINGLE_TENANT` mode; `PlanEligibilityPolicy` validates scope against active rollout mode
- **Billing settings** — each tenant has a 1:1 `billing_settings` record that is the single source of truth for payment gateway customer metadata; decoupled from IAM users by design
- **Billing email** — a separate `billing_email` field allows finance teams to receive invoices without a system account
- **Tax ID / VAT/GST** — stored in `billing_settings` for compliant B2B invoices
- **Webhook processing** — payment gateway webhooks are ingested and processed idempotently via a gateway-agnostic orchestrator; duplicate delivery is safe
- **Lifecycle events** — publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `invoice.created`, `invoice.finalized`, `invoice.updated`, `payment.failed`, and `refund.created` to the platform event bus
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

| Method | Path                                          | Auth                                        | Description                                 |
| ------ | --------------------------------------------- | ------------------------------------------- | ------------------------------------------- |
| `GET`  | `/subscriptions/{tenantKey}/active`           | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get active subscription for tenant          |
| `GET`  | `/subscriptions/{tenantKey}`                  | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get all subscriptions for tenant            |
| `POST` | `/subscriptions/{tenantKey}/checkout`         | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Create a Checkout Session for subscription  |
| `POST` | `/subscriptions/{tenantKey}/{subscriptionId}` | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Update an existing subscription             |
| `GET`  | `/subscriptions/me/active`                    | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get active subscription for current subject |
| `GET`  | `/subscriptions/me`                           | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get all subscriptions for current subject   |

### Payments — `/api/v1/billing/payments`

| Method | Path               | Auth                               | Description     |
| ------ | ------------------ | ---------------------------------- | --------------- |
| `POST` | `/payments/refund` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Create a refund |

> Subscription data is a local cache — no gateway round-trips on read. Subject resolves to tenant (multi-tenant) or user (single-tenant) based on `ROLLOUT_MODE`.

### Subscriptions (platform admin) — `/api/v1/billing/admin/subscriptions`

| Method   | Path                         | Auth                 | Description                                |
| -------- | ---------------------------- | -------------------- | ------------------------------------------ |
| `GET`    | `/admin/subscriptions`       | JWT `PLATFORM_ADMIN` | List subscriptions (paginated, filterable) |
| `GET`    | `/admin/subscriptions/count` | JWT `PLATFORM_ADMIN` | Count all subscriptions                    |
| `GET`    | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Get subscription by ID                     |
| `PATCH`  | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Partially update subscription              |
| `DELETE` | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Delete subscription                        |

### Plan Catalog — `/api/v1/billing/plans`

| Method | Path                | Auth                    | Description           |
| ------ | ------------------- | ----------------------- | --------------------- |
| `GET`  | `/plans`            | JWT (any authenticated) | List all active plans |
| `GET`  | `/plans/{planCode}` | JWT (any authenticated) | Get plan by planCode  |

### Plan Catalog (platform admin) — `/api/v1/billing/admin/plans`

| Method   | Path                      | Auth                 | Description                                     |
| -------- | ------------------------- | -------------------- | ----------------------------------------------- |
| `GET`    | `/admin/plans`            | JWT `PLATFORM_ADMIN` | List all plans (including inactive)             |
| `GET`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Get plan by planCode                            |
| `POST`   | `/admin/plans`            | JWT `PLATFORM_ADMIN` | Create a plan                                   |
| `PUT`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Replace a plan                                  |
| `PATCH`  | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Partially update a plan                         |
| `DELETE` | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Deactivate a plan (soft-delete, `active=false`) |

### Webhooks — `/api/v1/billing/webhooks`

| Method | Path               | Auth             | Description                               |
| ------ | ------------------ | ---------------- | ----------------------------------------- |
| `POST` | `/webhooks/stripe` | Stripe signature | Receive and process Stripe webhook events |

> Auth legend: `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char tenantKey header; Stripe signature = `Stripe-Signature` header verified against webhook secret.

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

- Java 25 / Spring Boot 4.0
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

# Start dependencies (PostgreSQL on :5432, RabbitMQ on :5672)
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable                   | Default                         | Description                                                     |
| -------------------------- | ------------------------------- | --------------------------------------------------------------- |
| `PAYMENT_GATEWAY_TYPE`     | `STRIPE`                        | Active payment gateway adapter (`STRIPE`)                       |
| `ROLLOUT_MODE`             | `MULTI_TENANT`                  | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT`                |
| `DB_HOST`                  | `localhost`                     | PostgreSQL host                                                 |
| `DB_PORT`                  | `5432`                          | PostgreSQL port                                                 |
| `DB_NAME`                  | `billing`                       | Database name                                                   |
| `DB_USERNAME`              | `billing`                       | Database user                                                   |
| `DB_PASSWORD`              | `billing`                       | Database password                                               |
| `RABBITMQ_HOST`            | `localhost`                     | RabbitMQ host                                                   |
| `RABBITMQ_PORT`            | `5672`                          | RabbitMQ AMQP port                                              |
| `RABBITMQ_USERNAME`        | `billing`                       | RabbitMQ user                                                   |
| `RABBITMQ_PASSWORD`        | `billing`                       | RabbitMQ password                                               |
| `STRIPE_SECRET_KEY`        | `sk_test_placeholder`           | Stripe secret key (required when `PAYMENT_GATEWAY_TYPE=STRIPE`) |
| `STRIPE_WEBHOOK_SECRET`    | `whsec_placeholder`             | Stripe webhook signing secret                                   |
| `STRIPE_PORTAL_RETURN_URL` | `http://localhost:3000/billing` | Stripe portal return URL                                        |
| `JWT_PUBLIC_KEY_PATH`      | `classpath:keys/public.pem`     | RS256 public key (from IAM)                                     |
| `MAIL_HOST`                | `localhost`                     | SMTP host                                                       |
| `MAIL_PORT`                | `587`                           | SMTP port                                                       |
| `MAIL_FROM`                | `noreply@iqkv.com`              | Sender address                                                  |
| `APP_BASE_URL`             | `http://localhost:3000`         | Frontend base URL (used in email links)                         |
| `DEFAULT_BILLING_EMAIL`    | _(empty)_                       | Fallback billing contact for single-tenant mode                 |
| `MESSAGING_ENABLED`        | `true`                          | Toggle RabbitMQ publishing (set `false` for local dev)          |

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

# Run full stack (service + dependencies)
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                   | Description                 |
| -------------------------- | --------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes |
| `GET /actuator/metrics`    | Application metrics         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint  |
| `GET /swagger-ui.html`     | API documentation           |

## Project Structure

```
src/main/java/com/iqkv/foundation/billingservice/
├── gateway/            # Payment gateway abstraction (Strategy pattern + Hexagonal Architecture)
│   ├── port/           # PaymentGatewayPort interface
│   ├── event/          # Gateway-agnostic webhook event models (sealed interface)
│   ├── command/        # Gateway-agnostic command models
│   └── adapter/stripe/ # Stripe implementation of PaymentGatewayPort
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
- **Plan catalog**: Admin-managed only (`plan_catalog` table, `PLATFORM_ADMIN` authority required); no end-user plan CRUD; `PlanEligibilityPolicy` validates plan scope against active rollout mode; deactivation is a soft-delete
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (90% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
