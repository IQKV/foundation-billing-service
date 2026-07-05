## 📜 API Documentation

Base path: `/api/v1/billing`

All endpoints require a valid RS256 JWT issued by the IAM service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

---

## Plan-Based Feature Access Control

The billing service is the **single source of truth** for plan features across the platform. All access control decisions are driven by the `PlanEntitlement` configuration defined in YAML.

### Architecture Overview

```
YAML Config → PlanFeatureRegistry → /internal/plans → Gateway/Services → Access Control
```

1. **Plan Definition**: Features defined in `application-prd.yml` as typed `PlanEntitlement` records
2. **In-Memory Registry**: `PlanFeatureRegistry` loads features at startup for zero-latency lookups
3. **Internal API**: `/internal/plans` serves feature catalog to gateway and downstream services
4. **Distributed Caching**: Each service maintains local `PlanCatalogCache` with 10-minute refresh
5. **Enforcement**: Gateway enforces boolean features; services enforce quotas at write operations

### Feature Types

`PlanEntitlement` uses a split design:

- **`maxUsers`** (integer): Maximum users per tenant — typed quota field (0 = unlimited); for `PER_SEAT` plans also acts as the maximum purchasable seat count
- **`maxProjects`** (integer): Maximum projects per tenant — typed quota field (0 = unlimited)
- **`trialPeriodDays`** (integer): Free trial length in days (0 = no trial)
- **`features`** (map): Open `Map<String, PlanFeature>` keyed by feature code (e.g. `priority_support`). Each entry carries `code`, `title`, `value`, and `description`. Adding a new feature requires only a YAML change — no code recompilation.
- **`pricingModel`** (string): Pricing mode for the plan — `FLAT` (fixed price per billing period, default) or `PER_SEAT` (price × seat count per billing period). Returned by both `/internal/plans` and `/internal/plans/public`. Downstream services use this field to render the correct price label and to know whether `maxUsers` is a hard cap or a seat ceiling.

### Integration Points

- **`GET /entitlements/me`**: Client applications get current plan features
- **`GET /internal/plans`**: Gateway and services refresh their feature cache
- **JWT `plan_code` claim**: Propagated by gateway as `X-Plan-Code` header

---

### Billing Settings

| Method  | Path                           | Auth               | Description                               |
| ------- | ------------------------------ | ------------------ | ----------------------------------------- |
| `GET`   | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Get billing settings for a tenant         |
| `POST`  | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Create billing settings for a tenant      |
| `PATCH` | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Update billing settings (syncs to Stripe) |
| `POST`  | `/settings/{tenantKey}/portal` | JWT `TENANT_OWNER` | Create a Stripe Customer Portal session   |

The `tenantKey` path variable is validated against the authenticated tenant's JWT `tenant_id` claim.
Cross-tenant access returns `403 Forbidden`.

---

### Billing Settings (platform admin)

| Method   | Path                                          | Auth                 | Description                          |
| -------- | --------------------------------------------- | -------------------- | ------------------------------------ |
| `GET`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Get billing settings for a tenant    |
| `POST`   | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Create billing settings for a tenant |
| `PUT`    | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Replace billing settings             |
| `PATCH`  | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Partially update billing settings    |
| `DELETE` | `/admin/tenants/{tenantKey}/billing-settings` | JWT `PLATFORM_ADMIN` | Delete billing settings              |

`POST` returns `409 Conflict` if billing settings already exist for the tenant.
`DELETE` permanently removes the row (not a soft-delete).

---

### User Billing Settings (single-tenant mode)

| Method | Path                    | Auth | Description                             |
| ------ | ----------------------- | ---- | --------------------------------------- |
| `POST` | `/user-settings/portal` | JWT  | Create a Stripe Customer Portal session |

Only active in `SINGLE_TENANT` rollout mode. Returns the portal session URL for the authenticated user.

---

### Subscriptions

| Method | Path                                          | Auth                           | Description                                 |
| ------ | --------------------------------------------- | ------------------------------ | ------------------------------------------- |
| `GET`  | `/subscriptions/{tenantKey}/active`           | JWT `TENANT_OWNER`             | Get active subscription for a tenant        |
| `GET`  | `/subscriptions/{tenantKey}`                  | JWT `TENANT_OWNER`             | Get all subscriptions for a tenant          |
| `POST` | `/subscriptions/{tenantKey}/checkout`         | JWT `TENANT_OWNER`             | Create a Checkout Session for subscription  |
| `POST` | `/subscriptions/{tenantKey}/{subscriptionId}` | JWT `TENANT_OWNER`             | Update an existing subscription             |
| `GET`  | `/subscriptions/me/active`                    | JWT `TENANT_OWNER` or `MEMBER` | Get active subscription for current subject |
| `GET`  | `/subscriptions/me`                           | JWT `TENANT_OWNER` or `MEMBER` | Get all subscriptions for current subject   |

Subscription data is a local cache of Stripe state — no payment gateway round-trips are made.
The `/{tenantKey}` paths validate `tenantKey` against the JWT `tenant_id` claim; cross-tenant access returns `403 Forbidden`.

`POST /checkout` returns the Stripe Checkout Session URL for onboarding flow, supporting `trial_period_days`, `quantity`, `allow_promotion_codes`, etc.
`POST /{subscriptionId}` supports upgrades/downgrades, quantity changes, and proration behavior control.

---

### Payments

| Method | Path                            | Auth               | Description                  |
| ------ | ------------------------------- | ------------------ | ---------------------------- |
| `POST` | `/payments/{tenantKey}/refund`  | JWT `TENANT_OWNER` | Create a refund for a tenant |
| `GET`  | `/payments/{tenantKey}/refunds` | JWT `TENANT_OWNER` | List refunds for a tenant    |

`POST /{tenantKey}/refund` creates a full or partial refund for a given payment (PaymentIntent or Charge ID).
`GET /{tenantKey}/refunds` returns all refunds for the tenant ordered by occurrence date.
The `{tenantKey}` is validated against the authenticated tenant's JWT `tenant_id` claim.

---

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

`DELETE` permanently removes the subscription record.

---

### Refunds (platform admin)

| Method | Path                  | Auth                 | Description                            |
| ------ | --------------------- | -------------------- | -------------------------------------- |
| `GET`  | `/admin/refunds`      | JWT `PLATFORM_ADMIN` | List all refunds (paginated, filtered) |
| `GET`  | `/admin/refunds/{id}` | JWT `PLATFORM_ADMIN` | Get refund by ID                       |

`GET /admin/refunds` supports pagination and filtering by `tenantKey`.

---

### Gateway Configuration (platform admin)

| Method | Path             | Auth                 | Description                              |
| ------ | ---------------- | -------------------- | ---------------------------------------- |
| `GET`  | `/admin/gateway` | JWT `PLATFORM_ADMIN` | Get active payment gateway configuration |

---

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

> [!NOTE]
> **Config-driven catalog**: Plans are defined in `application.yml` under `iqkv.billing.stripe.schema.products` and synchronized with Stripe at startup by `BillingSeedRunner`. There is no REST API for creating, updating, or deactivating plans — all catalog changes go through configuration and deployment.

---

### Entitlements

| Method | Path               | Auth                           | Description                                                              |
| ------ | ------------------ | ------------------------------ | ------------------------------------------------------------------------ |
| `GET`  | `/entitlements/me` | JWT `TENANT_OWNER` or `MEMBER` | Active plan, subscription status, and typed features for current subject |

**Entitlement evaluation is tightly coupled with plan features** — this endpoint serves as the primary integration point between billing and platform access control. The response includes the complete `PlanEntitlement` record for the tenant's active plan, enabling fine-grained access control decisions in client applications.

**Response Example (with trial):**

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

**Response Example (without trial):**

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

**Feature Fields:**

- **`maxUsers`** (integer): Maximum users allowed (0 = unlimited)
- **`maxProjects`** (integer): Maximum projects allowed (0 = unlimited)
- **`features`** (map): Display/boolean features keyed by code — check `value == "true"` for boolean flags

Returns `404` when no active subscription exists. Resolves subject by rollout mode (tenant in multi-tenant, user in single-tenant).

---

### Plan Catalog (internal service-to-service)

| Method | Path                     | Auth | Description                                              |
| ------ | ------------------------ | ---- | -------------------------------------------------------- |
| `GET`  | `/internal/plans`        | None | Full plan feature catalog for gateway and service caches |
| `GET`  | `/internal/plans/public` | None | Full plan catalog details for public pricing pages       |

**Public within internal network** — no authentication required since the response contains only non-sensitive plan feature data (same as any public pricing page).

- `GET /internal/plans`: Used by the gateway and downstream services to populate their local `PlanCatalogCache` at startup and on periodic refresh. Backed by in-memory `PlanFeatureRegistry` — no DB reads.
- `GET /internal/plans/public`: Exposes active plans with full details (display name, description, price, billing period, features) for public website pricing pages. Reads from static YAML properties — no DB reads.

**Response Example:**

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

**Integration Pattern:**
This endpoint is the foundation of the platform's **plan-based feature access control system**:

1. **Gateway Integration**: Gateway `PlanCatalogCache` refreshes from this endpoint every 10 minutes
2. **Downstream Services**: Each service maintains its own `PlanCatalogCache` for quota enforcement
3. **Zero Hot-Path Calls**: All feature checks use in-memory cache, no network requests during user operations
4. **Fail-Safe Security**: Services deny access when cache is empty or plan is unknown

Not exposed via a public gateway route — internal network access only.

---

### Webhooks

| Method | Path                      | Auth                    | Description                         |
| ------ | ------------------------- | ----------------------- | ----------------------------------- |
| `POST` | `/webhooks/stripe`        | Stripe signature        | Ingest Stripe webhook events        |
| `POST` | `/webhooks/lemon-squeezy` | Lemon Squeezy signature | Ingest Lemon Squeezy webhook events |

No JWT required. Secured by gateway-specific signature verification (`Stripe-Signature` for Stripe, `X-Signature` for Lemon Squeezy). Always returns `200 OK` after the idempotency check to prevent retries on business logic failures.

### Webhook Logs

| Method | Path                       | Auth                                     | Description                                      |
| ------ | -------------------------- | ---------------------------------------- | ------------------------------------------------ |
| `GET`  | `/webhook-logs/me`         | JWT `TENANT_OWNER`, `ADMIN`, or `MEMBER` | List webhook logs for current subject            |
| `GET`  | `/webhook-logs/me/{id}`    | JWT `TENANT_OWNER`, `ADMIN`, or `MEMBER` | Get single webhook log by ID for current subject |
| `GET`  | `/admin/webhook-logs`      | JWT `PLATFORM_ADMIN`                     | List all webhook logs (paginated, filterable)    |
| `GET`  | `/admin/webhook-logs/{id}` | JWT `PLATFORM_ADMIN`                     | Get single webhook log entry by UUID             |

- `GET /webhook-logs/me` and `GET /admin/webhook-logs` support query parameters: `page`, `size`, `sortBy`, `sortDir`, `search`, `status`, `tenantKey`
- `search` matches on `tenantKey`, `eventType`, or `externalEventId`
- `status` filters by webhook processing status (`RECEIVED`, `PROCESSED`, `FAILED`)
- `tenantKey` filters logs by tenant (single-tenant mode uses user key)
- `sortBy` accepts: `tenantKey`, `eventType`, `status`, `receivedAt`, `processedAt`

---

### Interactive Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running locally.
