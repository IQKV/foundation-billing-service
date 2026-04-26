> ## 🤔 What is this service all about?
>
> - Billing and subscription management microservice for the IQ Key Value platform.
> - Stripe Connect wrapper — no custom billing logic; subscriptions, invoices, and the dashboard are managed on Stripe's side.
> - Make the project easy to maintain with **8 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...

---

# 💳 IQ Key Value Billing Service

Billing and subscription management microservice. Acts as a Stripe Connect wrapper — handles tenant-to-customer mapping, webhook ingestion, and lifecycle event publishing. No custom billing logic lives here.

![CI](https://img.shields.io/github/actions/workflow/status/IQKV/foundation-billing-service/build-nodejs-project.yml?label=CI)
![License](https://img.shields.io/github/license/IQKV/foundation-billing-service)

## About

The Billing service owns the Stripe integration layer for the platform:

- **Automatic customer provisioning** — listens for `tenant.provisioned` events on RabbitMQ and creates a Stripe customer per tenant; the `stripe_customer_id` is stored in `billing_settings`
- **Billing settings** — each tenant has a 1:1 `billing_settings` record that is the single source of truth for Stripe customer metadata; decoupled from IAM users by design
- **Billing email** — a separate `billing_email` field allows finance teams to receive invoices without a system account
- **Tax ID / VAT/GST** — stored in `billing_settings` and synced to Stripe via `CustomerUpdateParams` for compliant B2B invoices
- **Webhook processing** — Stripe webhooks are ingested and processed idempotently; duplicate delivery is safe
- **Lifecycle events** — publishes `subscription.created`, `subscription.cancelled`, `invoice.paid`, and `payment.failed` to the platform event bus
- **Outbox pattern** — any update to billing settings syncs to Stripe reliably via the transactional outbox; no silent failures

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1/billing`

| Method  | Path                    | Auth               | Description                               |
| ------- | ----------------------- | ------------------ | ----------------------------------------- |
| `GET`   | `/settings/{tenantKey}` | JWT `TENANT_OWNER` | Get billing settings for a tenant         |
| `PATCH` | `/settings/{tenantKey}` | JWT `TENANT_OWNER` | Update billing settings (syncs to Stripe) |
| `POST`  | `/webhooks/stripe`      | Stripe signature   | Ingest Stripe webhook events              |

## Tech Stack

- Java 25 / Spring Boot 4.0
- MyBatis 3.x (no JPA) + PostgreSQL 17
- Liquibase for schema migrations
- RabbitMQ for async event consumption and publishing
- Stripe Java SDK
- ShedLock 7.x for distributed scheduled jobs
- Micrometer + Prometheus

## Prerequisites

- JDK 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0 (git hooks)
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

# Run full stack (service + dependencies)
docker compose -f compose.container.yaml up -d
```

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:21-jdk-alpine`, the runtime stage uses `eclipse-temurin:21-jre-alpine` with a non-root `appuser` and layered JAR extraction for optimal cache reuse.

## Monitoring

| Endpoint                   | Description                 |
| -------------------------- | --------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes |
| `GET /actuator/metrics`    | Application metrics         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint  |
| `GET /swagger-ui.html`     | API documentation           |

A Grafana dashboard (`docker/grafana/`) provides real-time visibility into service health and JVM metrics using Prometheus as the data source.

## Project Structure

```
src/main/java/com/iqkv/foundation/billingservice/
├── settings/           # Billing settings — Stripe customer metadata, tax IDs, billing email
├── webhook/            # Stripe webhook ingestion and idempotent event processing
├── subscription/       # Subscription lifecycle — event publishing on status changes
├── infrastructure/     # Spring config, security, MyBatis, RabbitMQ setup
└── shared/             # Common exceptions, utilities, value objects
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
