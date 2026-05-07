-- V61__fix_dashboard_templates.sql
-- DEPRECATED: Dashboard templates are now created by DashboardTemplateInitializer.java
-- This migration is kept as a no-op for Flyway compatibility with existing databases
-- that already executed this migration.

-- The Java initializer (DashboardTemplateInitializer) runs after DefaultAdminInitializer
-- and creates templates only if they don't exist, solving the chicken-and-egg problem
-- where migrations run before any users exist.

-- No SQL operations needed - templates are managed by application code.