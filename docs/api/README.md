## 📜 API Documentation

Base path: `/api/v1/billing`

All endpoints require a valid RS256 JWT issued by the IAM service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

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

---

### Plan Catalog (platform admin)

| Method   | Path                      | Auth                 | Description                         |
| -------- | ------------------------- | -------------------- | ----------------------------------- |
| `GET`    | `/admin/plans`            | JWT `PLATFORM_ADMIN` | List all plans (including inactive) |
| `GET`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Get plan by planCode                |
| `POST`   | `/admin/plans`            | JWT `PLATFORM_ADMIN` | Create a new plan                   |
| `PUT`    | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Replace a plan (full update)        |
| `PATCH`  | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Partially update a plan             |
| `DELETE` | `/admin/plans/{planCode}` | JWT `PLATFORM_ADMIN` | Deactivate a plan (soft-delete)     |

`DELETE` performs a soft-delete by setting `active = false`. The row is retained for historical reference.

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
