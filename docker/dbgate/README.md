# DbGate Configuration - Billing Service

This directory contains the DbGate database administration tool configuration for the Billing Service local development environment.

## Pre-configured Connections

The `connections.jsonl` file contains pre-configured connections for all Billing service infrastructure:

### 1. PostgreSQL - Billing Service

- **ID**: `postgres-billing`
- **Server**: `postgres-billing:5432`
- **Database**: `billing`
- **User**: `svc_billing_dba`
- **Engine**: `postgres@dbgate-plugin-postgres`

### 2. PostgreSQL - IAM Service (Local)

- **ID**: `postgres-iam`
- **Server**: `postgres-billing:5432`
- **Database**: `iam`
- **User**: `svc_iam_dba`
- **Engine**: `postgres@dbgate-plugin-postgres`

**Note**: The IAM Service shares the same PostgreSQL instance in local development for convenience.

### 3. RabbitMQ - Billing Service

- **ID**: `rabbitmq-management`
- **Server**: `rabbitmq-billing:15672`
- **User**: `svc_billing_rmq`
- **Engine**: `rabbitmq@dbgate-plugin-rabbitmq`

## Usage

### Start Infrastructure with DbGate

```bash
# Start infrastructure only (for IDE development)
docker compose up -d

# Access DbGate at: http://localhost:3100
```

**Note**: DbGate runs on port **3100** (not 3000) to avoid conflict with Grafana.

### Start Full Stack with DbGate

```bash
# Start infrastructure + Billing service + IAM service
docker compose -f compose.container.yaml up -d

# Access DbGate at: http://localhost:3100
```

### Access DbGate UI

Once the services are running, open your browser to:

**http://localhost:3100**

All connections are pre-configured and ready to use.

## What You Can Do with DbGate

### PostgreSQL Database - Billing Data

The billing service uses PostgreSQL to store:

- Subscription records (Stripe-synced status, plan details)
- Plan catalog (pricing, features, billing periods)
- Billing settings per tenant (Stripe customer ID, billing email, company info)
- Webhook logs (idempotency tracking for Stripe events)
- Payment and invoice records

With DbGate you can:

- Browse subscription tables and billing data
- Query plan catalog and pricing
- Check webhook processing status
- Export billing data for analysis
- Monitor Stripe synchronization
- Debug subscription states

### PostgreSQL Database - IAM Data (Local)

The local IAM service database contains:

- User accounts and authentication data
- Tenant records
- JWT token management
- Invitation data

With DbGate you can:

- View users and their tenant associations
- Check tenant provisioning status
- Debug authentication issues
- Browse invitation records

### RabbitMQ - Event Integration

The billing service interacts with RabbitMQ for:

- **Consuming**: `tenant.created` events to bootstrap Stripe customers
- **Publishing**: `subscription.*` and `invoice.*` events
- **Publishing**: Email notification events for billing updates

With DbGate you can:

- View queues and message counts
- Monitor exchange bindings
- Check connection statistics
- Verify event flow between services

## Billing Service Specifics

The Billing Service integrates with **Stripe** for payment processing:

- Creates Stripe customers on `tenant.created` events
- Syncs subscription status with Stripe webhooks
- Processes invoice events from Stripe
- Publishes billing events for other services

### Typical Queries

Browse active subscriptions:

```sql
SELECT * FROM subscriptions
WHERE status = 'active'
ORDER BY created_at DESC;
```

Find subscriptions by tenant:

```sql
SELECT * FROM subscriptions
WHERE tenant_id = 'tenant-uuid'
ORDER BY created_at DESC;
```

Check plan catalog:

```sql
SELECT * FROM plans
ORDER BY billing_period, price_minor_units;
```

View webhook processing logs:

```sql
SELECT * FROM webhook_logs
WHERE status = 'PROCESSED'
ORDER BY created_at DESC
LIMIT 100;
```

Find failed webhooks:

```sql
SELECT * FROM webhook_logs
WHERE status = 'FAILED'
ORDER BY created_at DESC;
```

Check billing settings:

```sql
SELECT * FROM billing_settings
WHERE tenant_id = 'tenant-uuid';
```

## Updating Credentials

If you change credentials in your `.env` or compose files, update `connections.jsonl`:

```json
{
    "_id": "postgres-billing",
    "engine": "postgres@dbgate-plugin-postgres",
    "server": "postgres-billing",
    "port": 5432,
    "user": "NEW_USER",
    "password": "NEW_PASSWORD",
    "database": "billing",
    "displayName": "PostgreSQL - Billing Service"
}
```

Then restart DbGate:

```bash
docker compose restart dbgate
```

## Troubleshooting

### DbGate won't start

```bash
# Check logs
docker logs foundation-billing-dbgate-dev

# Verify connections file exists
ls docker/dbgate/connections.jsonl

# Restart DbGate
docker compose restart dbgate
```

### Can't connect to PostgreSQL

```bash
# Verify PostgreSQL is running and healthy
docker ps --filter name=foundation-billing-postgres-dev

# Check PostgreSQL logs
docker logs foundation-billing-postgres-dev

# Test connection from DbGate container
docker exec foundation-billing-dbgate-dev ping postgres-billing
```

### Can't access IAM database

The IAM database (`iam`) is on the same PostgreSQL instance but requires IAM credentials:

- User: `svc_iam_dba`
- Password: `svc_iam_dba`
- Database: `iam`

### Can't connect to RabbitMQ

```bash
# Verify RabbitMQ is running
docker ps --filter name=foundation-billing-rabbitmq-dev

# Check RabbitMQ logs
docker logs foundation-billing-rabbitmq-dev

# Ensure management plugin is enabled
docker exec foundation-billing-rabbitmq-dev rabbitmq-plugins list
```

### Reset DbGate Data

```bash
# Stop and remove DbGate
docker compose stop dbgate
docker compose rm -f dbgate

# Remove volume
docker volume rm iqkv_billing_dbgate_data_dev

# Restart
docker compose up -d dbgate
```

## Port Configuration

DbGate runs on port **3100** to avoid conflict with Grafana (port 3000):

```yaml
ports:
    - "3100:3000" # DbGate Web UI
```

## Security Notes

⚠️ **Important**: This configuration is for local development only.

- Credentials are stored in plaintext in `connections.jsonl`
- Do not commit real Stripe API keys or production credentials
- DbGate port 3100 is exposed to localhost only
- Authentication is enabled (`LOGINS=1`)
- Billing data may contain sensitive financial information

## Compose File Configuration

DbGate is included in:

- ✅ `compose.yaml` - Infrastructure only (use with IDE)
- ✅ `compose.container.yaml` - Full stack (infrastructure + services)
- ✅ `compose.base.yaml` - Base definitions

All three configurations include DbGate by default.

## Stripe Integration Notes

When working with Stripe webhooks locally:

1. Use Stripe CLI to forward webhooks: `stripe listen --forward-to localhost:8080/api/v1/billing/webhooks/stripe`
2. Check webhook logs in DbGate to verify processing
3. Monitor `webhook_logs` table for idempotency and status

## More Information

- [DbGate Official Documentation](https://dbgate.org/docs/)
- [DbGate GitHub Repository](https://github.com/dbgate/dbgate)
- [Supported Database Engines](https://dbgate.org/docs/databases.html)

## Tips for Billing Service

1. **Webhook Debugging**: Check `webhook_logs` table when Stripe events aren't processing
2. **Subscription Status**: Query subscriptions by `status` to monitor active, past_due, or cancelled states
3. **Plan Management**: Use DbGate to quickly view and compare plan pricing
4. **Stripe Customer Sync**: Verify `billing_settings.stripe_customer_id` matches Stripe dashboard
5. **Event Tracing**: Cross-reference RabbitMQ queues with database records
6. **IAM Integration**: Use IAM database connection to verify tenant/user relationships
