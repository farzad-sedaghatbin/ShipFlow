-- V31: Update dashboard widget types to match frontend widget components
-- Replace old dashboard section widget types with actual widget component types

-- Delete old widget types
DELETE FROM dashboard_widgets WHERE widget_type IN ('STATS_CARDS', 'QUICK_LINKS', 'ACTIVE_CYCLES', 'RECENT_PITCHES', 'HILL_CHART', 'RISK_OVERVIEW');

-- The DashboardWidgetService will automatically create new default widgets on next access
