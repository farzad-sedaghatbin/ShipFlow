-- ============================================================================
-- V1009: Refresh demo seed data — 2026 dataset
--
-- Replaces the original V1000–V1008 demo data with realistic 2026 content:
--   • Mobile Banking App (Shape Up)  — sara / ali / mina
--   • DevOps Platform (Kanban)       — ali / sara
--
-- Demo credentials:
--   admin   / admin123  (ADMIN)
--   sara    / demo123   (MANAGER)
--   ali     / demo123   (MEMBER)
--   mina    / demo123   (MEMBER)
--   viewer  / demo123   (READONLY)
--
-- IMPORTANT: passwords use BCrypt $2b$ format which Spring Security accepts.
-- ============================================================================

-- ── STEP 1 — Wipe old seed data (targeted deletes preserve system config) ────
-- Delete in reverse FK dependency order so constraints are not violated.
-- Template dashboards from V36/V38 and org settings from V41 are preserved
-- because we only delete rows referencing the old seed person/user IDs (1–8).

DELETE FROM retro_item_votes;
DELETE FROM retro_items;
DELETE FROM retrospectives;
DELETE FROM comment_reactions;
DELETE FROM comments;
DELETE FROM hill_chart_history;
DELETE FROM task_dependencies;
DELETE FROM work_logs;
DELETE FROM meetings;
DELETE FROM bug_reports;
DELETE FROM test_runs;
DELETE FROM test_cases;
DELETE FROM betting_decisions;
DELETE FROM cooldown_activities;
DELETE FROM release_cycles;
DELETE FROM releases;
DELETE FROM epics;
DELETE FROM initiatives;
DELETE FROM betting_slots;
DELETE FROM hill_chart_points;
DELETE FROM pitches;
DELETE FROM tasks;
DELETE FROM team_assignments;
-- Remove only non-template user dashboards for old seed users
DELETE FROM dashboard_widget_configs WHERE dashboard_id IN (
    SELECT id FROM custom_dashboards WHERE is_template = false
      AND user_id IN (SELECT id FROM users)
);
DELETE FROM custom_dashboards WHERE is_template = false;
DELETE FROM user_preferences;
DELETE FROM user_projects;
DELETE FROM teams;
DELETE FROM cycles;
DELETE FROM projects;
DELETE FROM users;
DELETE FROM persons;

-- ── STEP 2 — PROJECTS ────────────────────────────────────────────────────────
INSERT INTO projects (id, name, project_key, description, color, project_type, is_active, enable_retrospectives, owner_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Mobile Banking App', 'MBA',
    'Consumer-facing mobile banking — payments, biometric auth, spending analytics, and card management.',
    '#3B82F6', 'SHAPE_UP', true, true,  NULL, '2026-01-05 09:00:00', '2026-01-05 09:00:00'),
(2, 'DevOps Platform',   'DVP',
    'Internal platform engineering — Kubernetes, CI/CD pipelines, observability, and infrastructure automation.',
    '#10B981', 'KANBAN',   true, false, NULL, '2026-01-10 09:00:00', '2026-01-10 09:00:00');

-- ── STEP 3 — PERSONS ─────────────────────────────────────────────────────────
INSERT INTO persons (id, name, email, skills, avatar_url, department, bio, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'System Administrator', 'admin@shipflow.local',  'Administration, System Management',          NULL, 'Leadership',  'System administrator',             true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Sara Hosseini',        'sara@shipflow.dev',     'Java, Spring Boot, Architecture, Leadership', NULL, 'Engineering', 'Tech lead — Mobile Banking team',  true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Ali Rezaei',           'ali@shipflow.dev',      'Java, Spring Boot, PostgreSQL, Redis',        NULL, 'Engineering', 'Backend engineer',                 true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Mina Ahmadi',          'mina@shipflow.dev',     'React, TypeScript, Tailwind CSS, Vite',       NULL, 'Engineering', 'Frontend engineer',                true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'View Only',            'viewer@shipflow.dev',   'Product Management',                          NULL, 'Product',     'Read-only stakeholder account',    true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ── STEP 4 — USERS ───────────────────────────────────────────────────────────
-- admin123 → $2b$10$Rb5z8752rv84J7kSNaOiJ.AAtIhMH6zfuWdMCpGqrM1GJQA2DxrES
-- demo123  → $2b$10$.WmsvBDuwf1fV1cMsk1/n.89QdpWXy.II6SwDJZinvQP/NTnhlyou
INSERT INTO users (id, username, password, role, person_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'admin',  '$2b$10$Rb5z8752rv84J7kSNaOiJ.AAtIhMH6zfuWdMCpGqrM1GJQA2DxrES', 'ADMIN',    1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'sara',   '$2b$10$.WmsvBDuwf1fV1cMsk1/n.89QdpWXy.II6SwDJZinvQP/NTnhlyou',  'MANAGER',  2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'ali',    '$2b$10$.WmsvBDuwf1fV1cMsk1/n.89QdpWXy.II6SwDJZinvQP/NTnhlyou',  'MEMBER',   3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'mina',   '$2b$10$.WmsvBDuwf1fV1cMsk1/n.89QdpWXy.II6SwDJZinvQP/NTnhlyou',  'MEMBER',   4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'viewer', '$2b$10$.WmsvBDuwf1fV1cMsk1/n.89QdpWXy.II6SwDJZinvQP/NTnhlyou',  'READONLY', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Set project owners
UPDATE projects SET owner_id = 2 WHERE id = 1;
UPDATE projects SET owner_id = 3 WHERE id = 2;

-- ── STEP 5 — CYCLES ──────────────────────────────────────────────────────────
INSERT INTO cycles (id, name, start_date, end_date, phase, project_id, is_active) OVERRIDING SYSTEM VALUE VALUES
(1, 'v2.0 — Payments Overhaul',        '2026-04-01', '2026-05-15', 'SHAPING_BUILDING', 1, true),
(2, 'v1.5 — Biometric Authentication', '2026-02-01', '2026-03-14', 'BETTING_COOLDOWN', 1, false),
(3, 'Continuous Flow',                  '2026-01-10', '2099-12-31', 'SHAPING_BUILDING', 2, true);

-- ── STEP 6 — TEAMS ───────────────────────────────────────────────────────────
INSERT INTO teams (id, name) OVERRIDING SYSTEM VALUE VALUES
(1, 'Payments Team'),
(2, 'Authentication Team');

-- ── STEP 7 — TEAM ASSIGNMENTS ────────────────────────────────────────────────
INSERT INTO team_assignments (person_id, team_id, role, start_date, is_active) VALUES
(3, 1, 'BACKEND',   '2026-04-01', true),
(4, 1, 'FRONTEND',  '2026-04-01', true),
(2, 1, 'TECH_LEAD', '2026-04-01', true),
(3, 2, 'BACKEND',   '2026-02-01', true),
(4, 2, 'FRONTEND',  '2026-02-01', true),
(2, 2, 'TECH_LEAD', '2026-02-01', true);

-- ── STEP 8 — USER-PROJECT ASSIGNMENTS ────────────────────────────────────────
INSERT INTO user_projects (user_id, project_id, project_role, created_at)
SELECT u.id, p.id,
       CASE WHEN u.role IN ('ADMIN', 'MANAGER') THEN 'MANAGER' ELSE 'CONTRIBUTOR' END,
       CURRENT_TIMESTAMP
FROM users u CROSS JOIN projects p
WHERE u.is_active = true AND p.is_active = true
ON CONFLICT DO NOTHING;

-- ── STEP 9 — USER PREFERENCES ────────────────────────────────────────────────
INSERT INTO user_preferences (user_id, theme_mode, compact_view, enable_animations, created_at, updated_at)
SELECT id, 'SYSTEM', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE id NOT IN (SELECT user_id FROM user_preferences)
ON CONFLICT (user_id) DO NOTHING;

UPDATE user_preferences SET theme_mode = 'DARK',  compact_view = true  WHERE user_id = 2; -- sara
UPDATE user_preferences SET theme_mode = 'SYSTEM', compact_view = false WHERE user_id = 3; -- ali
UPDATE user_preferences SET theme_mode = 'LIGHT',  compact_view = false WHERE user_id = 4; -- mina

-- ── STEP 10 — PITCHES ────────────────────────────────────────────────────────
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Biometric Login',
    'Replace password-only login with Face ID / fingerprint on iOS and Android. Server issues a device-bound JWT on biometric success. PCI-DSS 8.3.1 compliant.',
    14, 2, 2, 'DONE',        '2026-01-20 10:00:00', '2026-03-14 16:00:00'),
(2, 'Session Management Overhaul',
    'Refresh-token rotation (one-time-use tokens), per-device session listing, forced logout on password change. Zero-trust session hygiene.',
    10, 2, 2, 'DONE',        '2026-01-22 11:00:00', '2026-03-14 16:00:00'),
(3, 'Instant Transfer UI',
    'End-to-end P2P transfer: IBAN entry with bank-name lookup, idempotent POST /transfers, OTP confirmation screen, success animation.',
    14, 1, 1, 'IN_PROGRESS', '2026-03-01 09:00:00', CURRENT_TIMESTAMP),
(4, 'Spending Analytics Dashboard',
    'Categorise transactions by MCC into 8 spend categories. Donut chart + 6-month bar chart. Real-time category totals on balance widget.',
    10, 1, 1, 'STARTED',     '2026-03-05 09:00:00', CURRENT_TIMESTAMP),
(5, 'Card Freeze & Virtual Cards',
    'One-tap freeze/unfreeze for physical card. Instant virtual card issuance for online-only use. Append-only freeze_events audit trail.',
    5,  1, NULL, 'SHAPED',   '2026-03-15 14:00:00', '2026-03-25 10:00:00'),
(6, 'Recurring Payment Scheduler',
    'CRON-based scheduler for standing orders: weekly, monthly, custom. Conflict detection for low-balance days. Push notification 24h before execution.',
    10, 1, NULL, 'DRAFT',    '2026-03-28 10:00:00', '2026-03-28 10:00:00'),
(7, 'Fraud Alert System',
    'ML-based anomaly detection for unusual transaction patterns. Real-time push alert with deep link. Auto-freeze after 3 consecutive anomalies.',
    14, 1, NULL, 'IDEA',     '2026-03-29 09:00:00', '2026-03-29 09:00:00');

-- ── STEP 11 — HILL CHART POINTS ──────────────────────────────────────────────
INSERT INTO hill_chart_points (id, pitch_id, scope, description, position, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Pitch 1 — Biometric Login (DONE → all 100)
(1,  1, 'iOS Biometric',       'LocalAuthentication + JWT issuance',        100, '2026-02-06', '2026-03-10'),
(2,  1, 'Android Biometric',   'BiometricPrompt + device binding',           100, '2026-02-06', '2026-03-10'),
(3,  1, 'Enrolment UI',        'Onboarding screens + success animation',     100, '2026-02-08', '2026-03-10'),
(4,  1, 'Security Sign-off',   'PCI-DSS review + pen test',                  100, '2026-03-08', '2026-03-13'),
-- Pitch 2 — Session Overhaul (DONE → all 100)
(5,  2, 'Token Rotation',      'DB schema + service for one-time refresh',   100, '2026-02-10', '2026-03-12'),
(6,  2, 'Device Session API',  'List + revoke per-device sessions',          100, '2026-02-11', '2026-03-12'),
(7,  2, 'Sessions UI',         'Active sessions management screen',          100, '2026-02-14', '2026-03-12'),
(8,  2, 'Force-logout on PWD', 'Invalidate all tokens on password change',   100, '2026-02-16', '2026-03-12'),
-- Pitch 3 — Instant Transfer (IN_PROGRESS)
(9,  3, 'Transfer API',        'POST /transfers + idempotency + PSP calls',   75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 3, 'IBAN Validation',     'MOD-97 check + bank-name lookup',             60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 3, 'OTP Screen',          'Countdown + resend + auto-submit',            50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 3, 'Success Animation',   'Confetti + receipt deep link',                20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Pitch 4 — Spending Analytics (STARTED)
(13, 4, 'MCC Category Mapping','Seed MCC-to-category table, tag transactions',45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 4, 'Aggregation Queries', 'Category totals per month — cursor paging',   30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 4, 'Donut Chart',         'Recharts donut + drill-to-category list',     15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 4, 'Bar Chart Trend',     '6-month bar chart with % delta tooltip',       5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ── STEP 12 — BETTING SLOTS ──────────────────────────────────────────────────
INSERT INTO betting_slots (id, cycle_id, team_id, position, start_date, end_date, pitch_id, notes, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 1, 0, '2026-04-01', '2026-05-15', 3, 'Primary — Payments Team: Instant Transfer',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 1, 1, '2026-04-01', '2026-05-15', 4, 'Secondary — Payments Team: Spending Analytics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, 2, 0, '2026-02-01', '2026-03-14', 1, 'Primary — Auth Team: Biometric Login',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 2, 1, '2026-02-01', '2026-03-14', 2, 'Secondary — Auth Team: Session Overhaul',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ── STEP 13 — TASKS (MBA — cycle 1, Shape Up) ────────────────────────────────
-- created_by_id → persons.id   assignee_id → persons.id
INSERT INTO tasks (id, title, description, status, priority, estimate_hours, actual_hours, cycle_id, assignee_id, created_by_id, due_date, category, tags, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1,  'Instant transfer — backend API',
     'POST /transfers with PSP integration. Idempotency key via X-Idempotency-Key header. Async webhook confirmation.',
     'IN_PROGRESS', 'HIGH',    8.0, 5.0,  1, 3, 3, '2026-04-15', 'PITCH_SCOPE',    'backend,payments,webhooks',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  'IBAN validation service',
     'ISO 13616 IBAN validation: country code, check digits (MOD97), bank code lookup via BIC registry.',
     'TODO',        'HIGH',    5.0, NULL, 1, 3, 2, '2026-04-12', 'PITCH_SCOPE',    'backend,payments,validation',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  'Transfer confirmation screen',
     'OTP input with 60s countdown. Auto-submit on last digit. Resend after cooldown. Animated success + confetti.',
     'IN_PROGRESS', 'HIGH',    6.0, 4.0,  1, 4, 4, '2026-04-14', 'PITCH_SCOPE',    'frontend,payments,ui',            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  'Balance widget redesign',
     'Gradient background, masked balance toggle, 7-day sparkline, quick action buttons.',
     'DONE',        'MEDIUM',  4.0, 3.5,  1, 4, 4, '2026-04-05', 'PITCH_SCOPE',    'frontend,ui,design',              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  'Transaction history pagination',
     'Infinite scroll. Backend: cursor-based pagination. Frontend: useInfiniteQuery + skeleton loaders.',
     'IN_PROGRESS', 'MEDIUM',  5.0, 3.0,  1, 3, 3, '2026-04-18', 'PITCH_SCOPE',    'backend,frontend,payments',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  'Spending category tagging',
     'Tag each transaction with a spend category using MCC codes. Seed MCC-to-category mapping in Flyway migration.',
     'TODO',        'MEDIUM',  6.0, NULL, 1, 3, 2, '2026-04-20', 'PITCH_SCOPE',    'backend,analytics,database',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  'Category breakdown chart',
     'Donut chart for spend category split (Recharts). Animated on mount. Tap slice to drill into category list.',
     'BACKLOG',     'MEDIUM',  5.0, NULL, 1, 4, 4, '2026-04-25', 'PITCH_SCOPE',    'frontend,charts,analytics',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  'Monthly trend bar chart',
     'Bar chart: last 6 months total spend per month. Highlight current month. % change tooltip.',
     'BACKLOG',     'LOW',     4.0, NULL, 1, 4, 4, '2026-04-28', 'PITCH_SCOPE',    'frontend,charts,analytics',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  'Push notification for large transfers',
     'FCM push when transfer > 50M IRR. Include amount, beneficiary, and deep link to transaction detail.',
     'TODO',        'MEDIUM',  3.0, NULL, 1, 3, 2, '2026-04-22', 'PITCH_SCOPE',    'backend,notifications',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Fix decimal rounding in currency display',
     'Amounts show 3 decimal places in Persian locale. Fix: maximumFractionDigits=0 for IRR.',
     'DONE',        'HIGH',    2.0, 1.5,  1, 4, 3, '2026-04-03', 'DEBT_IMPROVEMENT','frontend,bug,i18n',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'API rate limiting — /api/transfers',
     'Bucket4j: 10 transfers/min per user. Return 429 with Retry-After header. Add to RateLimitFilter.',
     'IN_REVIEW',   'HIGH',    4.0, 3.0,  1, 3, 2, '2026-04-10', 'DEBT_IMPROVEMENT','backend,security,rate-limiting', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Accessibility audit — payment flow',
     'WCAG 2.1 AA audit for the transfer flow. Fix missing ARIA labels + insufficient contrast on disabled states.',
     'BACKLOG',     'MEDIUM',  6.0, NULL, 1, 4, 2, '2026-05-05', 'DEBT_IMPROVEMENT','frontend,a11y,compliance',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'E2E tests — transfer flow',
     'Playwright: login → dashboard → initiate transfer → OTP → success → verify transaction in history.',
     'TODO',        'MEDIUM',  8.0, NULL, 1, 3, 2, '2026-05-08', 'DEBT_IMPROVEMENT','testing,e2e,payments',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'JWT refresh token rotation',
     'Each /auth/refresh issues new access + refresh token and invalidates the previous one (one-time use).',
     'DONE',        'HIGH',    5.0, 4.5,  1, 3, 2, '2026-04-02', 'PITCH_SCOPE',    'backend,security,auth',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 'Error handling for payment timeout',
     'PSP timeout >30s: stop spinner, show friendly error, offer retry. Log correlation ID for support lookup.',
     'IN_PROGRESS', 'HIGH',    3.0, 1.5,  1, 4, 3, '2026-04-16', 'PITCH_SCOPE',    'frontend,error-handling,payments',CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 'Performance test — high load transfer',
     'k6 load test: 500 concurrent users for 5 min. Target: p99 < 2s, 0% error rate.',
     'TODO',        'LOW',     6.0, NULL, 1, 3, 2, '2026-05-10', 'DEBT_IMPROVEMENT','testing,performance,backend',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE tasks SET completed_at = '2026-04-05 17:00:00' WHERE id = 4;
UPDATE tasks SET completed_at = '2026-04-03 15:00:00' WHERE id = 10;
UPDATE tasks SET completed_at = '2026-04-02 16:30:00' WHERE id = 14;

-- ── STEP 14 — TASKS (DVP — cycle 3, Kanban) ──────────────────────────────────
INSERT INTO tasks (id, title, description, status, priority, estimate_hours, actual_hours, cycle_id, assignee_id, created_by_id, due_date, category, tags, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- DONE (5)
(17, 'Set up Kubernetes cluster on AWS EKS',
     'Provision 3-node EKS with managed node groups. Configure VPC, subnets, IAM roles. Enable cluster autoscaler.',
     'DONE',        'HIGH',   12.0, 14.0, 3, 3, 3, '2026-02-15', 'PITCH_SCOPE', 'infra,kubernetes,aws',            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(18, 'Configure Helm chart for ShipFlow backend',
     'Package Spring Boot as Helm chart: Deployment, Service, Ingress, ConfigMap, Secret. Values for dev/staging/prod.',
     'DONE',        'HIGH',    6.0,  5.5, 3, 3, 3, '2026-02-20', 'PITCH_SCOPE', 'infra,helm,kubernetes',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(19, 'CI/CD pipeline for frontend (GitHub Actions)',
     'On push to main: npm test → build → Docker → push GHCR → kubectl rollout. Cache node_modules.',
     'DONE',        'MEDIUM',  5.0,  4.5, 3, 3, 3, '2026-02-25', 'PITCH_SCOPE', 'ci-cd,github-actions,frontend',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 'Network policies for pod isolation',
     'Apply Kubernetes NetworkPolicy: deny all ingress by default, allow explicit pod-to-pod paths only.',
     'DONE',        'HIGH',    4.0,  3.5, 3, 3, 3, '2026-03-05', 'PITCH_SCOPE', 'security,kubernetes,networking',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 'On-call rotation setup (PagerDuty)',
     'Configure PagerDuty escalation: L1 → L2 → manager. Alert routing from Alertmanager. 24/7 on-call schedule.',
     'DONE',        'MEDIUM',  3.0,  3.0, 3, 2, 2, '2026-03-10', 'PITCH_SCOPE', 'ops,alerting,oncall',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- IN_PROGRESS (4)
(22, 'Prometheus + Alertmanager setup',
     'Deploy kube-prometheus-stack via Helm. Alerts: pod restarts > 3, CPU > 80%, memory > 90%, disk > 85%.',
     'IN_PROGRESS', 'HIGH',    8.0,  5.0, 3, 3, 3, '2026-04-20', 'PITCH_SCOPE', 'monitoring,prometheus,alerting',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 'Grafana dashboard templates',
     'Standard dashboards: JVM metrics, HTTP rates, error rates, PostgreSQL connections, Redis hit rate.',
     'IN_PROGRESS', 'MEDIUM',  6.0,  3.0, 3, 4, 3, '2026-04-25', 'PITCH_SCOPE', 'monitoring,grafana,dashboards',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(24, 'Terraform modules for AWS RDS',
     'Reusable module for PostgreSQL RDS: multi-AZ, encrypted at rest, S3 backups, parameter group tuning.',
     'IN_PROGRESS', 'HIGH',   10.0,  6.0, 3, 3, 3, '2026-05-01', 'PITCH_SCOPE', 'infra,terraform,aws',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 'SLA monitoring dashboard',
     'Availability %, P50/P95/P99 latency, error rate. 30-day rolling window. Weekly PDF report to stakeholders.',
     'IN_PROGRESS', 'MEDIUM',  7.0,  4.0, 3, 4, 2, '2026-04-30', 'PITCH_SCOPE', 'monitoring,sla,reporting',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- BLOCKED (1)
(26, 'SSL certificate renewal automation',
     'cert-manager + Let''s Encrypt DNS validation. Blocked: awaiting security team approval for DNS API key access.',
     'BLOCKED',     'HIGH',    4.0, NULL, 3, 3, 3, '2026-04-18', 'PITCH_SCOPE', 'security,tls,cert-manager',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- IN_REVIEW (3)
(27, 'Log aggregation with Grafana Loki',
     'Promtail DaemonSet → Loki. 30-day retention. LogQL queries for error spikes and slow query detection.',
     'IN_REVIEW',   'MEDIUM',  6.0,  5.5, 3, 3, 3, '2026-04-22', 'PITCH_SCOPE', 'logging,loki,observability',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 'Redis cluster setup (6 nodes)',
     'Redis 7 cluster: 3 primaries + 3 replicas. maxmemory-policy allkeys-lru. Sentinel failover. TLS between nodes.',
     'IN_REVIEW',   'HIGH',    8.0,  7.0, 3, 3, 3, '2026-04-19', 'PITCH_SCOPE', 'infra,redis,caching',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 'Documentation site deployment (VitePress)',
     'VitePress docs → GitHub Pages via Actions. Custom domain docs.shipflow.dev. Include API reference.',
     'IN_REVIEW',   'LOW',     4.0,  3.5, 3, 4, 2, '2026-04-21', 'PITCH_SCOPE', 'docs,vitepress,ci-cd',            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- TODO (4)
(30, 'ArgoCD GitOps deployment',
     'Install ArgoCD. Application CRs pointing to Helm charts in Git. Automated sync with self-heal. RBAC for devs/ops.',
     'TODO',        'HIGH',    8.0, NULL, 3, 3, 3, '2026-05-10', 'PITCH_SCOPE', 'ci-cd,argocd,gitops',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(31, 'HashiCorp Vault for secrets',
     'Vault in HA mode. Migrate K8s secrets to Vault KV v2. Vault Agent Injector for pod secret injection.',
     'TODO',        'HIGH',   10.0, NULL, 3, 3, 3, '2026-05-15', 'PITCH_SCOPE', 'security,vault,secrets',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(32, 'PostgreSQL automated backup to S3',
     'pg_dump nightly cron in K8s. AES-256 encryption before S3 upload. Alert on failure. Monthly restore drill.',
     'TODO',        'HIGH',    5.0, NULL, 3, 3, 3, '2026-05-08', 'PITCH_SCOPE', 'database,backup,aws',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(33, 'Container image security scanning (Trivy)',
     'Trivy scan in CI. Fail build on CRITICAL CVEs. Weekly scan of GHCR images. Report to Slack.',
     'TODO',        'MEDIUM',  4.0, NULL, 3, 3, 3, '2026-05-12', 'PITCH_SCOPE', 'security,ci-cd,containers',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- BACKLOG (3)
(34, 'ELK stack migration from CloudWatch',
     'Migrate app logs from CloudWatch to self-hosted ELK. Estimate 3× cost reduction.',
     'BACKLOG',     'MEDIUM', 15.0, NULL, 3, 3, 3, '2026-06-01', 'DEBT_IMPROVEMENT','logging,elk,infra',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(35, 'Service mesh with Istio',
     'Istio for mTLS, traffic mirroring, canary deployments, and circuit breakers via VirtualService.',
     'BACKLOG',     'LOW',    20.0, NULL, 3, 3, 3, '2026-06-15', 'DEBT_IMPROVEMENT','infra,istio,service-mesh',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(36, 'Multi-region failover testing',
     'Simulate full region failure: DNS failover to eu-west-1, RDS read replica promotion. Measure actual RTO.',
     'BACKLOG',     'LOW',    12.0, NULL, 3, 3, 3, '2026-06-20', 'DEBT_IMPROVEMENT','infra,dr,aws',                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- CANCELLED (1)
(37, 'Migrate to AWS ECS (cancelled)',
     'Evaluation of ECS as K8s alternative. Cancelled: team voted to stay on EKS for ecosystem and tooling.',
     'CANCELLED',   'LOW',    20.0, NULL, 3, 3, 3, '2026-03-01', 'PITCH_SCOPE', 'infra,ecs,cancelled',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE tasks SET completed_at = '2026-02-15 18:00:00' WHERE id = 17;
UPDATE tasks SET completed_at = '2026-02-20 17:30:00' WHERE id = 18;
UPDATE tasks SET completed_at = '2026-02-25 16:00:00' WHERE id = 19;
UPDATE tasks SET completed_at = '2026-03-05 15:30:00' WHERE id = 20;
UPDATE tasks SET completed_at = '2026-03-10 14:00:00' WHERE id = 21;

-- Task dependencies
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at) VALUES
(2,  1,  'DEPENDS_ON', CURRENT_TIMESTAMP),
(3,  1,  'DEPENDS_ON', CURRENT_TIMESTAMP),
(30, 22, 'DEPENDS_ON', CURRENT_TIMESTAMP),
(31, 26, 'DEPENDS_ON', CURRENT_TIMESTAMP);

-- ── STEP 15 — BUG REPORTS ────────────────────────────────────────────────────
-- reporter_id → users.id    assignee_id → persons.id
INSERT INTO bug_reports (id, bug_key, title, description, steps_to_reproduce, expected_behavior, actual_behavior, environment, severity, status, project_id, pitch_id, cycle_id, team_id, reporter_id, assignee_id, created_at, updated_at, tags) OVERRIDING SYSTEM VALUE VALUES
(1, 'MBA-BUG-001',
    'Double submission on network timeout during transfer',
    'When network drops between POST /transfers and PSP response, the user retries and the transfer executes twice. Balance deducted twice.',
    E'1. Initiate transfer\n2. Kill network after request sent, before 200 received\n3. App shows spinner timeout\n4. User taps Retry\n5. Transfer processed twice',
    'Idempotency key prevents duplicate processing.',
    'PSP processes second request independently. Balance deducted twice.',
    'iOS 17.4 / Production PSP sandbox',
    'CRITICAL', 'IN_PROGRESS', 1, 3, 1, 1, 3, 3, '2026-04-04 11:30:00', CURRENT_TIMESTAMP, 'backend,payments,idempotency'),
(2, 'MBA-BUG-002',
    'Biometric auth fails on Samsung Galaxy S24 Ultra',
    'BiometricPrompt dialog does not appear on Samsung Galaxy S24 Ultra running One UI 6.1. App falls through to PIN fallback silently.',
    E'1. Open app on Samsung Galaxy S24 Ultra (One UI 6.1)\n2. Tap ''Login with Biometrics''\n3. Observe: PIN screen appears immediately',
    'BiometricPrompt should appear.',
    'Biometric dialog skipped silently, falls to PIN.',
    'Samsung Galaxy S24 Ultra / Android 14 / One UI 6.1',
    'MAJOR', 'OPEN', 1, 1, 2, NULL, 4, NULL, '2026-03-16 14:00:00', CURRENT_TIMESTAMP, 'android,biometric,samsung'),
(3, 'MBA-BUG-003',
    'Currency symbol missing in Persian locale transaction list',
    'In the FA locale the currency symbol (﷼) does not appear next to transaction amounts. Amount shows as a plain number.',
    E'1. Switch app language to Persian\n2. Open transaction history\n3. Observe: amounts display without ﷼ symbol',
    'Amounts formatted as ''1,500,000 ﷼'' in Persian locale.',
    'Amounts show as ''1500000'' with no currency symbol.',
    'iOS 17.4 + Android 14 / Persian locale',
    'MINOR', 'OPEN', 1, NULL, NULL, NULL, 4, NULL, '2026-04-04 09:00:00', CURRENT_TIMESTAMP, 'i18n,persian,frontend');

-- ── STEP 16 — TEST CASES ─────────────────────────────────────────────────────
-- created_by_id → users.id (TestCase entity)
INSERT INTO test_cases (id, test_case_key, title, description, preconditions, steps, expected_result, pitch_id, cycle_id, team_id, type, priority, status, tags, estimated_minutes, created_by_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'MBA-TC-001',
    'OTP expiry after 3 failed attempts',
    'Verify OTP is invalidated after 3 consecutive wrong entries and user is prompted for a new code.',
    'User is on OTP confirmation screen after initiating a transfer.',
    E'1. Enter wrong OTP three times\n2. Observe error on third attempt\n3. Verify OTP field is disabled\n4. Verify ''Send new code'' CTA appears\n5. Request new OTP and verify counter resets',
    'OTP locked after 3 failures. New OTP request re-enables input.',
    1, 2, 2, 'E2E', 'HIGH', 'APPROVED', 'otp,auth,security', 15, 4, '2026-02-08 10:00:00', CURRENT_TIMESTAMP),
(2, 'MBA-TC-002',
    'Transfer rejected on insufficient balance',
    'Verify payment flow handles insufficient balance: shows error before OTP to avoid user frustration.',
    'User has balance 10,000 IRR. Attempting to transfer 1,000,000 IRR.',
    E'1. Open transfer screen\n2. Enter beneficiary IBAN\n3. Enter amount 1,000,000 IRR\n4. Tap Continue\n5. Observe: inline error before OTP screen',
    'Error message ''Insufficient balance'' shown inline. OTP screen never shown.',
    3, 1, 1, 'FUNCTIONAL', 'CRITICAL', 'READY', 'payments,validation,ux', 10, 3, '2026-04-02 11:00:00', CURRENT_TIMESTAMP);

-- ── STEP 17 — RETROSPECTIVES ─────────────────────────────────────────────────
INSERT INTO retrospectives (id, title, notes, status, cycle_id, project_id, created_by_id, created_at, updated_at, closed_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'v1.5 Biometric Auth — Sprint Retrospective',
    'Strong delivery. Biometric auth shipped on time and passed security review. Main pain point was lack of real Samsung devices for testing.',
    'CLOSED', 2, 1, 2, '2026-03-14 14:00:00', '2026-03-14 16:00:00', '2026-03-14 16:00:00'),
(2, 'v2.0 Payments Overhaul — Mid-Cycle Check-in',
    'Mid-cycle pulse. Payments team on track. Double-submission bug needs immediate attention — idempotency key story underestimated.',
    'OPEN',   1, 1, 2, '2026-04-04 15:00:00', CURRENT_TIMESTAMP, NULL);

-- Retro items — author_id → users.id
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- v1.5 retro (closed)
(1, 'Biometric flow shipped on time and passed PCI audit',              'WENT_WELL',       1, 2, '2026-03-14 14:10:00', '2026-03-14 14:10:00'),
(2, 'Team communication via ShipFlow comments kept everyone aligned',   'WENT_WELL',       1, 4, '2026-03-14 14:15:00', '2026-03-14 14:15:00'),
(3, 'No Samsung test devices — Galaxy S24 bug found only after release','DID_NOT_GO_WELL', 1, 3, '2026-03-14 14:20:00', '2026-03-14 14:20:00'),
(4, 'Set up cloud-based device farm before shipping next cycle',        'TRY_NEXT',        1, 3, '2026-03-14 14:25:00', '2026-03-14 14:25:00'),
(5, 'Buy 2 Samsung Galaxy devices for QA lab by end of April',         'ACTIONS',         1, 2, '2026-03-14 14:30:00', '2026-03-14 14:30:00'),
-- v2.0 mid-cycle check-in (open)
(6, 'Transfer UI design is the best we''ve ever shipped',              'WENT_WELL',       2, 4, '2026-04-04 15:10:00', '2026-04-04 15:10:00'),
(7, 'PSP sandbox reliability — 3 hours lost this week',                'DID_NOT_GO_WELL', 2, 3, '2026-04-04 15:15:00', '2026-04-04 15:15:00'),
(8, 'Add idempotency keys to ALL payment endpoints, not just transfer', 'TRY_NEXT',       2, 3, '2026-04-04 15:20:00', '2026-04-04 15:20:00'),
(9, 'Use WireMock PSP stub for local dev to avoid sandbox flakiness',  'TRY_NEXT',        2, 3, '2026-04-04 15:25:00', '2026-04-04 15:25:00');

-- Retro votes
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(1, 3, CURRENT_TIMESTAMP), (1, 4, CURRENT_TIMESTAMP), (1, 2, CURRENT_TIMESTAMP), (1, 1, CURRENT_TIMESTAMP),
(2, 3, CURRENT_TIMESTAMP), (2, 4, CURRENT_TIMESTAMP), (2, 2, CURRENT_TIMESTAMP),
(3, 2, CURRENT_TIMESTAMP), (3, 4, CURRENT_TIMESTAMP), (3, 1, CURRENT_TIMESTAMP), (3, 3, CURRENT_TIMESTAMP), (3, 5, CURRENT_TIMESTAMP),
(4, 3, CURRENT_TIMESTAMP), (4, 2, CURRENT_TIMESTAMP), (4, 4, CURRENT_TIMESTAMP), (4, 1, CURRENT_TIMESTAMP),
(6, 3, CURRENT_TIMESTAMP), (6, 2, CURRENT_TIMESTAMP), (6, 1, CURRENT_TIMESTAMP), (6, 4, CURRENT_TIMESTAMP), (6, 5, CURRENT_TIMESTAMP),
(7, 2, CURRENT_TIMESTAMP), (7, 4, CURRENT_TIMESTAMP), (7, 1, CURRENT_TIMESTAMP), (7, 3, CURRENT_TIMESTAMP),
(8, 3, CURRENT_TIMESTAMP), (8, 2, CURRENT_TIMESTAMP), (8, 4, CURRENT_TIMESTAMP), (8, 1, CURRENT_TIMESTAMP), (8, 5, CURRENT_TIMESTAMP);

-- ── STEP 18 — WORK LOGS ──────────────────────────────────────────────────────
INSERT INTO work_logs (id, person_id, pitch_id, date, hours_spent, note) OVERRIDING SYSTEM VALUE VALUES
-- Instant Transfer
(1,  3, 3, '2026-04-01', 7.5, 'Set up payment API integration, drafted webhook endpoint'),
(2,  3, 3, '2026-04-02', 6.0, 'Implemented IBAN validation and bank code lookup'),
(3,  3, 3, '2026-04-03', 7.0, 'Transfer submission flow + error handling'),
(4,  4, 3, '2026-04-02', 6.5, 'Designed beneficiary selection screen with search'),
(5,  4, 3, '2026-04-03', 7.0, 'OTP confirmation screen with animated states'),
(6,  2, 3, '2026-04-04', 3.0, 'Code review — suggested retry-on-timeout pattern'),
-- Spending Analytics
(7,  4, 4, '2026-04-01', 5.0, 'Dashboard layout prototype, category colour system'),
(8,  3, 4, '2026-04-02', 4.0, 'Transaction aggregation queries by MCC category'),
-- Biometric Login
(9,  3, 1, '2026-02-03', 8.0, 'iOS LocalAuthentication integration'),
(10, 3, 1, '2026-02-04', 7.5, 'Android BiometricPrompt integration'),
(11, 4, 1, '2026-02-05', 6.0, 'UI — enrollment screen, success animation'),
(12, 3, 1, '2026-02-06', 5.5, 'JWT issuance on biometric success, device binding'),
(13, 2, 1, '2026-03-08', 3.0, 'Final code review + sign-off'),
-- Session Overhaul
(14, 3, 2, '2026-02-10', 7.0, 'Refresh token rotation — DB schema + service layer'),
(15, 3, 2, '2026-02-11', 6.5, 'Per-device session listing and revocation API'),
(16, 4, 2, '2026-02-12', 5.5, 'Active sessions management UI');

-- ── STEP 19 — MEETINGS ───────────────────────────────────────────────────────
INSERT INTO meetings (id, pitch_id, type, date_held, dor_ready, dod_ready, notes, project_id) OVERRIDING SYSTEM VALUE VALUES
(1, 3, 'KICKOFF',     '2026-04-01', true,  false, 'Team aligned on scope. IBAN validation covers 42 countries. OTP flow prioritised.',                              1),
(2, 3, 'STANDUP',     '2026-04-04', true,  false, 'Backend 60% — webhook handler ready. Frontend 40% — OTP screen in progress. No blockers.',                       1),
(3, 5, 'SHAPING',     '2026-03-20', false, false, 'Appetite confirmed: 2 days. Chose flag approach over append-only freeze_events for simplicity — Envers audits.', 1),
(4, 5, 'BETTING',     '2026-03-25', true,  false, 'Card Freeze bet for v2.1 cooldown. Small but high-value for user trust. Sara will handle.',                       1),
(5, 1, 'DEMO',        '2026-03-13', true,  true,  'Live demo to product + security teams. iOS + Android flows shown. Security team approved for production.',        1);

-- ── STEP 20 — ROADMAP ────────────────────────────────────────────────────────
INSERT INTO initiatives (id, name, description, status, color, target_start_date, target_end_date, project_id, owner_id, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'PCI-DSS Compliance 2026',
    'Achieve PCI-DSS Level 1 compliance. Biometric auth, token rotation, fraud detection, and full audit trail.',
    'IN_PROGRESS', '#EF4444', '2026-01-01', '2026-06-30', 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Payments Excellence 2026',
    'Make payments instant, reliable, and intelligent. Instant transfers, recurring payments, spending analytics.',
    'IN_PROGRESS', '#3B82F6', '2026-02-01', '2026-09-30', 1, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Platform Reliability 2026',
    'SLA 99.9% uptime. Full observability stack, GitOps deployments, automated DR, and security hardening.',
    'IN_PROGRESS', '#10B981', '2026-01-10', '2026-12-31', 2, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO epics (id, name, description, status, target_start_date, target_end_date, project_id, initiative_id, owner_id, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Authentication Modernisation',
    'Replace password-only auth with biometric + OTP. Session hygiene overhaul.',
    'IN_PROGRESS', '2026-01-15', '2026-03-31', 1, 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Instant Payment Flow',
    'End-to-end instant transfer UI, IBAN validation, idempotency, and analytics.',
    'IN_PROGRESS', '2026-03-15', '2026-05-31', 1, 2, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE pitches SET epic_id = 1 WHERE id IN (1, 2);
UPDATE pitches SET epic_id = 2 WHERE id IN (3, 4);

INSERT INTO releases (id, name, version, description, status, project_id, release_date, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'v1.5 — Biometric Authentication', '1.5.0', 'Biometric login, session management overhaul. PCI-DSS compliant.', 'RELEASED',    1, '2026-03-14', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'v2.0 — Payments Overhaul',        '2.0.0', 'Instant transfer UI, IBAN validation, spending analytics, card freeze.',  'IN_PROGRESS', 1, '2026-05-20', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO release_cycles (release_id, cycle_id) VALUES (1, 2), (2, 1);

UPDATE pitches SET target_release_id = 1 WHERE id IN (1, 2);
UPDATE pitches SET target_release_id = 2 WHERE id IN (3, 4, 5);
UPDATE tasks   SET target_release_id = 2 WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
UPDATE bug_reports SET target_release_id = 2 WHERE id IN (1, 3);
UPDATE bug_reports SET target_release_id = 1 WHERE id = 2;

-- ── STEP 21 — COMMENTS ───────────────────────────────────────────────────────
INSERT INTO comments (id, content, entity_type, entity_id, author_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Confirmed with PSP support: idempotency key must be UUID v4, max 64 chars. Adding X-Idempotency-Key header to API spec now.',
    'BUG_REPORT', 1, 3, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(2, 'Device-binding approach approved by security team. JWT payload includes device_fingerprint + public_key hash.',
    'BUG_REPORT', 2, 2, '2026-02-20 11:00:00', '2026-02-20 11:00:00'),
(3, 'Blocked waiting for DNS API key approval from security team. Raised ticket SEC-441.',
    'TASK',  26, 3, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days');

INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at) VALUES
(1, 2, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(1, 4, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(2, 3, 'THUMBS_UP', '2026-02-20 12:00:00');

-- ── STEP 22 — HILL CHART HISTORY ─────────────────────────────────────────────
INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, user_id, previous_position, new_position, confidence_level, note, created_at) VALUES
-- Pitch 3 (Instant Transfer) — Transfer API scope
(9,  3, 3,  0, 40, 3, 'Backend API skeleton + PSP sandbox connected',         CURRENT_TIMESTAMP - INTERVAL '6 days'),
(9,  3, 3, 40, 75, 4, 'Idempotency logic + webhook handler done',             CURRENT_TIMESTAMP - INTERVAL '3 days'),
-- IBAN Validation scope
(10, 3, 3,  0, 30, 3, 'MOD-97 algorithm implemented, BIC lookup in progress', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(10, 3, 3, 30, 60, 4, 'BIC registry lookup working for 42 SEPA countries',   CURRENT_TIMESTAMP - INTERVAL '2 days'),
-- Biometric Login — fully completed
(1,  1, 3,  0, 50, 3, 'LocalAuthentication POC working on iOS simulator',     '2026-02-03 17:00:00'),
(1,  1, 3, 50, 85, 4, 'Real device testing on iPhone 15 — looking good',      '2026-02-05 17:00:00'),
(1,  1, 3, 85,100, 5, 'Security review passed, shipped to production',         '2026-03-13 16:00:00');

-- ── STEP 23 — RESET ALL SEQUENCES ────────────────────────────────────────────
SELECT setval('projects_id_seq',            (SELECT COALESCE(MAX(id), 0) + 1 FROM projects),            false);
SELECT setval('persons_id_seq',             (SELECT COALESCE(MAX(id), 0) + 1 FROM persons),             false);
SELECT setval('users_id_seq',               (SELECT COALESCE(MAX(id), 0) + 1 FROM users),               false);
SELECT setval('cycles_id_seq',              (SELECT COALESCE(MAX(id), 0) + 1 FROM cycles),              false);
SELECT setval('teams_id_seq',               (SELECT COALESCE(MAX(id), 0) + 1 FROM teams),               false);
SELECT setval('pitches_id_seq',             (SELECT COALESCE(MAX(id), 0) + 1 FROM pitches),             false);
SELECT setval('hill_chart_points_id_seq',   (SELECT COALESCE(MAX(id), 0) + 1 FROM hill_chart_points),  false);
SELECT setval('betting_slots_id_seq',       (SELECT COALESCE(MAX(id), 0) + 1 FROM betting_slots),      false);
SELECT setval('tasks_id_seq',               (SELECT COALESCE(MAX(id), 0) + 1 FROM tasks),               false);
SELECT setval('bug_reports_id_seq',         (SELECT COALESCE(MAX(id), 0) + 1 FROM bug_reports),        false);
SELECT setval('test_cases_id_seq',          (SELECT COALESCE(MAX(id), 0) + 1 FROM test_cases),         false);
SELECT setval('retrospectives_id_seq',      (SELECT COALESCE(MAX(id), 0) + 1 FROM retrospectives),     false);
SELECT setval('retro_items_id_seq',         (SELECT COALESCE(MAX(id), 0) + 1 FROM retro_items),        false);
SELECT setval('work_logs_id_seq',           (SELECT COALESCE(MAX(id), 0) + 1 FROM work_logs),          false);
SELECT setval('meetings_id_seq',            (SELECT COALESCE(MAX(id), 0) + 1 FROM meetings),            false);
SELECT setval('initiatives_id_seq',         (SELECT COALESCE(MAX(id), 0) + 1 FROM initiatives),        false);
SELECT setval('epics_id_seq',               (SELECT COALESCE(MAX(id), 0) + 1 FROM epics),              false);
SELECT setval('releases_id_seq',            (SELECT COALESCE(MAX(id), 0) + 1 FROM releases),           false);
SELECT setval('comments_id_seq',            (SELECT COALESCE(MAX(id), 0) + 1 FROM comments),            false);
