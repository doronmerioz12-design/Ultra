-- =============================================================================
-- ULTRA (Bletchley Park Intelligence System) - PostgreSQL Initialization Script
-- =============================================================================
-- Replace 'change_me' with strong, unique passwords matching your environment
-- variables before executing in production.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PART 1: Server-Level Setup (Run connected as 'postgres' superuser)
-- -----------------------------------------------------------------------------

-- 1. Create the application database
CREATE DATABASE my_database;

-- 2. Create the core ULTRA application user (Radio Simulator & Repository)
CREATE USER "ULTRA" WITH PASSWORD 'change_me';

-- 3. Create the 5 military branch RBAC users
CREATE USER "Navy" WITH PASSWORD 'change_me';
CREATE USER "RAF" WITH PASSWORD 'change_me';
CREATE USER "Army" WITH PASSWORD 'change_me';
CREATE USER "PMOFFICE" WITH PASSWORD 'change_me';
CREATE USER "SHAEF" WITH PASSWORD 'change_me';

-- 4. Grant database-level privileges
GRANT ALL PRIVILEGES ON DATABASE my_database TO "ULTRA";
GRANT CONNECT ON DATABASE my_database TO "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";


-- -----------------------------------------------------------------------------
-- PART 2: Database-Level Setup (Run connected INSIDE 'my_database')
-- In psql, run: \c my_database
-- -----------------------------------------------------------------------------

-- 5. Grant schema usage and table creation rights on the 'public' schema
-- (Required on PostgreSQL 15+ where public CREATE is revoked by default,
-- allowing each service/branch to run CREATE TABLE IF NOT EXISTS for its table)
GRANT USAGE, CREATE ON SCHEMA public TO "ULTRA", "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";

-- 6. Grant table and sequence privileges on all current tables/sequences in 'public'
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "ULTRA";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "ULTRA";

GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";

-- 7. Configure default privileges for future tables and sequences created in 'public'
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES ON TABLES TO "ULTRA";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES ON SEQUENCES TO "ULTRA";

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "Navy", "RAF", "Army", "PMOFFICE", "SHAEF";
