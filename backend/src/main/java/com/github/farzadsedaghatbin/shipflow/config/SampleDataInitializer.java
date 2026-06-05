package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO.FeedbackRating;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds realistic 2026 demo data showcasing ShipFlow features.
 *
 * <p>Demo credentials: admin / admin123 (created by DefaultAdminInitializer)
 *
 * <p>Projects: - Mobile Banking App (Shape Up) - DevOps Platform (Kanban)
 *
 * <p>Demo users: admin / admin123 — ADMIN sara / demo123 — MANAGER ali / demo123 — MEMBER mina /
 * demo123 — MEMBER viewer / demo123 — READONLY
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DefaultAdminInitializer (Order=1)
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements CommandLineRunner {

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final TeamRepository teamRepository;
  private final TeamAssignmentRepository teamAssignmentRepository;
  private final PitchRepository pitchRepository;
  private final WorkLogRepository workLogRepository;
  private final MeetingRepository meetingRepository;
  private final EvidenceRepository evidenceRepository;
  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final PasswordEncoder passwordEncoder;
  private final TaskRepository taskRepository;
  private final HillChartPointRepository hillChartPointRepository;
  private final ManualNoteRepository manualNoteRepository;
  private final CustomDashboardRepository customDashboardRepository;
  private final UserPreferenceRepository userPreferenceRepository;
  private final RiskFeedbackRepository riskFeedbackRepository;
  private final DashboardNotificationRepository dashboardNotificationRepository;
  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;
  private final ReleaseRepository releaseRepository;
  private final WiseArchitectureAdviceRepository wiseArchitectureAdviceRepository;
  private final BugReportRepository bugReportRepository;
  private final TestCaseRepository testCaseRepository;
  private final RetrospectiveRepository retrospectiveRepository;
  private final RetroItemRepository retroItemRepository;
  private final SavedViewRepository savedViewRepository;
  private final ImportJobRepository importJobRepository;
  private final OrganizationSettingsRepository organizationSettingsRepository;

  @Override
  @Transactional
  public void run(String... args) {
    // Always ensure a safe OrganizationSettings row exists (idempotent).
    seedOrganizationSettingsIfAbsent();

    // Always seed the Scrum demo project independently so it appears even when
    // the rest of the sample data was already seeded by an older version.
    seedScrumDemoProjectIfAbsent();

    if (cycleRepository.count() > 0) {
      log.info("Sample data already exists, skipping initialization");
      return;
    }

    log.info("Initializing 2026 sample data...");

    // ── Persons ──────────────────────────────────────────────────────────────
    Person saraPerson =
        createPerson(
            "Sara Hosseini",
            "sara@shipflow.dev",
            "Java, Spring Boot, Architecture, Leadership",
            null);
    Person aliPerson =
        createPerson(
            "Ali Rezaei", "ali@shipflow.dev", "Java, Spring Boot, PostgreSQL, Redis", null);
    Person minaPerson =
        createPerson(
            "Mina Ahmadi",
            "mina@shipflow.dev",
            "React, TypeScript, Tailwind CSS, Vite",
            null);
    Person viewerPerson =
        createPerson("View Only", "viewer@shipflow.dev", "Product Management", null);

    // ── Users ─────────────────────────────────────────────────────────────────
    createUser("sara", "demo123", UserRole.MANAGER, saraPerson);
    createUser("ali", "demo123", UserRole.MEMBER, aliPerson);
    createUser("mina", "demo123", UserRole.MEMBER, minaPerson);
    createUser("viewer", "demo123", UserRole.READONLY, viewerPerson);

    User adminUser = userRepository.findByUsername("admin").orElse(null);
    User saraUser = userRepository.findByUsername("sara").orElse(null);
    User aliUser = userRepository.findByUsername("ali").orElse(null);
    User minaUser = userRepository.findByUsername("mina").orElse(null);

    // ── Projects ──────────────────────────────────────────────────────────────
    Project bankingProject =
        Project.builder()
            .name("Mobile Banking App")
            .projectKey("MBA")
            .description(
                "Consumer-facing mobile banking application — payments, biometric auth, "
                    + "spending analytics, and card management.")
            .color("#3B82F6")
            .projectType(ProjectType.SHAPE_UP)
            .owner(saraUser)
            .isActive(true)
            .enableRetrospectives(true)
            .createdAt(LocalDateTime.of(2026, 1, 5, 9, 0))
            .build();
    projectRepository.save(bankingProject);

    Project devopsProject =
        Project.builder()
            .name("DevOps Platform")
            .projectKey("DVP")
            .description(
                "Internal platform engineering — Kubernetes, CI/CD pipelines, observability, "
                    + "and infrastructure automation.")
            .color("#10B981")
            .projectType(ProjectType.KANBAN)
            .owner(aliUser)
            .isActive(true)
            .enableRetrospectives(false)
            .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
            .build();
    projectRepository.save(devopsProject);

    // ── Cycles ────────────────────────────────────────────────────────────────
    // Shape Up — MBA cycles
    Cycle mbaActiveCycle =
        Cycle.builder()
            .project(bankingProject)
            .name("v2.0 — Payments Overhaul")
            .startDate(LocalDate.of(2026, 4, 1))
            .endDate(LocalDate.of(2026, 5, 15))
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(true)
            .build();
    cycleRepository.save(mbaActiveCycle);

    Cycle mbaCompletedCycle =
        Cycle.builder()
            .project(bankingProject)
            .name("v1.5 — Biometric Authentication")
            .startDate(LocalDate.of(2026, 2, 1))
            .endDate(LocalDate.of(2026, 3, 14))
            .phase(CyclePhase.BETTING_COOLDOWN)
            .isActive(false)
            .build();
    cycleRepository.save(mbaCompletedCycle);

    // Kanban — Continuous Flow cycle (mirrors ProjectService.createDefaultKanbanCycle)
    Cycle kanbanCycle =
        Cycle.builder()
            .project(devopsProject)
            .name("Continuous Flow")
            .startDate(LocalDate.of(2026, 1, 10))
            .endDate(LocalDate.of(2099, 12, 31))
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(true)
            .build();
    cycleRepository.save(kanbanCycle);

    // ── Roadmap: Initiatives, Epics, Releases ─────────────────────────────────
    createRoadmapData(bankingProject, devopsProject, saraUser, aliUser);

    // ── Teams ─────────────────────────────────────────────────────────────────
    Team paymentsTeam = Team.builder().name("Payments Team").build();
    teamRepository.save(paymentsTeam);

    Team authTeam = Team.builder().name("Auth Team").build();
    teamRepository.save(authTeam);

    // Active cycle team assignments
    createAssignment(aliPerson, paymentsTeam, TeamMemberRole.BACKEND, mbaActiveCycle.getStartDate(), null);
    createAssignment(minaPerson, paymentsTeam, TeamMemberRole.FRONTEND, mbaActiveCycle.getStartDate(), null);
    createAssignment(saraPerson, paymentsTeam, TeamMemberRole.TECH_LEAD, mbaActiveCycle.getStartDate(), null);

    // Explicit cycle–team assignments (cycle_teams join table — required for slot generation)
    mbaActiveCycle.getTeams().add(paymentsTeam);
    mbaActiveCycle.getTeams().add(authTeam);
    cycleRepository.save(mbaActiveCycle);
    mbaCompletedCycle.getTeams().add(authTeam);
    cycleRepository.save(mbaCompletedCycle);

    // Past cycle team assignments
    createAssignment(aliPerson, authTeam, TeamMemberRole.BACKEND,
        mbaCompletedCycle.getStartDate(), mbaCompletedCycle.getEndDate(), false);
    createAssignment(minaPerson, authTeam, TeamMemberRole.FRONTEND,
        mbaCompletedCycle.getStartDate(), mbaCompletedCycle.getEndDate(), false);
    createAssignment(saraPerson, authTeam, TeamMemberRole.TECH_LEAD,
        mbaCompletedCycle.getStartDate(), mbaCompletedCycle.getEndDate(), false);

    // ── Pitches — MBA (6 pitches across all stages) ───────────────────────────

    // In-cycle DONE pitches (completed cycle)
    Pitch biometricLogin =
        Pitch.builder()
            .title("Biometric Login Flow")
            .description(
                "Replace password-only login with Face ID / fingerprint as the primary auth "
                    + "method. Fallback to PIN. Integrates with iOS LocalAuthentication and "
                    + "Android BiometricPrompt APIs. Server-side: issue short-lived JWT on "
                    + "biometric success, revoke all sessions on device loss.")
            .appetiteDays(6)
            .cycle(mbaCompletedCycle)
            .team(authTeam)
            .status(PitchStatus.DONE)
            .createdAt(LocalDateTime.of(2026, 1, 20, 10, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 10, 16, 0))
            .build();
    pitchRepository.save(biometricLogin);

    Pitch sessionOverhaul =
        Pitch.builder()
            .title("Session Management Overhaul")
            .description(
                "Introduce refresh-token rotation, per-device session listing, and "
                    + "remote logout. Users can see all active sessions (device, last seen, "
                    + "location) and revoke any individually. Prevents session fixation "
                    + "attacks and meets PCI-DSS session requirements.")
            .appetiteDays(4)
            .cycle(mbaCompletedCycle)
            .team(authTeam)
            .status(PitchStatus.DONE)
            .createdAt(LocalDateTime.of(2026, 1, 22, 11, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 12, 14, 0))
            .build();
    pitchRepository.save(sessionOverhaul);

    // In-cycle active pitches (active cycle)
    Pitch instantTransfer =
        Pitch.builder()
            .title("Instant Transfer UI")
            .description(
                "End-to-end flow for instant bank transfers: beneficiary selection, "
                    + "IBAN validation, transfer amount with fee preview, OTP confirmation, "
                    + "and animated success/failure states. Connects to the existing "
                    + "PaymentService. Target: < 5 taps from dashboard to confirmed transfer.")
            .appetiteDays(6)
            .cycle(mbaActiveCycle)
            .team(paymentsTeam)
            .status(PitchStatus.IN_PROGRESS)
            .createdAt(LocalDateTime.of(2026, 3, 20, 9, 0))
            .updatedAt(LocalDateTime.now())
            .build();
    pitchRepository.save(instantTransfer);

    Pitch spendingAnalytics =
        Pitch.builder()
            .title("Spending Analytics Dashboard")
            .description(
                "Monthly and weekly spending breakdown by category (Food, Transport, "
                    + "Bills, Entertainment). Bar chart for last 6 months, donut chart for "
                    + "category split, biggest transactions list. Data derived from existing "
                    + "transaction history. No new backend APIs needed — derive from "
                    + "TransactionService.")
            .appetiteDays(4)
            .cycle(mbaActiveCycle)
            .team(paymentsTeam)
            .status(PitchStatus.STARTED)
            .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0))
            .updatedAt(LocalDateTime.now())
            .build();
    pitchRepository.save(spendingAnalytics);

    // Pre-cycle pitches (no cycle — in betting or shaping queue)
    Pitch cardFreeze =
        Pitch.builder()
            .title("Card Freeze & Unfreeze")
            .description(
                "One-tap card freeze from the card detail screen. Frozen cards reject all "
                    + "transactions at the network level. Unfreeze is equally instant. "
                    + "Append-only freeze_events table for audit. Push notification on "
                    + "freeze/unfreeze. Appetite: 2 days. No new DB tables needed — "
                    + "add frozen_at TIMESTAMPTZ nullable to cards table.")
            .appetiteDays(2)
            .status(PitchStatus.SHAPED)
            .createdAt(LocalDateTime.of(2026, 3, 15, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 25, 11, 0))
            .build();
    pitchRepository.save(cardFreeze);

    Pitch recurringPayments =
        Pitch.builder()
            .title("Recurring Payment Support")
            .description(
                "Allow users to set up standing orders: pick beneficiary, amount, frequency "
                    + "(weekly/monthly/custom), and end date. Scheduler executes transfers "
                    + "automatically. User receives push before each execution. Cancel/pause "
                    + "at any time.")
            .appetiteDays(5)
            .status(PitchStatus.DRAFT)
            .createdAt(LocalDateTime.of(2026, 3, 28, 14, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 29, 16, 0))
            .build();
    pitchRepository.save(recurringPayments);

    Pitch fraudAlert =
        Pitch.builder()
            .title("AI Fraud Detection Alert")
            .description(
                "Use transaction velocity and geo-anomaly signals to flag suspicious "
                    + "transactions in real time. Surface a dismissable alert in the app "
                    + "within 30 seconds of a suspicious transaction. User can confirm "
                    + "legitimate or freeze card immediately.")
            .appetiteDays(6)
            .status(PitchStatus.IDEA)
            .createdAt(LocalDateTime.of(2026, 3, 29, 9, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 29, 9, 0))
            .build();
    pitchRepository.save(fraudAlert);

    // ── Hill Chart Points ──────────────────────────────────────────────────────
    // Completed pitches — near 100%
    createHillChartPoints(biometricLogin, 95, 92, 100, 88);
    createHillChartPoints(sessionOverhaul, 98, 95, 100, 90);
    // Active pitches — realistic in-progress positions
    createHillChartPoints(instantTransfer, 70, 55, 30, 45);
    createHillChartPoints(spendingAnalytics, 80, 40, 20, 60);

    // ── Tasks — MBA active cycle (15+ tasks) ──────────────────────────────────
    createTask(
        "Payment webhook handler",
        "Implement /api/webhooks/payments endpoint. Validate HMAC-SHA256 signature, "
            + "parse PSP event payload, map to internal PaymentEvent, publish to "
            + "ApplicationEventPublisher.",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("8.0"), new BigDecimal("5.0"),
        mbaActiveCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 15), "backend,payments,webhooks");

    createTask(
        "IBAN validation service",
        "ISO 13616 IBAN validation: country code, check digits (MOD97), bank code "
            + "lookup via BIC registry. Return validation result with bank name for UX.",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("5.0"), null,
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 4, 12), "backend,payments,validation");

    createTask(
        "Transfer confirmation screen",
        "OTP input screen with 60-second countdown. Auto-submit on last digit. Resend "
            + "OTP after cooldown. Animated success state with confetti. Error state "
            + "with retry CTA.",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("6.0"), new BigDecimal("4.0"),
        mbaActiveCycle, minaPerson, null, minaPerson, LocalDate.of(2026, 4, 14), "frontend,payments,ui");

    createTask(
        "Balance widget redesign",
        "Redesign home screen balance widget: gradient background, masked balance "
            + "toggle, mini sparkline for last 7 days, quick action buttons.",
        TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("4.0"), new BigDecimal("3.5"),
        mbaActiveCycle, minaPerson, null, minaPerson, LocalDate.of(2026, 4, 5), "frontend,ui,design");

    createTask(
        "Transaction history pagination",
        "Infinite scroll on transaction list. Backend: cursor-based pagination by "
            + "transaction date + ID. Frontend: useInfiniteQuery, skeleton loaders.",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("3.0"),
        mbaActiveCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 18), "backend,frontend,payments");

    createTask(
        "Spending category tagging",
        "Tag each transaction with a spending category using merchant category codes "
            + "(MCC). Seed MCC-to-category mapping table in Flyway migration.",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("6.0"), null,
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 4, 20), "backend,analytics,database");

    createTask(
        "Category breakdown chart",
        "Donut chart for spending category split (Recharts). Animated on mount. "
            + "Tap slice to drill into category transaction list.",
        TaskStatus.BACKLOG, TaskPriority.MEDIUM, new BigDecimal("5.0"), null,
        mbaActiveCycle, minaPerson, null, minaPerson, LocalDate.of(2026, 4, 25), "frontend,charts,analytics");

    createTask(
        "Monthly trend bar chart",
        "Bar chart showing last 6 months total spending per month. Highlight current "
            + "month. Tooltip shows amount and % change vs previous month.",
        TaskStatus.BACKLOG, TaskPriority.LOW, new BigDecimal("4.0"), null,
        mbaActiveCycle, minaPerson, null, minaPerson, LocalDate.of(2026, 4, 28), "frontend,charts,analytics");

    createTask(
        "Push notification for large transfers",
        "Trigger FCM push notification when transfer > 50M IRR. Include amount, "
            + "beneficiary, and deep link to transaction detail.",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("3.0"), null,
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 4, 22), "backend,notifications");

    createTask(
        "Fix decimal rounding in currency display",
        "Bug: Amounts show 3 decimal places in Persian locale. Fix: use "
            + "NumberFormat with maximumFractionDigits=0 for IRR, 2 for EUR/USD.",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("2.0"), new BigDecimal("1.5"),
        mbaActiveCycle, minaPerson, null, aliPerson, LocalDate.of(2026, 4, 3), "frontend,bug,i18n");

    createTask(
        "API rate limiting — /api/transfers",
        "Add Bucket4j rate limit: 10 transfers/min per user. Return 429 with "
            + "Retry-After header. Add to RateLimitFilter.",
        TaskStatus.IN_REVIEW, TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("3.0"),
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 4, 10), "backend,security,rate-limiting");

    createTask(
        "Accessibility audit — payment flow",
        "WCAG 2.1 AA audit for the entire transfer flow. Fix: missing ARIA labels on "
            + "icon buttons, insufficient contrast on disabled states.",
        TaskStatus.BACKLOG, TaskPriority.MEDIUM, new BigDecimal("6.0"), null,
        mbaActiveCycle, minaPerson, null, saraPerson, LocalDate.of(2026, 5, 5), "frontend,a11y,compliance");

    createTask(
        "E2E tests — transfer flow",
        "Playwright E2E: login → dashboard → initiate transfer → OTP confirmation "
            + "→ success screen → verify transaction in history.",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("8.0"), null,
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 5, 8), "testing,e2e,payments");

    createTask(
        "JWT refresh token rotation",
        "Implement refresh token rotation: each /auth/refresh issues new access + "
            + "refresh token and invalidates the previous refresh token (one-time use).",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("5.0"), new BigDecimal("4.5"),
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 4, 2), "backend,security,auth");

    createTask(
        "Error handling for payment timeout",
        "Graceful UI for PSP timeout (>30s): stop spinner, show friendly error, "
            + "offer retry. Log correlation ID for support lookup.",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("3.0"), new BigDecimal("1.5"),
        mbaActiveCycle, minaPerson, null, aliPerson, LocalDate.of(2026, 4, 16), "frontend,error-handling,payments");

    createTask(
        "Performance test — high load transfer",
        "k6 load test: simulate 500 concurrent users initiating transfers for 5 min. "
            + "Target: p99 < 2s, 0% error rate.",
        TaskStatus.TODO, TaskPriority.LOW, new BigDecimal("6.0"), null,
        mbaActiveCycle, aliPerson, null, saraPerson, LocalDate.of(2026, 5, 10), "testing,performance,backend");

    // ── Tasks — DevOps Platform Kanban (21 tasks across all 7 statuses) ───────
    // DONE
    createTask("Set up Kubernetes cluster on AWS EKS",
        "Provision 3-node EKS cluster with managed node groups. Configure VPC, subnets, "
            + "security groups, and IAM roles. Enable cluster autoscaler.",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("12.0"), new BigDecimal("14.0"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 2, 15), "infra,kubernetes,aws");

    createTask("Configure Helm chart for ShipFlow backend",
        "Package Spring Boot app as Helm chart: Deployment, Service, Ingress, "
            + "ConfigMap, Secret. Values file for dev/staging/prod environments.",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("6.0"), new BigDecimal("5.5"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 2, 20), "infra,helm,kubernetes");

    createTask("CI/CD pipeline for frontend (GitHub Actions)",
        "On push to main: npm test → npm build → Docker build → push to GHCR → "
            + "kubectl rollout restart. Cache node_modules between runs.",
        TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("4.5"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 2, 25), "ci-cd,github-actions,frontend");

    createTask("Network policies for pod isolation",
        "Apply Kubernetes NetworkPolicy: deny all ingress by default, allow only "
            + "explicit pod-to-pod communication paths.",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("3.5"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 3, 5), "security,kubernetes,networking");

    createTask("On-call rotation setup (PagerDuty)",
        "Configure PagerDuty escalation policy: L1 → L2 → manager. Alert routing "
            + "from Alertmanager. On-call schedule covering 24/7.",
        TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("3.0"), new BigDecimal("3.0"),
        kanbanCycle, saraPerson, null, saraPerson, LocalDate.of(2026, 3, 10), "ops,alerting,oncall");

    // IN_PROGRESS
    createTask("Prometheus + Alertmanager setup",
        "Deploy kube-prometheus-stack via Helm. Configure alerts: pod restarts > 3, "
            + "CPU > 80%, memory > 90%, disk > 85%. Route to PagerDuty and Slack.",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("8.0"), new BigDecimal("5.0"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 20), "monitoring,prometheus,alerting");

    createTask("Grafana dashboard templates",
        "Build standard dashboards: JVM metrics (heap, GC, threads), HTTP request "
            + "rates, error rates, PostgreSQL connections, Redis hit rate.",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("3.0"),
        kanbanCycle, minaPerson, null, aliPerson, LocalDate.of(2026, 4, 25), "monitoring,grafana,dashboards");

    createTask("Terraform modules for AWS RDS",
        "Reusable Terraform module for PostgreSQL RDS: multi-AZ, encrypted at rest, "
            + "automated backups to S3, parameter group tuning.",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("10.0"), new BigDecimal("6.0"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 5, 1), "infra,terraform,aws");

    createTask("SLA monitoring dashboard",
        "Real-time SLA tracking: availability %, P50/P95/P99 latency, error rate. "
            + "30-day rolling window. Send weekly PDF report to stakeholders.",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("7.0"), new BigDecimal("4.0"),
        kanbanCycle, minaPerson, null, saraPerson, LocalDate.of(2026, 4, 30), "monitoring,sla,reporting");

    // BLOCKED
    createTask("SSL certificate renewal automation",
        "Automate TLS cert renewal with cert-manager + Let's Encrypt. Requires DNS "
            + "validation. Blocked: awaiting security team approval for DNS API key access.",
        TaskStatus.BLOCKED, TaskPriority.HIGH, new BigDecimal("4.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 18), "security,tls,cert-manager");

    // IN_REVIEW
    createTask("Log aggregation with Grafana Loki",
        "Ship pod logs to Loki via Promtail DaemonSet. Configure log retention 30 days. "
            + "Add LogQL queries for error spikes and slow query detection.",
        TaskStatus.IN_REVIEW, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("5.5"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 22), "logging,loki,observability");

    createTask("Redis cluster setup (6 nodes)",
        "Deploy Redis 7 cluster mode: 3 primaries, 3 replicas. Configure maxmemory-policy "
            + "allkeys-lru. Sentinel for failover. TLS between nodes.",
        TaskStatus.IN_REVIEW, TaskPriority.HIGH, new BigDecimal("8.0"), new BigDecimal("7.0"),
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 4, 19), "infra,redis,caching");

    createTask("Documentation site deployment (VitePress)",
        "Deploy VitePress docs to GitHub Pages via Actions. Publish on every push to "
            + "main. Custom domain docs.shipflow.dev. Include API reference.",
        TaskStatus.IN_REVIEW, TaskPriority.LOW, new BigDecimal("4.0"), new BigDecimal("3.5"),
        kanbanCycle, minaPerson, null, saraPerson, LocalDate.of(2026, 4, 21), "docs,vitepress,ci-cd");

    // TODO
    createTask("ArgoCD GitOps deployment",
        "Install ArgoCD. Create Application CRs pointing to Helm charts in Git. "
            + "Sync policy: automated with self-heal. RBAC: devs can view, ops can sync.",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("8.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 5, 10), "ci-cd,argocd,gitops");

    createTask("HashiCorp Vault for secrets",
        "Deploy Vault in HA mode. Migrate all K8s secrets to Vault KV v2. "
            + "Enable Vault Agent Injector for pod secret injection. Audit logging.",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("10.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 5, 15), "security,vault,secrets");

    createTask("PostgreSQL automated backup to S3",
        "pg_dump nightly cron job in K8s. Encrypt with AES-256 before S3 upload. "
            + "Alert if backup fails. Monthly restore drill automation.",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("5.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 5, 8), "database,backup,aws");

    createTask("Container image security scanning (Trivy)",
        "Add Trivy scan step to CI pipeline. Fail build on CRITICAL CVEs. "
            + "Weekly scan of all images in GHCR registry. Report to Slack.",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("4.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 5, 12), "security,ci-cd,containers");

    createTask("Disaster recovery playbook",
        "Document full DR procedure: RTO target 4h, RPO target 1h. Cover DB restore, "
            + "K8s cluster rebuild, DNS failover. Run quarterly drill.",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("6.0"), null,
        kanbanCycle, saraPerson, null, saraPerson, LocalDate.of(2026, 5, 20), "ops,dr,documentation");

    // BACKLOG
    createTask("ELK stack migration from CloudWatch",
        "Migrate app logs from CloudWatch to self-hosted ELK (Elasticsearch + Logstash "
            + "+ Kibana). Estimate 3× cost reduction.",
        TaskStatus.BACKLOG, TaskPriority.MEDIUM, new BigDecimal("15.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 6, 1), "logging,elk,infra");

    createTask("Service mesh with Istio",
        "Deploy Istio for mTLS between all services, traffic mirroring, canary "
            + "deployments, and circuit breakers via VirtualService rules.",
        TaskStatus.BACKLOG, TaskPriority.LOW, new BigDecimal("20.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 6, 15), "infra,istio,service-mesh");

    createTask("Multi-region failover testing",
        "Simulate full region failure: DNS failover to eu-west-1 backup region. "
            + "Verify RDS read replica promotion. Measure actual RTO.",
        TaskStatus.BACKLOG, TaskPriority.LOW, new BigDecimal("12.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 6, 20), "infra,dr,aws");

    // CANCELLED
    createTask("Migrate to AWS ECS (cancelled)",
        "Evaluation of ECS as Kubernetes alternative. Cancelled: team voted to "
            + "stay on EKS for better ecosystem and tooling compatibility.",
        TaskStatus.CANCELLED, TaskPriority.LOW, new BigDecimal("20.0"), null,
        kanbanCycle, aliPerson, null, aliPerson, LocalDate.of(2026, 3, 1), "infra,ecs,cancelled");

    // ── Work Logs ─────────────────────────────────────────────────────────────
    createWorkLog(aliPerson, instantTransfer, LocalDate.of(2026, 4, 1), new BigDecimal("7.5"),
        "Set up payment API integration, drafted webhook endpoint");
    createWorkLog(aliPerson, instantTransfer, LocalDate.of(2026, 4, 2), new BigDecimal("6.0"),
        "Implemented IBAN validation and bank code lookup");
    createWorkLog(aliPerson, instantTransfer, LocalDate.of(2026, 4, 3), new BigDecimal("7.0"),
        "Transfer submission flow + error handling");
    createWorkLog(minaPerson, instantTransfer, LocalDate.of(2026, 4, 2), new BigDecimal("6.5"),
        "Designed beneficiary selection screen with search");
    createWorkLog(minaPerson, instantTransfer, LocalDate.of(2026, 4, 3), new BigDecimal("7.0"),
        "OTP confirmation screen with animated states");
    createWorkLog(saraPerson, instantTransfer, LocalDate.of(2026, 4, 4), new BigDecimal("3.0"),
        "Code review — suggested retry-on-timeout pattern");

    createWorkLog(minaPerson, spendingAnalytics, LocalDate.of(2026, 4, 1), new BigDecimal("5.0"),
        "Dashboard layout prototype, category color system");
    createWorkLog(aliPerson, spendingAnalytics, LocalDate.of(2026, 4, 2), new BigDecimal("4.0"),
        "Transaction aggregation queries by MCC category");

    createWorkLog(aliPerson, biometricLogin, LocalDate.of(2026, 2, 3), new BigDecimal("8.0"),
        "iOS LocalAuthentication integration");
    createWorkLog(aliPerson, biometricLogin, LocalDate.of(2026, 2, 4), new BigDecimal("7.5"),
        "Android BiometricPrompt integration");
    createWorkLog(minaPerson, biometricLogin, LocalDate.of(2026, 2, 5), new BigDecimal("6.0"),
        "UI — enrollment screen, success animation");
    createWorkLog(aliPerson, biometricLogin, LocalDate.of(2026, 2, 6), new BigDecimal("5.5"),
        "JWT issuance on biometric success, device binding");
    createWorkLog(saraPerson, biometricLogin, LocalDate.of(2026, 3, 8), new BigDecimal("3.0"),
        "Final code review + sign off");

    createWorkLog(aliPerson, sessionOverhaul, LocalDate.of(2026, 2, 10), new BigDecimal("7.0"),
        "Refresh token rotation — DB schema + service layer");
    createWorkLog(aliPerson, sessionOverhaul, LocalDate.of(2026, 2, 11), new BigDecimal("6.5"),
        "Per-device session listing and revocation API");
    createWorkLog(minaPerson, sessionOverhaul, LocalDate.of(2026, 2, 12), new BigDecimal("5.5"),
        "Active sessions management UI");

    // ── Meetings ──────────────────────────────────────────────────────────────
    Meeting kickoff = Meeting.builder()
        .pitch(instantTransfer)
        .type("KICKOFF")
        .dateHeld(LocalDate.of(2026, 4, 1))
        .dorReady(true)
        .dodReady(false)
        .notes("Team aligned on scope. IBAN validation covers 42 countries. OTP flow prioritised.")
        .build();
    meetingRepository.save(kickoff);

    Meeting standup = Meeting.builder()
        .pitch(instantTransfer)
        .type("STANDUP")
        .dateHeld(LocalDate.of(2026, 4, 4))
        .dorReady(true)
        .dodReady(false)
        .notes("Backend 60% — webhook handler ready. Frontend 40% — OTP screen in progress. No blockers.")
        .build();
    meetingRepository.save(standup);

    Meeting shapingMeeting = Meeting.builder()
        .pitch(cardFreeze)
        .type("SHAPING")
        .dateHeld(LocalDate.of(2026, 3, 20))
        .dorReady(false)
        .dodReady(false)
        .notes("Appetite confirmed: 2 days. Decided append-only freeze_events vs flag on card. "
            + "Chose flag for simplicity — audit via Envers.")
        .build();
    meetingRepository.save(shapingMeeting);

    Meeting bettingMeeting = Meeting.builder()
        .pitch(cardFreeze)
        .type("BETTING")
        .dateHeld(LocalDate.of(2026, 3, 25))
        .dorReady(true)
        .dodReady(false)
        .notes("Card Freeze bet for v2.1 cooldown. Small but high-value for trust. Sara will handle.")
        .build();
    meetingRepository.save(bettingMeeting);

    Meeting biometricDemo = Meeting.builder()
        .pitch(biometricLogin)
        .type("DEMO")
        .dateHeld(LocalDate.of(2026, 3, 13))
        .dorReady(true)
        .dodReady(true)
        .notes("Live demo to product and security teams. iOS + Android flows shown. "
            + "Security team: approved for production rollout.")
        .build();
    meetingRepository.save(biometricDemo);

    // ── Evidence ──────────────────────────────────────────────────────────────
    Evidence ev1 = Evidence.builder()
        .pitch(instantTransfer)
        .person(aliPerson)
        .date(LocalDate.of(2026, 4, 3))
        .description("Blocker: PSP sandbox returns inconsistent IBAN validation errors. "
            + "Raised support ticket #PT-88234. ETA: 2 working days.")
        .fileUrl(null)
        .build();
    evidenceRepository.save(ev1);

    Evidence ev2 = Evidence.builder()
        .pitch(biometricLogin)
        .person(saraPerson)
        .date(LocalDate.of(2026, 3, 13))
        .description("Security review passed. PCI-DSS session requirements met. "
            + "Signed off for production — see review doc.")
        .fileUrl("https://drive.shipflow.dev/biometric-security-review-v1.5.pdf")
        .build();
    evidenceRepository.save(ev2);

    // ── Bug Reports ───────────────────────────────────────────────────────────
    BugReport criticalBug = BugReport.builder()
        .bugKey("MBA-BUG-001")
        .title("Double submission on network timeout during transfer")
        .description("When the network drops between POST /transfers and the PSP response, "
            + "the user retries and the transfer executes twice. Balance deducted twice.")
        .stepsToReproduce(
            "1. Initiate transfer\n"
            + "2. Kill network after request sent, before 200 received\n"
            + "3. App shows spinner timeout\n"
            + "4. User taps Retry\n"
            + "5. Transfer processed twice")
        .expectedBehavior("Idempotency key prevents duplicate processing.")
        .actualBehavior("PSP processes second request independently. Balance deducted twice.")
        .environment("iOS 17.4 / Production PSP sandbox")
        .severity(BugSeverity.CRITICAL)
        .status(BugStatus.IN_PROGRESS)
        .project(bankingProject)
        .pitch(instantTransfer)
        .cycle(mbaActiveCycle)
        .team(paymentsTeam)
        .reporter(aliUser)
        .assignee(aliPerson)
        .createdAt(LocalDateTime.of(2026, 4, 4, 11, 30))
        .build();
    bugReportRepository.save(criticalBug);

    BugReport majorBug = BugReport.builder()
        .bugKey("MBA-BUG-002")
        .title("Biometric auth fails on Samsung Galaxy S24 Ultra")
        .description("BiometricPrompt dialog does not appear on Samsung Galaxy S24 Ultra "
            + "running One UI 6.1. App falls through to PIN fallback without showing "
            + "the biometric prompt at all.")
        .stepsToReproduce(
            "1. Open app on Samsung Galaxy S24 Ultra (One UI 6.1)\n"
            + "2. Tap 'Login with Biometrics'\n"
            + "3. Observe: PIN screen appears immediately, no biometric prompt")
        .expectedBehavior("BiometricPrompt should appear.")
        .actualBehavior("Biometric dialog skipped silently, falls to PIN.")
        .environment("Samsung Galaxy S24 Ultra / Android 14 / One UI 6.1")
        .severity(BugSeverity.MAJOR)
        .status(BugStatus.OPEN)
        .project(bankingProject)
        .pitch(biometricLogin)
        .cycle(mbaCompletedCycle)
        .reporter(minaUser)
        .createdAt(LocalDateTime.of(2026, 3, 16, 14, 0))
        .build();
    bugReportRepository.save(majorBug);

    BugReport minorBug = BugReport.builder()
        .bugKey("MBA-BUG-003")
        .title("Currency symbol missing in Persian locale transaction list")
        .description("In the Persian (FA) locale, the currency symbol (﷼) does not appear "
            + "next to transaction amounts in the history list. Amount shows as a plain "
            + "number with no unit. English locale is unaffected.")
        .stepsToReproduce(
            "1. Switch app language to Persian\n"
            + "2. Open transaction history\n"
            + "3. Observe: amounts display without ﷼ symbol")
        .expectedBehavior("Amounts formatted as '1,500,000 ﷼' in Persian locale.")
        .actualBehavior("Amounts show as '1500000' with no currency symbol.")
        .environment("iOS 17.4 + Android 14 / Persian locale")
        .severity(BugSeverity.MINOR)
        .status(BugStatus.OPEN)
        .project(bankingProject)
        .reporter(minaUser)
        .createdAt(LocalDateTime.of(2026, 4, 4, 9, 0))
        .build();
    bugReportRepository.save(minorBug);

    // ── Test Cases ────────────────────────────────────────────────────────────
    TestCase tc1 = TestCase.builder()
        .testCaseKey("MBA-TC-001")
        .title("OTP expiry after 3 failed attempts")
        .description("Verify that OTP is invalidated after 3 consecutive wrong entries "
            + "and the user is prompted to request a new code.")
        .preconditions("User is on the OTP confirmation screen after initiating transfer.")
        .steps(
            "1. Enter wrong OTP three times\n"
            + "2. Observe error message on third attempt\n"
            + "3. Verify OTP field is disabled\n"
            + "4. Verify 'Send new code' CTA appears\n"
            + "5. Request new OTP and verify counter resets")
        .expectedResult("OTP locked after 3 failures. New OTP request re-enables input.")
        .pitch(biometricLogin)
        .cycle(mbaCompletedCycle)
        .team(authTeam)
        .status(TestCaseStatus.APPROVED)
        .type(TestCaseType.E2E)
        .priority(TestCasePriority.HIGH)
        .createdBy(aliUser)
        .createdAt(LocalDateTime.of(2026, 2, 8, 10, 0))
        .build();
    testCaseRepository.save(tc1);

    TestCase tc2 = TestCase.builder()
        .testCaseKey("MBA-TC-002")
        .title("Transfer rejected on insufficient balance")
        .description("Verify the payment flow correctly handles insufficient balance: "
            + "shows a clear error before the OTP step to avoid user frustration.")
        .preconditions("User has a balance of 10,000 IRR. Attempting to transfer 1,000,000 IRR.")
        .steps(
            "1. Open transfer screen\n"
            + "2. Enter beneficiary IBAN\n"
            + "3. Enter amount 1,000,000 IRR\n"
            + "4. Tap Continue\n"
            + "5. Observe: inline error before OTP screen appears")
        .expectedResult("Error message 'Insufficient balance' shown inline. OTP screen never shown.")
        .pitch(instantTransfer)
        .cycle(mbaActiveCycle)
        .team(paymentsTeam)
        .status(TestCaseStatus.READY)
        .type(TestCaseType.FUNCTIONAL)
        .priority(TestCasePriority.CRITICAL)
        .createdBy(aliUser)
        .createdAt(LocalDateTime.of(2026, 4, 2, 11, 0))
        .build();
    testCaseRepository.save(tc2);

    // ── Retrospectives ────────────────────────────────────────────────────────
    Retrospective retro1 = Retrospective.builder()
        .title("v1.5 Biometric Auth — Sprint Retrospective")
        .notes("Strong delivery. Biometric auth shipped on time and passed security review. "
            + "Main pain point was lack of real Samsung devices for testing.")
        .status(RetroStatus.CLOSED)
        .cycle(mbaCompletedCycle)
        .project(bankingProject)
        .createdAt(LocalDateTime.of(2026, 3, 14, 14, 0))
        .updatedAt(LocalDateTime.of(2026, 3, 14, 16, 0))
        .closedAt(LocalDateTime.of(2026, 3, 14, 16, 0))
        .build();
    retrospectiveRepository.save(retro1);

    createRetroItem(retro1, "Biometric flow shipped on time and passed PCI audit", RetroColumnType.WENT_WELL, saraUser, 4);
    createRetroItem(retro1, "Team communication via ShipFlow comments kept everyone aligned", RetroColumnType.WENT_WELL, minaUser, 3);
    createRetroItem(retro1, "No Samsung test devices — discovered Galaxy S24 bug only after release", RetroColumnType.DID_NOT_GO_WELL, aliUser, 5);
    createRetroItem(retro1, "Set up cloud-based device farm for regression testing before shipping", RetroColumnType.TRY_NEXT, aliUser, 4);
    createRetroItem(retro1, "Buy 2 Samsung Galaxy devices for QA lab by end of April", RetroColumnType.ACTIONS, saraUser, 3);

    Retrospective retro2 = Retrospective.builder()
        .title("v2.0 Payments Overhaul — Mid-Cycle Check-in")
        .notes("Mid-cycle pulse. Payments team is on track. Double-submission bug needs "
            + "immediate attention — idempotency key story underestimated.")
        .status(RetroStatus.OPEN)
        .cycle(mbaActiveCycle)
        .project(bankingProject)
        .createdAt(LocalDateTime.of(2026, 4, 4, 15, 0))
        .updatedAt(LocalDateTime.now())
        .build();
    retrospectiveRepository.save(retro2);

    createRetroItem(retro2, "Transfer UI design is the best we've ever shipped", RetroColumnType.WENT_WELL, minaPerson.getId() != null ? minaUser : saraUser, 6);
    createRetroItem(retro2, "PSP sandbox reliability is blocking progress — 3 hours lost this week", RetroColumnType.DID_NOT_GO_WELL, aliUser, 4);
    createRetroItem(retro2, "Add idempotency keys to ALL payment endpoints, not just transfer", RetroColumnType.TRY_NEXT, aliUser, 5);
    createRetroItem(retro2, "Use WireMock PSP stub for local dev to avoid sandbox flakiness", RetroColumnType.TRY_NEXT, aliUser, 3);

    // ── Manual Notes ──────────────────────────────────────────────────────────
    ManualNote note1 = ManualNote.builder()
        .title("Idempotency key design decision")
        .content("Decision: Generate idempotency-key on client (UUID v4), include in X-Idempotency-Key "
            + "header. Server stores in transfer_idempotency table (key, user_id, result_payload, "
            + "expires_at 24h). If duplicate key seen within TTL, return cached response.")
        .contextType("pitch")
        .contextId(instantTransfer.getId())
        .pitchId(instantTransfer.getId())
        .authorId(aliPerson.getId())
        .includeInKnowledge(true)
        .build();
    manualNoteRepository.save(note1);

    ManualNote note2 = ManualNote.builder()
        .title("IBAN validation scope — 42 countries")
        .content("Scope confirmed: validate IBAN structure for all 42 countries in SEPA zone. "
            + "Use iban4j library (v3.2.7) — covers format validation + check digit. "
            + "Bank code lookup not required for MVP (add in v2.1).")
        .contextType("pitch")
        .contextId(instantTransfer.getId())
        .pitchId(instantTransfer.getId())
        .authorId(saraPerson.getId())
        .includeInKnowledge(true)
        .build();
    manualNoteRepository.save(note2);

    ManualNote note3 = ManualNote.builder()
        .title("Biometric auth — server-side session architecture")
        .content("Architecture decision: biometric auth stays client-side only. "
            + "Server only sees a valid JWT request — no biometric data ever leaves the device. "
            + "Satisfied PCI-DSS 8.3.1 cardholder authentication requirement.")
        .contextType("pitch")
        .contextId(biometricLogin.getId())
        .pitchId(biometricLogin.getId())
        .authorId(saraPerson.getId())
        .includeInKnowledge(true)
        .build();
    manualNoteRepository.save(note3);

    // ── Custom Dashboards + User Preferences ─────────────────────────────────
    createCustomDashboards(saraPerson, aliPerson, minaPerson);
    createUserPreferences(saraPerson, aliPerson, minaPerson, viewerPerson);

    // ── Risk Feedback ─────────────────────────────────────────────────────────
    createRiskFeedback(instantTransfer, saraUser, aliUser);
    createRiskFeedback(biometricLogin, aliUser, minaUser);

    // ── Dashboard Notifications ───────────────────────────────────────────────
    createDashboardNotifications(saraUser, aliUser, minaUser);

    // ── WISE Architecture advice history ─────────────────────────────────────
    if (adminUser != null) {
      createWiseArchitectureHistory(adminUser, saraUser, aliUser, minaUser,
          instantTransfer, biometricLogin, sessionOverhaul);
    }

    // ── Saved Views ───────────────────────────────────────────────────────────
    if (adminUser != null && saraUser != null) {
      createSampleSavedViews(adminUser, saraUser, bankingProject.getId(), devopsProject.getId());
    }

    // ── Import Jobs history ───────────────────────────────────────────────────
    if (adminUser != null) {
      createSampleImportJobs(adminUser, saraUser);
    }

    log.info(
        "Sample data initialized successfully — Mobile Banking App (Shape Up) + DevOps Platform (Kanban) + Mobile App Scrum Demo (Scrum)");
  }

  /**
   * Ensures that at least one OrganizationSettings row exists with safe MCP defaults. Called
   * unconditionally so that deployments upgraded from older versions (which had no such row) get
   * the row without needing a full re-seed.
   */
  private void seedOrganizationSettingsIfAbsent() {
    if (organizationSettingsRepository.findFirstByOrderByIdAsc().isPresent()) {
      log.info("OrganizationSettings already exists — skipping seed");
      return;
    }
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("ShipFlow Demo")
            .mcpServerEnabled(false)
            .mcpServerWriteEnabled(false)
            .updatedBy("system")
            .build();
    organizationSettingsRepository.save(settings);
    log.info("OrganizationSettings seeded with safe MCP defaults (server=off, write=off)");
  }

  /**
   * Wrapper called unconditionally at startup so the Scrum demo project is added to deployments
   * that were already seeded by an older version (before v1.1.0).
   */
  private void seedScrumDemoProjectIfAbsent() {
    if (projectRepository.existsByProjectKeyNotDeleted("MAS")) {
      log.info("Scrum demo back-fill: MAS project already exists — skipping");
      return;
    }
    // Prefer the seed 'sara' manager; fall back to any existing user so custom DBs work too.
    User ownerUser = userRepository.findByUsername("sara")
        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    if (ownerUser == null) {
      log.info("Scrum demo back-fill: no users exist yet — will be seeded by the full initializer");
      return;
    }
    log.info("Scrum demo back-fill: seeding Mobile App — Scrum Demo (MAS) with owner '{}'", ownerUser.getUsername());
    Person aliPerson = personRepository.findByEmail("ali@shipflow.dev").orElse(null);
    Person minaPerson = personRepository.findByEmail("mina@shipflow.dev").orElse(null);
    Person saraPerson = personRepository.findByEmail("sara@shipflow.dev").orElse(null);
    seedScrumDemoProject(ownerUser, aliPerson, minaPerson, saraPerson);
    log.info("Scrum demo back-fill: complete — Mobile App — Scrum Demo seeded successfully");
  }

  /**
   * Seeds a SCRUM-mode project ("Mobile App — Scrum Demo") with three sprints, story-pointed tasks,
   * and completed-sprint velocity data so the Burndown and Velocity charts render with real data.
   */
  private void seedScrumDemoProject(
      User ownerUser, Person aliPerson, Person minaPerson, Person saraPerson) {
    // Guard against duplicate seeding on subsequent application restarts or test reruns
    if (projectRepository.existsByProjectKeyNotDeleted("MAS")) {
      log.info("Scrum demo project (MAS) already exists — skipping seed");
      return;
    }
    Project scrumProject =
        Project.builder()
            .name("Mobile App — Scrum Demo")
            .projectKey("MAS")
            .description(
                "Cross-platform mobile app delivered in two-week sprints — showcases ShipFlow's "
                    + "Scrum mode with story points, burndown, and velocity tracking.")
            .color("#A855F7")
            .projectType(ProjectType.SCRUM)
            .owner(ownerUser)
            .isActive(true)
            .enableRetrospectives(true)
            .createdAt(LocalDateTime.of(2026, 3, 1, 9, 0))
            .build();
    projectRepository.save(scrumProject);

    // Use relative dates so burndown/velocity charts always render correctly regardless of when the
    // demo is run. Dates are anchored to LocalDate.now() so completedAt always falls within the
    // sprint window (Sprint 1/2 are in the past, Sprint 3 is the active sprint).
    LocalDate now = LocalDate.now();

    // Sprint 1 — completed sprint (8 weeks ago → 6 weeks ago)
    LocalDate sprint1Start = now.minusWeeks(8);
    LocalDate sprint1End = now.minusWeeks(6);
    Cycle sprint1 =
        Cycle.builder()
            .project(scrumProject)
            .name("Sprint 1")
            .startDate(sprint1Start)
            .endDate(sprint1End)
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(false)
            .sprintGoal("Ship onboarding flow with email + social sign-in")
            .build();
    cycleRepository.save(sprint1);

    // Sprint 2 — completed sprint (5 weeks ago → 3 weeks ago)
    LocalDate sprint2Start = now.minusWeeks(5);
    LocalDate sprint2End = now.minusWeeks(3);
    Cycle sprint2 =
        Cycle.builder()
            .project(scrumProject)
            .name("Sprint 2")
            .startDate(sprint2Start)
            .endDate(sprint2End)
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(false)
            .sprintGoal("Add push notifications and in-app messaging")
            .build();
    cycleRepository.save(sprint2);

    // Sprint 3 — active sprint (2 weeks ago → 3 days from now)
    LocalDate sprint3Start = now.minusWeeks(2);
    LocalDate sprint3End = now.plusDays(3);
    Cycle sprint3 =
        Cycle.builder()
            .project(scrumProject)
            .name("Sprint 3")
            .startDate(sprint3Start)
            .endDate(sprint3End)
            .phase(CyclePhase.SHAPING_BUILDING)
            .isActive(true)
            .sprintGoal("Polish UX, fix top customer-reported bugs, ship dark mode")
            .build();
    cycleRepository.save(sprint3);

    // Sprint 1 tasks (all DONE — 5 + 3 + 5 = 13 pts)
    // Spread completedAt across the sprint so the burndown chart shows a descending staircase
    // rather than all points dropping on the same day.
    createScrumTask("Email sign-up endpoint", TaskStatus.DONE, TaskPriority.HIGH, 5, sprint1,
        aliPerson, saraPerson, "backend,auth", sprint1Start.plusDays(3).atTime(11, 0));
    createScrumTask("Google OAuth integration", TaskStatus.DONE, TaskPriority.HIGH, 3, sprint1,
        aliPerson, saraPerson, "backend,oauth", sprint1Start.plusDays(7).atTime(15, 30));
    createScrumTask("Onboarding wizard screens", TaskStatus.DONE, TaskPriority.MEDIUM, 5, sprint1,
        minaPerson, saraPerson, "frontend,ux", sprint1Start.plusDays(12).atTime(9, 45));

    // Sprint 2 tasks (all DONE — 8 + 3 + 5 = 16 pts)
    createScrumTask("APNs + FCM token registration", TaskStatus.DONE, TaskPriority.HIGH, 8, sprint2,
        aliPerson, saraPerson, "backend,notifications", sprint2Start.plusDays(4).atTime(14, 0));
    createScrumTask("Notification preferences UI", TaskStatus.DONE, TaskPriority.MEDIUM, 3, sprint2,
        minaPerson, saraPerson, "frontend", sprint2Start.plusDays(8).atTime(10, 15));
    createScrumTask("In-app message center", TaskStatus.DONE, TaskPriority.MEDIUM, 5, sprint2,
        minaPerson, saraPerson, "frontend,messaging", sprint2Start.plusDays(11).atTime(16, 0));

    // Sprint 3 tasks (mix of statuses for a realistic burndown chart)
    // Spread the two completed tasks across the sprint window (not the same day)
    createScrumTask("Dark-mode theme tokens", TaskStatus.DONE, TaskPriority.MEDIUM, 2, sprint3,
        minaPerson, saraPerson, "frontend,theme", sprint3Start.plusDays(3).atTime(11, 0));
    createScrumTask("Fix top 5 crash reports", TaskStatus.DONE, TaskPriority.HIGH, 5, sprint3,
        aliPerson, saraPerson, "bugfix", sprint3Start.plusDays(8).atTime(17, 0));
    createScrumTask("Accessibility audit pass", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, 3,
        sprint3, minaPerson, saraPerson, "a11y,frontend", null);
    createScrumTask("Performance: lazy-load images", TaskStatus.TODO, TaskPriority.LOW, 2, sprint3,
        minaPerson, saraPerson, "performance,frontend", null);
  }

  private void createScrumTask(
      String title,
      TaskStatus status,
      TaskPriority priority,
      int storyPoints,
      Cycle cycle,
      Person assignee,
      Person createdBy,
      String tags,
      LocalDateTime completedAt) {
    Task task =
        Task.builder()
            .title(title)
            .description(title + " — Scrum demo task")
            .status(status)
            .priority(priority)
            .storyPoints(storyPoints)
            .cycle(cycle)
            .project(cycle.getProject())
            .assignee(assignee)
            .createdBy(createdBy)
            .tags(tags)
            .build();
    if (completedAt != null) {
      task.setCompletedAt(completedAt);
    }
    taskRepository.save(task);
  }

  // ── Helper Methods ──────────────────────────────────────────────────────────

  private void createUser(String username, String password, UserRole role, Person person) {
    if (!userRepository.existsByUsername(username)) {
      User user =
          User.builder()
              .username(username)
              .password(passwordEncoder.encode(password))
              .role(role)
              .person(person)
              .isActive(true)
              .build();
      userRepository.save(user);
    }
  }

  private Person createPerson(String name, String email, String skills, String avatarUrl) {
    Person person =
        Person.builder()
            .name(name)
            .email(email)
            .skills(skills)
            .avatarUrl(avatarUrl)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    return personRepository.save(person);
  }

  private void createAssignment(
      Person person, Team team, TeamMemberRole role, LocalDate startDate, LocalDate endDate) {
    createAssignment(person, team, role, startDate, endDate, true);
  }

  private void createAssignment(
      Person person,
      Team team,
      TeamMemberRole role,
      LocalDate startDate,
      LocalDate endDate,
      boolean isActive) {
    TeamAssignment assignment =
        TeamAssignment.builder()
            .person(person)
            .team(team)
            .role(role)
            .startDate(startDate)
            .endDate(endDate)
            .isActive(isActive)
            .build();
    teamAssignmentRepository.save(assignment);
  }

  private void createWorkLog(
      Person person, Pitch pitch, LocalDate date, BigDecimal hours, String note) {
    WorkLog workLog =
        WorkLog.builder().person(person).pitch(pitch).date(date).hoursSpent(hours).note(note).build();
    workLogRepository.save(workLog);
  }

  private void createTask(
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      BigDecimal estimateHours,
      BigDecimal actualHours,
      Cycle cycle,
      Person assignee,
      Person pairAssignee,
      Person createdBy,
      LocalDate dueDate,
      String tags) {
    Task task =
        Task.builder()
            .title(title)
            .description(description)
            .status(status)
            .priority(priority)
            .estimateHours(estimateHours)
            .actualHours(actualHours)
            .cycle(cycle)
            .assignee(assignee)
            .pairAssignee(pairAssignee)
            .createdBy(createdBy)
            .dueDate(dueDate)
            .tags(tags)
            .build();

    if (status == TaskStatus.DONE) {
      task.setCompletedAt(LocalDateTime.now().minusDays(1));
    }

    taskRepository.save(task);
  }

  private void createHillChartPoints(
      Pitch pitch, int backendPos, int frontendPos, int testingPos, int integrationPos) {
    hillChartPointRepository.save(
        HillChartPoint.builder()
            .pitch(pitch)
            .scope("Backend Services")
            .description("API endpoints, business logic, and data layer for " + pitch.getTitle())
            .position(backendPos)
            .build());

    hillChartPointRepository.save(
        HillChartPoint.builder()
            .pitch(pitch)
            .scope("Frontend UI")
            .description("Screens, components, and interactions for " + pitch.getTitle())
            .position(frontendPos)
            .build());

    hillChartPointRepository.save(
        HillChartPoint.builder()
            .pitch(pitch)
            .scope("Testing & QA")
            .description("Unit tests, integration tests, and manual QA")
            .position(testingPos)
            .build());

    hillChartPointRepository.save(
        HillChartPoint.builder()
            .pitch(pitch)
            .scope("Third-party Integration")
            .description("PSP, notification services, external API connections")
            .position(integrationPos)
            .build());
  }

  private void createRetroItem(
      Retrospective retro, String content, RetroColumnType columnType, User author, int votes) {
    RetroItem item =
        RetroItem.builder()
            .content(content)
            .columnType(columnType)
            .retrospective(retro)
            .author(author)
            .isAnonymous(false)
            .voteCount(votes)
            .actedOn(columnType == RetroColumnType.ACTIONS)
            .createdAt(retro.getCreatedAt().plusMinutes(votes * 3L))
            .updatedAt(retro.getCreatedAt().plusMinutes(votes * 3L))
            .build();
    retroItemRepository.save(item);
  }

  private void createCustomDashboards(Person... persons) {
    for (Person person : persons) {
      User userEntity = userRepository.findByPersonId(person.getId()).orElse(null);
      if (userEntity == null) continue;

      CustomDashboard dashboard =
          CustomDashboard.builder()
              .user(userEntity)
              .name(person.getName() + "'s Dashboard")
              .isDefault(true)
              .build();
      customDashboardRepository.save(dashboard);
    }
  }

  private void createUserPreferences(Person... persons) {
    for (Person person : persons) {
      User userEntity = userRepository.findByPersonId(person.getId()).orElse(null);
      if (userEntity == null) continue;

      UserPreference pref = UserPreference.builder().user(userEntity).build();
      userPreferenceRepository.save(pref);
    }
  }

  private void createDashboardNotifications(User... users) {
    String[] taskTitles = {
      "Payment webhook handler",
      "Biometric auth flow review",
      "IBAN validation service",
      "Prometheus monitoring setup",
      "Transfer confirmation screen"
    };
    String[] pitchTitles = {
      "Instant Transfer UI",
      "Biometric Login Flow",
      "Card Freeze & Unfreeze",
      "Spending Analytics Dashboard",
      "Session Management Overhaul"
    };

    for (int i = 0; i < users.length; i++) {
      User user = users[i];

      dashboardNotificationRepository.save(
          DashboardNotification.builder()
              .user(user)
              .type("TASK_ASSIGNED")
              .title("New Task Assigned")
              .message("You have been assigned: " + taskTitles[i % taskTitles.length])
              .isRead(false)
              .createdAt(LocalDateTime.now().minusHours(i + 1))
              .build());

      dashboardNotificationRepository.save(
          DashboardNotification.builder()
              .user(user)
              .type("PITCH_STATUS_CHANGED")
              .title("Pitch Status Updated")
              .message(
                  "Pitch '"
                      + pitchTitles[i % pitchTitles.length]
                      + "' moved to "
                      + (i % 2 == 0 ? "IN_PROGRESS" : "TESTING"))
              .isRead(true)
              .createdAt(LocalDateTime.now().minusDays(i + 1))
              .build());

      if (i % 2 == 0) {
        dashboardNotificationRepository.save(
            DashboardNotification.builder()
                .user(user)
                .type("MENTION")
                .title("You were mentioned")
                .message("@" + (i == 0 ? "ali" : "sara") + " mentioned you in a comment on 'Instant Transfer UI'")
                .isRead(false)
                .createdAt(LocalDateTime.now().minusMinutes(30 + i * 15))
                .build());
      }
    }
  }

  private void createRiskFeedback(Pitch pitch, User... reviewers) {
    for (int i = 0; i < reviewers.length; i++) {
      RiskFeedback feedback =
          RiskFeedback.builder()
              .pitch(pitch)
              .user(reviewers[i])
              .originalRiskScore(i == 0 ? 72 : 55)
              .rating(FeedbackRating.ACCURATE)
              .suggestedRiskScore(i == 0 ? 75 : null)
              .notes(
                  i == 0
                      ? "PSP dependency is the main risk — sandbox flakiness already cost us time. "
                          + "Add WireMock stub before next cycle."
                      : "Scope is well-defined. Main concern is Samsung device coverage for biometrics.")
              .missedFactors(
                  i == 0
                      ? "Consider adding integration test against real PSP in staging, not just sandbox"
                      : "Device fragmentation risk for biometric APIs on custom Android ROMs")
              .build();
      riskFeedbackRepository.save(feedback);
    }
  }

  private void createRoadmapData(
      Project bankingProject, Project devopsProject, User saraUser, User aliUser) {

    // ── MBA Initiatives ───────────────────────────────────────────────────────
    Initiative securityInitiative =
        Initiative.builder()
            .name("Security & Compliance 2026")
            .description(
                "Achieve PCI-DSS Level 1 compliance. Implement biometric auth, "
                    + "token rotation, fraud detection, and full audit trail.")
            .status(InitiativeStatus.IN_PROGRESS)
            .color("#EF4444")
            .targetStartDate(LocalDate.of(2026, 1, 1))
            .targetEndDate(LocalDate.of(2026, 6, 30))
            .project(bankingProject)
            .owner(saraUser)
            .sortOrder(1)
            .build();
    initiativeRepository.save(securityInitiative);

    Initiative paymentsInitiative =
        Initiative.builder()
            .name("Payments Excellence 2026")
            .description(
                "Make payments instant, reliable, and intelligent. "
                    + "Instant transfers, recurring payments, spending analytics.")
            .status(InitiativeStatus.IN_PROGRESS)
            .color("#3B82F6")
            .targetStartDate(LocalDate.of(2026, 2, 1))
            .targetEndDate(LocalDate.of(2026, 9, 30))
            .project(bankingProject)
            .owner(saraUser)
            .sortOrder(2)
            .build();
    initiativeRepository.save(paymentsInitiative);

    // ── MBA Epics ──────────────────────────────────────────────────────────────
    Epic authEpic =
        Epic.builder()
            .name("Authentication Modernisation")
            .description("Replace password-only auth with biometric + OTP. Session hygiene overhaul.")
            .status(EpicStatus.IN_PROGRESS)
            .initiative(securityInitiative)
            .project(bankingProject)
            .owner(saraUser)
            .targetStartDate(LocalDate.of(2026, 1, 15))
            .targetEndDate(LocalDate.of(2026, 3, 31))
            .sortOrder(1)
            .build();
    epicRepository.save(authEpic);

    Epic paymentsEpic =
        Epic.builder()
            .name("Instant Payment Flow")
            .description("End-to-end instant transfer UI, IBAN validation, idempotency, and analytics.")
            .status(EpicStatus.IN_PROGRESS)
            .initiative(paymentsInitiative)
            .project(bankingProject)
            .owner(saraUser)
            .targetStartDate(LocalDate.of(2026, 3, 15))
            .targetEndDate(LocalDate.of(2026, 5, 31))
            .sortOrder(1)
            .build();
    epicRepository.save(paymentsEpic);

    // ── MBA Releases ───────────────────────────────────────────────────────────
    Release v15Release =
        Release.builder()
            .name("v1.5 — Biometric Authentication")
            .description("Biometric login, session management overhaul. PCI-DSS compliant.")
            .version("1.5.0")
            .status(ReleaseStatus.RELEASED)
            .project(bankingProject)
            .releaseDate(LocalDate.of(2026, 3, 14))
            .build();
    releaseRepository.save(v15Release);

    Release v20Release =
        Release.builder()
            .name("v2.0 — Payments Overhaul")
            .description("Instant transfer UI, IBAN validation, spending analytics, card freeze.")
            .version("2.0.0")
            .status(ReleaseStatus.IN_PROGRESS)
            .project(bankingProject)
            .releaseDate(LocalDate.of(2026, 5, 20))
            .build();
    releaseRepository.save(v20Release);

    // ── DVP Initiative ─────────────────────────────────────────────────────────
    Initiative infraInitiative =
        Initiative.builder()
            .name("Platform Reliability 2026")
            .description(
                "SLA 99.9% uptime. Full observability stack, GitOps deployments, "
                    + "automated DR, and security hardening.")
            .status(InitiativeStatus.IN_PROGRESS)
            .color("#10B981")
            .targetStartDate(LocalDate.of(2026, 1, 10))
            .targetEndDate(LocalDate.of(2026, 12, 31))
            .project(devopsProject)
            .owner(aliUser)
            .sortOrder(1)
            .build();
    initiativeRepository.save(infraInitiative);
  }

  private void createWiseArchitectureHistory(
      User adminUser, User saraUser, User aliUser, User minaUser,
      Pitch instantTransfer, Pitch biometricLogin, Pitch sessionOverhaul) {

    // ── Conversation 1: Sara — Instant Transfer Architecture ──────────────────
    String conv1Id = "conv-wise-" + UUID.randomUUID().toString().substring(0, 8);
    LocalDateTime conv1Start = LocalDateTime.now().minusDays(5);

    wiseArchitectureAdviceRepository.save(
        WiseArchitectureAdvice.builder()
            .conversationId(conv1Id)
            .pitch(instantTransfer)
            .userId(saraUser.getId())
            .messageType("INITIAL_SOLUTION")
            .userMessage("Initial solution request")
            .aiResponse(
                "## Backend Spring — Idempotent Payment API\n\n"
                    + "Design pattern: **Idempotent REST + Outbox Pattern** for reliable payment processing.\n\n"
                    + "### Architecture\n"
                    + "- **X-Idempotency-Key header** (UUID v4 from client) — stored in `transfer_idempotency` table\n"
                    + "- **Outbox table** — `payment_outbox(id, payload, status, created_at, processed_at)` for retry safety\n"
                    + "- **PaymentService** — checks idempotency cache first, processes if new, returns cached result if duplicate\n\n"
                    + "### Implementation Steps\n"
                    + "1. **Idempotency table** (~2h) — Flyway migration, unique constraint on (user_id, idempotency_key)\n"
                    + "2. **PaymentService guard** (~3h) — check cache on entry, return cached on duplicate\n"
                    + "3. **Outbox processor** (~4h) — `@Scheduled` job retries PENDING outbox entries with exponential backoff\n"
                    + "4. **PSP adapter** (~3h) — Stripe/PSP SDK wrapped behind `PaymentGatewayPort` interface\n\n"
                    + "### Recommended Libraries\n"
                    + "- **resilience4j-spring-boot3** — Circuit breaker around PSP calls\n"
                    + "- **spring-retry** — Declarative retry for transient network errors\n"
                    + "- **iban4j** v3.2.7 — IBAN format validation for 42 SEPA countries")
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(false)
            .hasGitHubContext(true)
            .hasRoadmapContext(true)
            .processingTimeMs(5840L)
            .feedbackHelpful(true)
            .feedbackText("Exactly what we needed. The idempotency design solves the double-submission bug perfectly.")
            .feedbackAt(conv1Start.plusHours(3))
            .createdAt(conv1Start)
            .updatedAt(conv1Start.plusHours(3))
            .build());

    wiseArchitectureAdviceRepository.save(
        WiseArchitectureAdvice.builder()
            .conversationId(conv1Id)
            .pitch(instantTransfer)
            .userId(saraUser.getId())
            .messageType("FOLLOW_UP_QUESTION")
            .userMessage("What TTL should we use for the idempotency cache?")
            .aiResponse(null)
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(false)
            .hasGitHubContext(false)
            .hasRoadmapContext(false)
            .processingTimeMs(null)
            .createdAt(conv1Start.plusDays(1))
            .updatedAt(conv1Start.plusDays(1))
            .build());

    wiseArchitectureAdviceRepository.save(
        WiseArchitectureAdvice.builder()
            .conversationId(conv1Id)
            .pitch(instantTransfer)
            .userId(saraUser.getId())
            .messageType("FOLLOW_UP_ANSWER")
            .userMessage("What TTL should we use for the idempotency cache?")
            .aiResponse(
                "For payment idempotency, **24 hours** is the industry standard (Stripe, Adyen both use this).\n\n"
                    + "Rationale:\n"
                    + "- Long enough to catch retries from network failures or app crashes\n"
                    + "- Short enough to avoid false-positive rejection of legitimate re-submissions days later\n"
                    + "- Set TTL via scheduled cleanup job rather than relying on DB row TTL for auditability\n\n"
                    + "Implementation: `TIMESTAMPTZ expires_at = created_at + INTERVAL '24 hours'`\n"
                    + "Nightly cleanup job deletes expired records to keep the table lean.")
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(false)
            .hasGitHubContext(false)
            .hasRoadmapContext(false)
            .processingTimeMs(1820L)
            .createdAt(conv1Start.plusDays(1).plusMinutes(4))
            .updatedAt(conv1Start.plusDays(1).plusMinutes(4))
            .build());

    // ── Conversation 2: Ali — Biometric Auth Backend ──────────────────────────
    String conv2Id = "conv-wise-" + UUID.randomUUID().toString().substring(0, 8);
    LocalDateTime conv2Start = LocalDateTime.now().minusDays(45);

    wiseArchitectureAdviceRepository.save(
        WiseArchitectureAdvice.builder()
            .conversationId(conv2Id)
            .pitch(biometricLogin)
            .userId(aliUser.getId())
            .messageType("INITIAL_SOLUTION")
            .userMessage("Initial solution request")
            .aiResponse(
                "## Backend Spring — Biometric Auth (Zero Server-Side Biometric Data)\n\n"
                    + "**Core principle**: Biometric verification happens entirely on-device. "
                    + "The server only sees a standard JWT request — no biometric data ever transmitted.\n\n"
                    + "### Flow\n"
                    + "1. **Enrollment**: User enables biometric on device → app generates a device-bound key pair\n"
                    + "2. **Public key registration**: `POST /api/auth/devices` stores `(userId, deviceId, publicKey)`\n"
                    + "3. **Login**: Biometric unlocks private key → signs a challenge → server verifies signature\n"
                    + "4. **JWT issuance**: Standard JWT issued on successful signature verification\n\n"
                    + "### Implementation Steps\n"
                    + "1. **Device registration endpoint** (~3h)\n"
                    + "2. **Challenge generation** (~2h) — time-bound HMAC challenge (30s TTL)\n"
                    + "3. **Signature verification** (~3h) — ECDSA P-256 via Java Security\n"
                    + "4. **JWT with device binding claim** (~1h) — include `device_id` in JWT for audit\n\n"
                    + "### PCI-DSS 8.3.1 Compliance\n"
                    + "This architecture satisfies PCI-DSS 8.3.1 (multi-factor authentication) "
                    + "because 'something you are' (biometric) + 'something you have' (device key) = MFA.")
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(false)
            .hasGitHubContext(true)
            .hasRoadmapContext(false)
            .processingTimeMs(6210L)
            .feedbackHelpful(true)
            .feedbackAt(conv2Start.plusHours(2))
            .createdAt(conv2Start)
            .updatedAt(conv2Start.plusHours(2))
            .build());

    // ── Conversation 3: Admin — Session Management ────────────────────────────
    String conv3Id = "conv-wise-" + UUID.randomUUID().toString().substring(0, 8);
    LocalDateTime conv3Start = LocalDateTime.now().minusDays(40);

    wiseArchitectureAdviceRepository.save(
        WiseArchitectureAdvice.builder()
            .conversationId(conv3Id)
            .pitch(sessionOverhaul)
            .userId(adminUser.getId())
            .messageType("INITIAL_SOLUTION")
            .userMessage("Initial solution request")
            .aiResponse(
                "## Backend Spring — Refresh Token Rotation with Per-Device Sessions\n\n"
                    + "### Architecture\n"
                    + "- **`user_sessions` table**: `(id, user_id, device_id, device_name, "
                    + "refresh_token_hash, last_seen_at, last_ip, created_at, revoked_at)`\n"
                    + "- **Token rotation**: Each `/auth/refresh` call issues a NEW refresh token "
                    + "and immediately revokes the old one\n"
                    + "- **Revocation**: SET `revoked_at = NOW()` — soft delete for audit trail\n\n"
                    + "### Implementation Steps\n"
                    + "1. **Session table** (~2h) — Flyway migration, index on `refresh_token_hash`\n"
                    + "2. **TokenService refactor** (~3h) — store SHA-256 hash of refresh token\n"
                    + "3. **Rotation logic** (~2h) — atomic swap: revoke old, issue new in single transaction\n"
                    + "4. **List sessions endpoint** (~1h) — `GET /api/auth/sessions`\n"
                    + "5. **Revoke session endpoint** (~1h) — `DELETE /api/auth/sessions/{id}`\n\n"
                    + "### Security Notes\n"
                    + "Never store raw refresh token — only SHA-256 hash. "
                    + "This prevents mass token exposure if DB is breached.")
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(false)
            .hasGitHubContext(false)
            .hasRoadmapContext(true)
            .processingTimeMs(4930L)
            .createdAt(conv3Start)
            .updatedAt(conv3Start)
            .build());

    log.info("WISE Architecture advice history created: 3 conversations, 7 entries");
  }

  // ── Saved Views ──────────────────────────────────────────────────────────────

  private void createSampleSavedViews(User adminUser, User saraUser, Long bankingProjectId,
      Long devopsProjectId) {

    // Admin — "High Priority Bugs" (Mobile Banking App)
    savedViewRepository.save(
        SavedView.builder()
            .userId(adminUser.getId())
            .projectId(bankingProjectId)
            .name("High Priority Bugs")
            .filters("{\"statusFilter\":[\"BACKLOG\",\"TODO\"],"
                + "\"priorityFilter\":[\"URGENT\",\"HIGH\"],"
                + "\"activeCategory\":\"BUG_FIX\","
                + "\"sortBy\":\"priority\","
                + "\"sortOrder\":\"desc\"}")
            .isDefault(false)
            .build());

    // Admin — "My Tasks In Progress" (Mobile Banking App) — marked as default
    savedViewRepository.save(
        SavedView.builder()
            .userId(adminUser.getId())
            .projectId(bankingProjectId)
            .name("My Tasks In Progress")
            .filters("{\"statusFilter\":[\"IN_PROGRESS\"],"
                + "\"assigneeFilter\":[" + adminUser.getId() + "],"
                + "\"sortBy\":\"updatedAt\","
                + "\"sortOrder\":\"desc\"}")
            .isDefault(true)
            .build());

    // Sara — "Blocked Items" (Mobile Banking App)
    savedViewRepository.save(
        SavedView.builder()
            .userId(saraUser.getId())
            .projectId(bankingProjectId)
            .name("Blocked Items")
            .filters("{\"statusFilter\":[\"BLOCKED\"],"
                + "\"dependencyFilter\":\"blocked\","
                + "\"sortBy\":\"createdAt\","
                + "\"sortOrder\":\"asc\"}")
            .isDefault(false)
            .build());

    // Admin — "All Open" (DevOps Platform)
    savedViewRepository.save(
        SavedView.builder()
            .userId(adminUser.getId())
            .projectId(devopsProjectId)
            .name("All Open")
            .filters("{\"statusFilter\":[\"BACKLOG\",\"TODO\",\"IN_PROGRESS\",\"BLOCKED\",\"IN_REVIEW\"],"
                + "\"sortBy\":\"priority\","
                + "\"sortOrder\":\"desc\"}")
            .isDefault(true)
            .build());

    log.info("Saved views sample data created: 4 entries");
  }

  /** Seeds demo import job history so the Import History page shows realistic entries. */
  private void createSampleImportJobs(User adminUser, User saraUser) {
    // Completed Jira import (with a few row-level errors)
    importJobRepository.save(
        ImportJob.builder()
            .fileName("jira-mobile-banking-export.csv")
            .sourceFormat(ImportSourceFormat.JIRA_CSV)
            .status(ImportJobStatus.COMPLETED)
            .totalRows(142)
            .importedRows(139)
            .failedRows(3)
            .errorLog(
                "Row 47: missing 'Summary' field — skipped\n"
                    + "Row 93: unrecognised status 'AWAITING_REVIEW' — mapped to IN_PROGRESS\n"
                    + "Row 118: empty assignee — task created unassigned")
            .createdBy(adminUser)
            .createdAt(LocalDateTime.of(2026, 5, 10, 14, 23))
            .completedAt(LocalDateTime.of(2026, 5, 10, 14, 24))
            .build());

    // Completed Linear import (clean — zero failures)
    importJobRepository.save(
        ImportJob.builder()
            .fileName("linear-devops-issues.csv")
            .sourceFormat(ImportSourceFormat.LINEAR_CSV)
            .status(ImportJobStatus.COMPLETED)
            .totalRows(58)
            .importedRows(58)
            .failedRows(0)
            .errorLog(null)
            .createdBy(saraUser != null ? saraUser : adminUser)
            .createdAt(LocalDateTime.of(2026, 5, 15, 9, 5))
            .completedAt(LocalDateTime.of(2026, 5, 15, 9, 6))
            .build());

    // Completed Jira CSV import (~50 tasks — small project snapshot)
    importJobRepository.save(
        ImportJob.builder()
            .fileName("jira-payments-backlog-small.csv")
            .sourceFormat(ImportSourceFormat.JIRA_CSV)
            .status(ImportJobStatus.COMPLETED)
            .totalRows(52)
            .importedRows(50)
            .failedRows(2)
            .errorLog(
                "Row 21: missing 'Assignee' field — task created unassigned\n"
                    + "Row 38: invalid priority value 'HOTFIX' — defaulted to MEDIUM")
            .createdBy(adminUser)
            .createdAt(LocalDateTime.of(2026, 5, 18, 11, 30))
            .completedAt(LocalDateTime.of(2026, 5, 18, 11, 31))
            .build());

    // Completed Linear API import (~30 tasks via OAuth)
    importJobRepository.save(
        ImportJob.builder()
            .fileName("linear-api-import")
            .sourceFormat(ImportSourceFormat.LINEAR_API)
            .status(ImportJobStatus.COMPLETED)
            .totalRows(30)
            .importedRows(30)
            .failedRows(0)
            .errorLog(null)
            .createdBy(saraUser != null ? saraUser : adminUser)
            .createdAt(LocalDateTime.of(2026, 5, 20, 14, 0))
            .completedAt(LocalDateTime.of(2026, 5, 20, 14, 2))
            .build());

    // Failed Jira API import — OAuth token expired
    importJobRepository.save(
        ImportJob.builder()
            .fileName("jira-api-import")
            .sourceFormat(ImportSourceFormat.JIRA_API)
            .status(ImportJobStatus.FAILED)
            .totalRows(0)
            .importedRows(0)
            .failedRows(0)
            .errorLog("OAuth token expired — please re-authorise the Jira integration and retry")
            .createdBy(adminUser)
            .createdAt(LocalDateTime.of(2026, 5, 21, 10, 15))
            .completedAt(LocalDateTime.of(2026, 5, 21, 10, 15))
            .build());

    log.info("Import jobs sample data created: 5 demo entries (Jira CSV x2, Linear CSV, Linear API, Jira API failed)");
  }
}
