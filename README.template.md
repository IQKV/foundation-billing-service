# Foundation Billing Service 💳

<!-- TEMPLATE: Copy relevant sections into README.md and replace placeholders. Remove guidance blocks when done. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title to your service name and add a logo if desired.
2. Add badges (build, license) under the title.
3. Fill each section with your actual service content.
4. Update the API table to reflect actual endpoints and auth requirements.
5. Update the environment variables table to match your `application.yml` bindings.
6. Update the project structure tree if your bounded contexts differ.
7. Remove this guidance block after customizing.

</details>

- Add your service logo.
- Write a short introduction — what the service does and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: `![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-nodejs-project.yml?label=CI)`
- License: `![License](https://img.shields.io/github/license/ORG/REPO)`
- Java: `![Java](https://img.shields.io/badge/java-25-blue)`
- Spring Boot: `![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)`

</details>

## About

The Foundation Billing Service manages subscription lifecycle and payment processing for the IQKV platform. It acts as the bridge between platform tenants (or users in single-tenant mode) and Stripe. Key business responsibilities:

- Provisions a Stripe customer automatically when a tenant is created
- Tracks subscription state (active, trialing, past due, cancelled) as a local cache of Stripe
- Enforces plan eligibility rules — plans are scoped to `MULTI_TENANT` or `SINGLE_TENANT` mode
- Processes Stripe webhook events idempotently and publishes downstream lifecycle events
- Sends transactional billing emails (payment receipts, trial reminders, overdue notices) via async notification events
- Supports both per-tenant billing (multi-tenant) and per-user billing (single-tenant) through a strategy pattern

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Lifecycle Events & Email Notifications](./docs/lifecycle-events.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/billing`

### Plan Catalog — `/api/v1/billing/plans`

| Method   | Path                | Auth                    | Description                     |
| -------- | ------------------- | ----------------------- | ------------------------------- |
| `GET`    | `/plans`            | JWT                     | List all active plans           |
| `GET`    | `/plans/{planCode}` | JWT                     | Get plan by code                |
| `POST`   | `/plans`            | JWT `PLATFORM_OPERATOR` | Create a plan                   |
| `PUT`    | `/plans/{planCode}` | JWT `PLATFORM_OPERATOR` | Replace a plan                  |
| `DELETE` | `/plans/{planCode}` | JWT `PLATFORM_OPERATOR` | Deactivate a plan (soft-delete) |

### Subscriptions — `/api/v1/billing/subscriptions`

| Method | Path                                | Auth                                        | Description                                 |
| ------ | ----------------------------------- | ------------------------------------------- | ------------------------------------------- |
| `GET`  | `/subscriptions/{tenantKey}/active` | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get active subscription for tenant          |
| `GET`  | `/subscriptions/{tenantKey}`        | JWT `TENANT_OWNER` + `X-Tenant-ID`          | Get all subscriptions for tenant            |
| `GET`  | `/subscriptions/me/active`          | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get active subscription for current subject |
| `GET`  | `/subscriptions/me`                 | JWT `TENANT_OWNER`/`MEMBER` + `X-Tenant-ID` | Get all subscriptions for current subject   |

> Subscription data is a local cache — no Stripe round-trips on read. Subject resolves to tenant (multi-tenant) or user (single-tenant) based on `ROLLOUT_MODE`.

### Billing Settings — `/api/v1/billing/settings`

| Method  | Path                    | Auth                               | Description                                        |
| ------- | ----------------------- | ---------------------------------- | -------------------------------------------------- |
| `GET`   | `/settings/{tenantKey}` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Get billing settings (contact email, tax ID, etc.) |
| `PATCH` | `/settings/{tenantKey}` | JWT `TENANT_OWNER` + `X-Tenant-ID` | Update billing settings                            |

### Webhooks — `/api/v1/billing/webhooks`

| Method | Path               | Auth             | Description                               |
| ------ | ------------------ | ---------------- | ----------------------------------------- |
| `POST` | `/webhooks/stripe` | Stripe signature | Receive and process Stripe webhook events |

> Auth legend: `JWT` = valid Bearer token; `JWT ROLE` = JWT with that authority; `X-Tenant-ID` = 8-char tenantKey header; Stripe signature = `Stripe-Signature` header verified against webhook secret.

## Events

### Consumed (from IAM via RabbitMQ)

| Routing Key          | Trigger                | Action                                          |
| -------------------- | ---------------------- | ----------------------------------------------- |
| `tenant.created`     | New tenant provisioned | Create Stripe customer + initial billing record |
| `tenant.provisioned` | Tenant schema ready    | Activate billing for tenant                     |
| `tenant.suspended`   | Tenant suspended       | Suspend billing; send `ACCOUNT_SUSPENDED` email |
| `tenant.deleted`     | Tenant deleted         | Clean up billing records                        |

### Published (to RabbitMQ `iqkv.events` exchange)

| Routing Key                  | Trigger                                      |
| ---------------------------- | -------------------------------------------- |
| `subscription.created`       | Stripe subscription created                  |
| `subscription.cancelled`     | Stripe subscription cancelled                |
| `invoice.paid`               | Stripe invoice payment succeeded             |
| `payment.failed`             | Stripe invoice payment failed                |
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

- Java 25 / Spring Boot 3.x
- MyBatis 3.x + PostgreSQL
- Liquibase for schema migrations
- RabbitMQ (event consumption and publishing)
- Stripe Java SDK (subscription and customer management)
- ShedLock (trial-ending and overdue-payment scheduled jobs)
- Micrometer + Prometheus
- Thymeleaf (email templates)
- springdoc-openapi (Swagger UI)

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0 (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/ORG/REPO.git
cd REPO

# Install git hooks
pnpm install

# Copy environment variables
cp .env.example .env.local
# Edit .env.local — set STRIPE_SECRET_KEY and STRIPE_WEBHOOK_SECRET at minimum

# Start dependencies (PostgreSQL on :5432, RabbitMQ on :5673)
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable                | Default                     | Description                                            |
| ----------------------- | --------------------------- | ------------------------------------------------------ |
| `ROLLOUT_MODE`          | `MULTI_TENANT`              | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT`       |
| `DB_HOST`               | `localhost`                 | PostgreSQL host                                        |
| `DB_PORT`               | `5432`                      | PostgreSQL port                                        |
| `DB_NAME`               | `billing`                   | Database name                                          |
| `DB_USERNAME`           | `billing`                   | Database user                                          |
| `DB_PASSWORD`           | `billing`                   | Database password                                      |
| `RABBITMQ_HOST`         | `localhost`                 | RabbitMQ host                                          |
| `RABBITMQ_PORT`         | `5673`                      | RabbitMQ AMQP port                                     |
| `RABBITMQ_USERNAME`     | `billing`                   | RabbitMQ user                                          |
| `RABBITMQ_PASSWORD`     | `billing`                   | RabbitMQ password                                      |
| `STRIPE_SECRET_KEY`     | `sk_test_placeholder`       | Stripe secret key                                      |
| `STRIPE_WEBHOOK_SECRET` | `whsec_placeholder`         | Stripe webhook signing secret                          |
| `JWT_PUBLIC_KEY_PATH`   | `classpath:keys/public.pem` | RS256 public key (from IAM)                            |
| `MAIL_HOST`             | `localhost`                 | SMTP host                                              |
| `MAIL_PORT`             | `587`                       | SMTP port                                              |
| `MAIL_FROM`             | `noreply@iqkv.com`          | Sender address                                         |
| `APP_BASE_URL`          | `http://localhost:3000`     | Frontend base URL (used in email links)                |
| `DEFAULT_BILLING_EMAIL` | _(empty)_                   | Fallback billing contact for single-tenant mode        |
| `MESSAGING_ENABLED`     | `true`                      | Toggle RabbitMQ publishing (set `false` for local dev) |

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
docker build -t ORG/REPO:latest .

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
├── plan/           # Plan catalog — CRUD, eligibility policy, scope validation
├── subscription/   # Subscription queries, subject resolution, entitlement evaluation
├── settings/       # Tenant billing settings (contact email, tax ID, Stripe customer ID)
├── userbilling/    # Per-user billing settings (single-tenant mode)
├── webhook/        # Stripe webhook ingestion, idempotent processing, event log
├── tenancy/        # Tenant context extraction from X-Tenant-ID header
├── shared/         # Common exceptions, utilities
└── infrastructure/ # Spring config, security, MyBatis, RabbitMQ, Stripe setup
```

---

<details>
  <summary><strong>✅ Pre-publish checklist (remove in final README)</strong></summary>

- [ ] Title updated and logo added
- [ ] Badges added (CI, license)
- [ ] About section completed
- [ ] API table reflects actual endpoints and auth requirements
- [ ] Tech stack updated (remove unused entries, add missing ones)
- [ ] Environment variables table matches `application.yml` bindings
- [ ] Project structure tree updated to match actual packages
- [ ] Links verified (docs, external resources)
- [ ] Guidance blocks removed before publishing

</details>

---

## 🧩 Boilerplate Architecture

- **Persistence**: MyBatis with XML mappers + PostgreSQL; Liquibase manages schema migrations; `subscriptions` table carries `subject_type` (`TENANT` | `USER`) and `subject_key` for mode-aware entitlement evaluation; `user_billing_settings` supports per-user billing in single-tenant mode
- **Messaging**: RabbitMQ consumer for tenant lifecycle events (`TENANT_CREATED`, `TENANT_PROVISIONED`, `TENANT_SUSPENDED`, `TENANT_DELETED`); publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `payment.failed`, and `notification.billing.email`; `ownerEmail` in `TENANT_CREATED` is optional — `BillingContactResolver` applies a fallback chain (event → `DEFAULT_BILLING_EMAIL` → null)
- **Security**: Spring Security + JWT RS256 validation (tokens issued by IAM, validated locally via public key)
- **Platform rollout mode**: Controlled via `ROLLOUT_MODE` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; `SubscriptionSubjectResolver` selects `TENANT` or `USER` subject scope based on active mode; service fails readiness on invalid/missing mode
- **Single-tenant mode**: `UserBillingSettingsServiceImpl` handles per-user billing settings; `SingleTenantSubscriptionSubjectResolver` scopes subscriptions to `subject_type=USER`; `BillingContactResolver` uses `DEFAULT_BILLING_EMAIL` fallback when `ownerEmail` is absent
- **Stripe integration**: Stripe Connect wrapper — no custom billing logic; subscriptions and invoices managed on Stripe's side; webhook processing is idempotent (logged in `webhook_log`); `PaymentGatewayClient.createCustomer` accepts null email safely
- **Plan catalog**: Operator-managed only (`plan_catalog` table, `PLATFORM_OPERATOR` authority required); no end-user plan CRUD; `PlanEligibilityPolicy` validates plan scope against active rollout mode; deactivation is a soft-delete
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (90% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
