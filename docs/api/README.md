## 📜 API Documentation

Base path: `/api/v1/billing`

All endpoints require a valid RS256 JWT issued by the IAM service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

---

## Plan-Based Feature Access Control

The billing service is the **single source of truth** for plan features across the platform. All access control decisions are driven by the `PlanFeatures` configuration defined in YAML.

### Architecture Overview

```
YAML Config → PlanFeatureRegistry → /internal/plans → Gateway/Services → Access Control
```

1. **Plan Definition**: Features defined in `application-prd.yml` as typed `PlanFeatures` records
2. **In-Memory Registry**: `PlanFeatureRegistry` loads features at startup for zero-latency lookups
3. **Internal API**: `/internal/plans` serves feature catalog to gateway and downstream services
4. **Distributed Caching**: Each service maintains local `PlanCatalogCache` with 10-minute refresh
5. **Enforcement**: Gateway enforces boolean features; services enforce quotas at write operations

### Feature Types

- **`prioritySupport`** (boolean): Access to priority support channels
- **`maxUsers`** (integer): Maximum users per tenant (0 = unlimited)
- **`maxProjects`** (integer): Maximum projects per tenant (0 = unlimited)

### Integration Points

- **`GET /entitlements/me`**: Client applications get current plan features
- **`GET /internal/plans`**: Gateway and services refresh their feature cache
- **JWT `plan_code` claim**: Propagated by gateway as `X-Plan-Code` header

---

### Billing Settings

| Method  | Path                           | Auth               | Description                               |
| ------- | ------------------------------ | ------------------ | ----------------------------------------- |
| `GET`   | `/settings/{tenantKey}`        | JWT `TENANT_OWNER` | Get billing settings for a tenant         |
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

| Method   | Path                         | Auth                 | Description                                |
| -------- | ---------------------------- | -------------------- | ------------------------------------------ |
| `GET`    | `/admin/subscriptions`       | JWT `PLATFORM_ADMIN` | List subscriptions (paginated, filterable) |
| `GET`    | `/admin/subscriptions/count` | JWT `PLATFORM_ADMIN` | Count all subscriptions                    |
| `GET`    | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Get subscription by ID                     |
| `PATCH`  | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Partially update subscription              |
| `DELETE` | `/admin/subscriptions/{id}`  | JWT `PLATFORM_ADMIN` | Delete subscription                        |

`DELETE` permanently removes the subscription record.

---

### Refunds (platform admin)

| Method | Path                  | Auth                 | Description                            |
| ------ | --------------------- | -------------------- | -------------------------------------- |
| `GET`  | `/admin/refunds`      | JWT `PLATFORM_ADMIN` | List all refunds (paginated, filtered) |
| `GET`  | `/admin/refunds/{id}` | JWT `PLATFORM_ADMIN` | Get refund by ID                       |

`GET /admin/refunds` supports pagination and filtering by `tenantKey`.

---

### Plan Catalog

| Method | Path                | Auth                    | Description           |
| ------ | ------------------- | ----------------------- | --------------------- |
| `GET`  | `/plans`            | JWT (any authenticated) | List all active plans |
| `GET`  | `/plans/{planCode}` | JWT (any authenticated) | Get plan by planCode  |

### Plan Catalog (platform admin)

| Method   | Path                      | Auth                 | Description                                     |
| -------- | ------------------------- | -------------------- | ----------------------------------------------- |
| `GET`    | `/admin/plans`            | JWT `PLATFORM_ADMIN` | List all plans (including inactive)             |
| `GET`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Get plan by planCode                            |
| `POST`   | `/admin/plans`            | JWT `PLATFORM_ADMIN` | Create a plan                                   |
| `PUT`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Replace a plan (full update)                    |
| `PATCH`  | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Partially update a plan                         |
| `DELETE` | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Deactivate a plan (soft-delete, `active=false`) |

`DELETE` performs a soft-delete by setting `active = false`. The row is retained for historical reference.

---

### Entitlements

| Method | Path               | Auth                           | Description                                                              |
| ------ | ------------------ | ------------------------------ | ------------------------------------------------------------------------ |
| `GET`  | `/entitlements/me` | JWT `TENANT_OWNER` or `MEMBER` | Active plan, subscription status, and typed features for current subject |

**Entitlement evaluation is tightly coupled with plan features** — this endpoint serves as the primary integration point between billing and platform access control. The response includes the complete `PlanFeatures` record for the tenant's active plan, enabling fine-grained access control decisions in client applications.

**Response Example:**

```json
{
    "planCode": "pro-monthly",
    "status": "active",
    "currentPeriodEnd": "2026-07-15T00:00:00Z",
    "features": {
        "prioritySupport": true,
        "maxUsers": 50,
        "maxProjects": 0
    }
}
```

**Feature Types:**

- **`prioritySupport`** (boolean): Access to priority support channels
- **`maxUsers`** (integer): Maximum users allowed (0 = unlimited)
- **`maxProjects`** (integer): Maximum projects allowed (0 = unlimited)

Returns `404` when no active subscription exists. Resolves subject by rollout mode (tenant in multi-tenant, user in single-tenant).

---

### Plan Catalog (internal service-to-service)

| Method | Path              | Auth | Description                                              |
| ------ | ----------------- | ---- | -------------------------------------------------------- |
| `GET`  | `/internal/plans` | None | Full plan feature catalog for gateway and service caches |

**Public within internal network** — no authentication required since the response contains only non-sensitive plan feature data (same as any public pricing page). Used by the gateway and downstream services to populate their local `PlanCatalogCache` at startup and on periodic refresh. Backed by in-memory `PlanFeatureRegistry` — no DB reads.

**Response Example:**

```json
[
    {
        "planCode": "basic-monthly",
        "features": {
            "prioritySupport": false,
            "maxUsers": 5,
            "maxProjects": 3
        }
    },
    {
        "planCode": "pro-monthly",
        "features": {
            "prioritySupport": true,
            "maxUsers": 50,
            "maxProjects": 0
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

| Method | Path               | Auth             | Description                  |
| ------ | ------------------ | ---------------- | ---------------------------- |
| `POST` | `/webhooks/stripe` | Stripe signature | Ingest Stripe webhook events |

No JWT required. Secured by Stripe signature verification (`Stripe-Signature` header).
Always returns `200 OK` after the idempotency check to prevent Stripe retries on business logic failures.

---

### Interactive Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running locally.
