# Project Name 💳

<!-- TEMPLATE: This README.template.md is a starter template. Copy parts into your real README.md and replace placeholders. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title above to your service name and optionally add a logo right below it.
2. Add badges (build, license) under the title.
3. Fill each section below with your actual service content.
4. Update the API table to reflect your actual endpoints and auth requirements.
5. Update the environment variables table to match your `application.yml` bindings.
6. Update the project structure tree if your bounded contexts differ.
7. Remove this guidance block after you finish customizing.

</details>

- Add your service logo.
- Write a short introduction — what the service does and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: <code>![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-nodejs-project.yml?label=CI)</code>
- License: <code>![License](https://img.shields.io/github/license/ORG/REPO)</code>
- Java: <code>![Java](https://img.shields.io/badge/java-25-blue)</code>
- Spring Boot: <code>![Spring Boot](https://img.shields.io/badge/spring--boot-4.x-brightgreen)</code>

</details>

## About

Describe the service's responsibilities in plain language. Focus on what it owns, not how it works internally. Good prompts:

- What domain does it manage?
- What are the key business rules it enforces?
- What events does it consume and publish?
- What does it produce for other services or external systems (Stripe, webhooks)?

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/billing`

| Method   | Path             | Auth        | Description        |
| -------- | ---------------- | ----------- | ------------------ |
| `POST`   | `/resource`      | public      | Create a resource  |
| `GET`    | `/resource/{id}` | JWT         | Get resource by ID |
| `PATCH`  | `/resource/{id}` | JWT `ROLE`  | Update resource    |
| `DELETE` | `/resource/{id}` | JWT `ADMIN` | Delete resource    |

> Replace with your actual endpoints. Document the auth requirement for each — `public`, `X-Tenant-ID`, `JWT`, or `JWT ROLE_NAME`.

## Tech Stack

- Java 25 / Spring Boot 4.x
- MyBatis 3.x + PostgreSQL (no JPA)
- Liquibase for schema migrations
- RabbitMQ for async event consumption and publishing
- Stripe Java SDK (if applicable)
- ShedLock for distributed scheduled jobs (if applicable)
- Micrometer + Prometheus

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
# Edit .env.local with your local values

# Start dependencies (PostgreSQL on :5432, RabbitMQ on :5673)
docker compose up -d

# Run the service
./mvnw spring-boot:run -Pdev
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable                | Default     | Description                   |
| ----------------------- | ----------- | ----------------------------- |
| `DB_HOST`               | `localhost` | PostgreSQL host               |
| `DB_PORT`               | `5432`      | PostgreSQL port               |
| `DB_NAME`               | `billing`   | Database name                 |
| `DB_USERNAME`           | `billing`   | Database user                 |
| `DB_PASSWORD`           | `billing`   | Database password             |
| `RABBITMQ_HOST`         | `localhost` | RabbitMQ host                 |
| `RABBITMQ_PORT`         | `5673`      | RabbitMQ AMQP port            |
| `RABBITMQ_USERNAME`     | `billing`   | RabbitMQ user                 |
| `RABBITMQ_PASSWORD`     | `billing`   | RabbitMQ password             |
| `STRIPE_API_KEY`        | —           | Stripe secret key             |
| `STRIPE_WEBHOOK_SECRET` | —           | Stripe webhook signing secret |

> Add or remove rows to match your `application.yml` environment variable bindings. Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd`.

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
├── settings/           # e.g. billing settings — Stripe customer metadata, tax IDs
├── webhook/            # e.g. Stripe webhook ingestion and idempotent processing
├── subscription/       # e.g. subscription lifecycle events
├── infrastructure/     # Spring config, security, MyBatis, RabbitMQ setup
└── shared/             # Common exceptions, utilities, value objects
```

> Update bounded context names to match your actual package structure.

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

- **Persistence**: MyBatis with XML mappers + PostgreSQL; Liquibase manages schema migrations; `user_billing_settings` table supports per-user billing in single-tenant mode; `subscriptions` table carries `subject_type` (`TENANT` | `USER`) and `subject_key` for mode-aware entitlement evaluation
- **Messaging**: RabbitMQ consumer for tenant lifecycle events (`TENANT_CREATED`, `TENANT_PROVISIONED`, `TENANT_SUSPENDED`, `TENANT_DELETED`); publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, `payment.failed`; `ownerEmail` in `TENANT_CREATED` is optional — `BillingContactResolver` applies a fallback chain (event → `iqkv.billing.default-contact-email` → null)
- **Security**: Spring Security + JWT RS256 validation (tokens issued by IAM, validated locally)
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; service fails readiness on invalid/missing mode; `SubscriptionSubjectResolver` selects `TENANT` or `USER` subject scope based on active mode
- **Single-tenant mode**: `UserBillingSettingsServiceImpl` handles per-user billing settings; `SingleTenantSubscriptionSubjectResolver` scopes subscriptions to `subject_type=USER`; `BillingContactResolver` uses configured fallback email when `ownerEmail` is absent
- **Stripe integration**: Stripe Connect wrapper — no custom billing logic; subscriptions and invoices managed on Stripe's side; webhook processing is idempotent; `PaymentGatewayClient.createCustomer` accepts null email safely
- **Plan catalog**: Operator-managed only (`plan_catalog` table); no end-user plan CRUD; `PlanEligibilityPolicy` validates plan scope against active rollout mode
- **Observability**: Micrometer + Prometheus; structured JSON logging with Logstash encoder; health probes for Kubernetes
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (90% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
