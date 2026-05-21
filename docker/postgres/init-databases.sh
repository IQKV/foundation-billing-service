#!/bin/bash
set -e

# Create IAM user and database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER svc_iam_dba WITH PASSWORD 'svc_iam_dba';
    CREATE DATABASE iam;
    GRANT ALL PRIVILEGES ON DATABASE iam TO svc_iam_dba;
EOSQL

# Run Billing Service initialization
echo "Running initialization for 'billing' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "billing" -f /docker-entrypoint-initdb.d/init-billing.sql

# Run IAM Service initialization
echo "Running initialization for 'iam' database..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iam" -f /docker-entrypoint-initdb.d/init-iam.sql
