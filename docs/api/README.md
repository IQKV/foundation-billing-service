## 📜 API Documentation

Base path: `/api/v1/billing`

All endpoints require a valid RS256 JWT issued by the IAM service unless marked as public.
The JWT must be passed as a `Bearer` token in the `Authorization` header.

---

### Billing Settings

| Method  | Path                    | Auth               | Description                               |
| ------- | ----------------------- | ------------------ | ----------------------------------------- |
| `GET`   | `/settings/{tenantKey}` | JWT `TENANT_OWNER` | Get billing settings for a tenant         |
| `PATCH` | `/settings/{tenantKey}` | JWT `TENANT_OWNER` | Update billing settings (syncs to Stripe) |

The `tenantKey` path variable is validated against the authenticated tenant's JWT `tenant_id` claim.
Cross-tenant access returns `403 Forbidden`.

---

### Subscriptions

| Method | Path                                | Auth                           | Description                                 |
| ------ | ----------------------------------- | ------------------------------ | ------------------------------------------- |
| `GET`  | `/subscriptions/{tenantKey}/active` | JWT `TENANT_OWNER`             | Get active subscription for a tenant        |
| `GET`  | `/subscriptions/{tenantKey}`        | JWT `TENANT_OWNER`             | Get all subscriptions for a tenant          |
| `GET`  | `/subscriptions/me/active`          | JWT `TENANT_OWNER` or `MEMBER` | Get active subscription for current subject |
| `GET`  | `/subscriptions/me`                 | JWT `TENANT_OWNER` or `MEMBER` | Get all subscriptions for current subject   |

Subscription data is a local cache of Stripe state — no payment gateway round-trips are made.

---

### Plan Catalog

| Method   | Path                | Auth                    | Description                     |
| -------- | ------------------- | ----------------------- | ------------------------------- |
| `GET`    | `/plans`            | JWT (any authenticated) | List all active plans           |
| `GET`    | `/plans/{planCode}` | JWT (any authenticated) | Get plan by planCode            |
| `POST`   | `/plans`            | JWT `PLATFORM_ADMIN`    | Create a new plan               |
| `PUT`    | `/plans/{planCode}` | JWT `PLATFORM_ADMIN`    | Replace a plan (full update)    |
| `DELETE` | `/plans/{planCode}` | JWT `PLATFORM_ADMIN`    | Deactivate a plan (soft-delete) |

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
