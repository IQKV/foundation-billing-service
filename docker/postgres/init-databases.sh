#!/bin/bash
set -e

# Create IAM user and database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER svc_iam_dba WITH PASSWORD 'svc_iam_dba';
    CREATE DATABASE iamservice;
    GRANT ALL PRIVILEGES ON DATABASE iamservice TO svc_iam_dba;
EOSQL

# Run Billing Service initialization
echo "Running initialization for 'billingservice' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "billingservice" -f /docker-entrypoint-initdb.d/init-billing.sql

# Run IAM Service initialization
echo "Running initialization for 'iamservice' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iamservice" -f /docker-entrypoint-initdb.d/init-iam.sql
