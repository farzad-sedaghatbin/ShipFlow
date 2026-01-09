package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run before KnowledgeSeeder
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements CommandLineRunner {

    private final CycleRepository cycleRepository;
    private final TeamRepository teamRepository;
    private final PersonRepository personRepository;
    private final TeamAssignmentRepository teamAssignmentRepository;
    private final PitchRepository pitchRepository;
    private final WorkLogRepository workLogRepository;
    private final MeetingRepository meetingRepository;
    private final EvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (cycleRepository.count() > 0) {
            log.info("Sample data already exists, skipping initialization");
            return;
        }

        log.info("Initializing sample data...");

        // Create persons (independent of teams)
        Person alice = createPerson("Alice Johnson", "alice.johnson@example.com", "Java, React, PostgreSQL", null);
        Person bob = createPerson("Bob Smith", "bob.smith@example.com", "Java, Spring Boot, Microservices", null);
        Person carol = createPerson("Carol Williams", "carol.williams@example.com", "Testing, Selenium, Cypress", null);
        Person dave = createPerson("Dave Brown", "dave.brown@example.com", "React, TypeScript, CSS", null);
        Person eve = createPerson("Eve Davis", "eve.davis@example.com", "Figma, UI/UX, Prototyping", null);
        Person frank = createPerson("Frank Miller", "frank.miller@example.com", "Architecture, DevOps, Leadership", null);
        Person grace = createPerson("Grace Lee", "grace.lee@example.com", "Python, Data Analysis, Machine Learning", null);
        Person henry = createPerson("Henry Wilson", "henry.wilson@example.com", "Android, Kotlin, Mobile Development", null);

        // Create users for sample persons
        createUser("alice", "password", UserRole.DEVELOPER, alice);
        createUser("bob", "password", UserRole.DEVELOPER, bob);
        createUser("carol", "password", UserRole.QA, carol);
        createUser("dave", "password", UserRole.DEVELOPER, dave);
        createUser("eve", "password", UserRole.PRODUCT, eve);
        createUser("frank", "password", UserRole.PROJECT_MANAGER, frank);
        createUser("grace", "password", UserRole.DEVELOPER, grace);
        createUser("henry", "password", UserRole.DEVELOPER, henry);

        // Create active cycle
        Cycle activeCycle = Cycle.builder()
                .name("Q1 2025 - Feature Sprint")
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 2, 14))
                .phase(CyclePhase.BUILD)
                .isActive(true)
                .build();
        cycleRepository.save(activeCycle);

        // Create completed cycles for better Reports visualization
        Cycle completedCycle1 = Cycle.builder()
                .name("Q4 2024 - Holiday Release")
                .startDate(LocalDate.of(2024, 11, 4))
                .endDate(LocalDate.of(2024, 12, 13))
                .phase(CyclePhase.COOLDOWN)
                .isActive(false)
                .build();
        cycleRepository.save(completedCycle1);

        Cycle completedCycle2 = Cycle.builder()
                .name("Q3 2024 - Summer Sprint")
                .startDate(LocalDate.of(2024, 8, 5))
                .endDate(LocalDate.of(2024, 9, 20))
                .phase(CyclePhase.COOLDOWN)
                .isActive(false)
                .build();
        cycleRepository.save(completedCycle2);

        Cycle completedCycle3 = Cycle.builder()
                .name("Q2 2024 - Mobile Expansion")
                .startDate(LocalDate.of(2024, 5, 6))
                .endDate(LocalDate.of(2024, 6, 21))
                .phase(CyclePhase.COOLDOWN)
                .isActive(false)
                .build();
        cycleRepository.save(completedCycle3);

        // Create teams for active cycle
        Team alphaTeam = Team.builder()
                .name("Alpha Team")
                .cycle(activeCycle)
                .build();
        teamRepository.save(alphaTeam);

        Team betaTeam = Team.builder()
                .name("Beta Team")
                .cycle(activeCycle)
                .build();
        teamRepository.save(betaTeam);

        // Create teams for past cycles
        Team pastTeam1 = Team.builder()
                .name("Release Team")
                .cycle(completedCycle1)
                .build();
        teamRepository.save(pastTeam1);

        Team pastTeam2 = Team.builder()
                .name("Summer Squad")
                .cycle(completedCycle2)
                .build();
        teamRepository.save(pastTeam2);

        Team pastTeam3 = Team.builder()
                .name("Mobile Team")
                .cycle(completedCycle3)
                .build();
        teamRepository.save(pastTeam3);

        // Create team assignments for active cycle - Alpha Team
        createAssignment(alice, alphaTeam, TeamMemberRole.FULLSTACK, activeCycle.getStartDate(), null);
        createAssignment(bob, alphaTeam, TeamMemberRole.BACKEND, activeCycle.getStartDate(), null);
        createAssignment(carol, alphaTeam, TeamMemberRole.QA, activeCycle.getStartDate(), null);

        // Create team assignments for active cycle - Beta Team
        createAssignment(dave, betaTeam, TeamMemberRole.FRONTEND, activeCycle.getStartDate(), null);
        createAssignment(eve, betaTeam, TeamMemberRole.DESIGNER, activeCycle.getStartDate(), null);
        createAssignment(frank, betaTeam, TeamMemberRole.TECH_LEAD, activeCycle.getStartDate(), null);

        // Create team assignments for past cycles (historical data)
        createAssignment(alice, pastTeam1, TeamMemberRole.TECH_LEAD, completedCycle1.getStartDate(), completedCycle1.getEndDate(), false);
        createAssignment(bob, pastTeam1, TeamMemberRole.BACKEND, completedCycle1.getStartDate(), completedCycle1.getEndDate(), false);
        createAssignment(grace, pastTeam1, TeamMemberRole.FULLSTACK, completedCycle1.getStartDate(), completedCycle1.getEndDate(), false);

        createAssignment(dave, pastTeam2, TeamMemberRole.FRONTEND, completedCycle2.getStartDate(), completedCycle2.getEndDate(), false);
        createAssignment(eve, pastTeam2, TeamMemberRole.DESIGNER, completedCycle2.getStartDate(), completedCycle2.getEndDate(), false);
        createAssignment(frank, pastTeam2, TeamMemberRole.TECH_LEAD, completedCycle2.getStartDate(), completedCycle2.getEndDate(), false);

        createAssignment(henry, pastTeam3, TeamMemberRole.FULLSTACK, completedCycle3.getStartDate(), completedCycle3.getEndDate(), false);
        createAssignment(grace, pastTeam3, TeamMemberRole.BACKEND, completedCycle3.getStartDate(), completedCycle3.getEndDate(), false);
        createAssignment(carol, pastTeam3, TeamMemberRole.QA, completedCycle3.getStartDate(), completedCycle3.getEndDate(), false);

        // Create pitches for active cycle with varied statuses
        Pitch userDashboard = Pitch.builder()
                .title("User Dashboard Redesign")
                .description("Complete redesign of the user dashboard with new analytics widgets and improved UX")
                .appetiteDays(6)
                .cycle(activeCycle)
                .team(alphaTeam)
                .status(PitchStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build();
        pitchRepository.save(userDashboard);

        Pitch apiIntegration = Pitch.builder()
                .title("Third-party API Integration")
                .description("Integrate with external payment provider API and implement webhook handlers")
                .appetiteDays(4)
                .cycle(activeCycle)
                .team(alphaTeam)
                .status(PitchStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();
        pitchRepository.save(apiIntegration);

        Pitch mobileApp = Pitch.builder()
                .title("Mobile App Notification System")
                .description("Push notification system for mobile app with customizable preferences")
                .appetiteDays(5)
                .cycle(activeCycle)
                .team(betaTeam)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        pitchRepository.save(mobileApp);

        Pitch reportModule = Pitch.builder()
                .title("Advanced Reporting Module")
                .description("New reporting module with PDF export and scheduled email delivery")
                .appetiteDays(6)
                .cycle(activeCycle)
                .team(betaTeam)
                .status(PitchStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now().minusDays(8))
                .updatedAt(LocalDateTime.now())
                .build();
        pitchRepository.save(reportModule);

        Pitch searchFeature = Pitch.builder()
                .title("Enhanced Search Functionality")
                .description("Add full-text search with filters and autocomplete")
                .appetiteDays(4)
                .cycle(activeCycle)
                .team(alphaTeam)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(12))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();
        pitchRepository.save(searchFeature);

        // Create pitches for completed cycles with full work history
        Pitch darkMode = Pitch.builder()
                .title("Dark Mode Support")
                .description("Implement dark mode across all UI components")
                .appetiteDays(3)
                .cycle(completedCycle1)
                .team(pastTeam1)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(60))
                .updatedAt(LocalDateTime.now().minusDays(45))
                .build();
        pitchRepository.save(darkMode);

        Pitch performanceOpt = Pitch.builder()
                .title("Performance Optimization")
                .description("Database query optimization and caching implementation")
                .appetiteDays(5)
                .cycle(completedCycle1)
                .team(pastTeam1)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(58))
                .updatedAt(LocalDateTime.now().minusDays(43))
                .build();
        pitchRepository.save(performanceOpt);

        Pitch socialIntegration = Pitch.builder()
                .title("Social Media Integration")
                .description("Share content to Twitter, LinkedIn, and Facebook")
                .appetiteDays(4)
                .cycle(completedCycle2)
                .team(pastTeam2)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(100))
                .updatedAt(LocalDateTime.now().minusDays(85))
                .build();
        pitchRepository.save(socialIntegration);

        Pitch dataExport = Pitch.builder()
                .title("Data Export Feature")
                .description("Export user data in CSV and Excel formats")
                .appetiteDays(3)
                .cycle(completedCycle2)
                .team(pastTeam2)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(95))
                .updatedAt(LocalDateTime.now().minusDays(82))
                .build();
        pitchRepository.save(dataExport);

        Pitch mobileOnboarding = Pitch.builder()
                .title("Mobile Onboarding Flow")
                .description("Improved first-time user experience on mobile")
                .appetiteDays(5)
                .cycle(completedCycle3)
                .team(pastTeam3)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(140))
                .updatedAt(LocalDateTime.now().minusDays(125))
                .build();
        pitchRepository.save(mobileOnboarding);

        Pitch offlineMode = Pitch.builder()
                .title("Offline Mode Support")
                .description("Enable app functionality without internet connection")
                .appetiteDays(6)
                .cycle(completedCycle3)
                .team(pastTeam3)
                .status(PitchStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(138))
                .updatedAt(LocalDateTime.now().minusDays(123))
                .build();
        pitchRepository.save(offlineMode);

        // Create work logs for active cycle pitches
        createWorkLog(alice, userDashboard, LocalDate.now().minusDays(5), new BigDecimal("6.5"), "Designed new widget layout");
        createWorkLog(alice, userDashboard, LocalDate.now().minusDays(4), new BigDecimal("7.0"), "Implemented chart components");
        createWorkLog(alice, userDashboard, LocalDate.now().minusDays(3), new BigDecimal("5.5"), "Added drag-and-drop functionality");
        createWorkLog(alice, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("6.0"), "Fixed responsive layout issues");
        createWorkLog(alice, userDashboard, LocalDate.now().minusDays(1), new BigDecimal("5.0"), "Integrated with analytics API");
        createWorkLog(bob, userDashboard, LocalDate.now().minusDays(4), new BigDecimal("8.0"), "Built API endpoints for widgets");
        createWorkLog(bob, userDashboard, LocalDate.now().minusDays(3), new BigDecimal("6.0"), "Optimized database queries");
        createWorkLog(bob, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("7.0"), "Added caching layer");
        createWorkLog(carol, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("4.0"), "Created test cases");
        createWorkLog(carol, userDashboard, LocalDate.now().minusDays(1), new BigDecimal("5.0"), "Performed integration testing");

        createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(4), new BigDecimal("7.5"), "Set up payment provider SDK");
        createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(3), new BigDecimal("6.0"), "Implemented webhook handlers");
        createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(2), new BigDecimal("6.5"), "Added error handling and retry logic");
        createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(1), new BigDecimal("5.0"), "Writing integration tests");
        createWorkLog(alice, apiIntegration, LocalDate.now().minusDays(2), new BigDecimal("4.0"), "Built admin UI for payment settings");

        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(14), new BigDecimal("8.0"), "Set up notification service");
        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(13), new BigDecimal("7.0"), "Built notification preferences UI");
        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(12), new BigDecimal("6.5"), "Integrated with Firebase");
        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(11), new BigDecimal("6.0"), "Implemented push notification handlers");
        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(10), new BigDecimal("5.5"), "Added notification scheduling");
        createWorkLog(eve, mobileApp, LocalDate.now().minusDays(9), new BigDecimal("5.0"), "Designed notification templates");
        createWorkLog(eve, mobileApp, LocalDate.now().minusDays(8), new BigDecimal("4.5"), "Created user preference flows");
        createWorkLog(frank, mobileApp, LocalDate.now().minusDays(7), new BigDecimal("4.0"), "Code review and architecture refinement");
        createWorkLog(dave, mobileApp, LocalDate.now().minusDays(3), new BigDecimal("6.0"), "Bug fixes and polish");

        createWorkLog(frank, reportModule, LocalDate.now().minusDays(7), new BigDecimal("7.0"), "Designed report templates");
        createWorkLog(frank, reportModule, LocalDate.now().minusDays(6), new BigDecimal("6.5"), "Implemented PDF generation");
        createWorkLog(frank, reportModule, LocalDate.now().minusDays(5), new BigDecimal("7.0"), "Built report scheduler");
        createWorkLog(dave, reportModule, LocalDate.now().minusDays(4), new BigDecimal("5.5"), "Created report UI components");
        createWorkLog(dave, reportModule, LocalDate.now().minusDays(3), new BigDecimal("6.0"), "Added chart visualizations");
        createWorkLog(eve, reportModule, LocalDate.now().minusDays(2), new BigDecimal("4.0"), "Designed report layouts");

        createWorkLog(alice, searchFeature, LocalDate.now().minusDays(11), new BigDecimal("7.5"), "Integrated Elasticsearch");
        createWorkLog(alice, searchFeature, LocalDate.now().minusDays(10), new BigDecimal("6.0"), "Built search API endpoints");
        createWorkLog(alice, searchFeature, LocalDate.now().minusDays(9), new BigDecimal("6.5"), "Implemented autocomplete");
        createWorkLog(bob, searchFeature, LocalDate.now().minusDays(8), new BigDecimal("5.5"), "Added search filters");
        createWorkLog(bob, searchFeature, LocalDate.now().minusDays(7), new BigDecimal("6.0"), "Optimized search performance");
        createWorkLog(alice, searchFeature, LocalDate.now().minusDays(6), new BigDecimal("5.0"), "Built search UI");
        createWorkLog(carol, searchFeature, LocalDate.now().minusDays(3), new BigDecimal("4.5"), "QA testing");

        // Work logs for completed cycle 1 (Q4 2024)
        createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 5), new BigDecimal("7.0"), "Designed dark theme color palette");
        createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 6), new BigDecimal("8.0"), "Implemented theme switching");
        createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 7), new BigDecimal("6.5"), "Updated all components");
        createWorkLog(bob, darkMode, LocalDate.of(2024, 11, 8), new BigDecimal("5.0"), "Fixed theme persistence");
        createWorkLog(grace, darkMode, LocalDate.of(2024, 11, 11), new BigDecimal("4.5"), "Added user preferences");
        createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 12), new BigDecimal("5.5"), "Final polish and testing");

        createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 6), new BigDecimal("8.0"), "Analyzed slow queries");
        createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 7), new BigDecimal("7.5"), "Added database indexes");
        createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 8), new BigDecimal("7.0"), "Implemented Redis caching");
        createWorkLog(grace, performanceOpt, LocalDate.of(2024, 11, 11), new BigDecimal("6.5"), "Optimized API endpoints");
        createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 12), new BigDecimal("6.0"), "Added query result caching");
        createWorkLog(grace, performanceOpt, LocalDate.of(2024, 11, 13), new BigDecimal("5.5"), "Load testing and tuning");
        createWorkLog(alice, performanceOpt, LocalDate.of(2024, 11, 14), new BigDecimal("4.5"), "Performance monitoring setup");

        // Work logs for completed cycle 2 (Q3 2024)
        createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 6), new BigDecimal("7.0"), "Set up OAuth integrations");
        createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 7), new BigDecimal("6.5"), "Built Twitter integration");
        createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 8), new BigDecimal("6.5"), "Built LinkedIn integration");
        createWorkLog(eve, socialIntegration, LocalDate.of(2024, 8, 9), new BigDecimal("5.5"), "Designed share dialogs");
        createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 12), new BigDecimal("6.0"), "Built Facebook integration");
        createWorkLog(frank, socialIntegration, LocalDate.of(2024, 8, 13), new BigDecimal("5.0"), "Added analytics tracking");

        createWorkLog(eve, dataExport, LocalDate.of(2024, 8, 14), new BigDecimal("6.5"), "Designed export UI");
        createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 15), new BigDecimal("7.0"), "Implemented CSV export");
        createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 16), new BigDecimal("6.5"), "Implemented Excel export");
        createWorkLog(dave, dataExport, LocalDate.of(2024, 8, 19), new BigDecimal("5.5"), "Added export filters");
        createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 20), new BigDecimal("5.0"), "Performance optimization");

        // Work logs for completed cycle 3 (Q2 2024)
        createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 7), new BigDecimal("7.5"), "Designed onboarding flow");
        createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 8), new BigDecimal("7.0"), "Built welcome screens");
        createWorkLog(grace, mobileOnboarding, LocalDate.of(2024, 5, 9), new BigDecimal("6.5"), "Implemented tutorial steps");
        createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 10), new BigDecimal("6.0"), "Added skip functionality");
        createWorkLog(grace, mobileOnboarding, LocalDate.of(2024, 5, 13), new BigDecimal("5.5"), "Built progress indicators");
        createWorkLog(carol, mobileOnboarding, LocalDate.of(2024, 5, 14), new BigDecimal("4.5"), "QA and testing");

        createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 15), new BigDecimal("8.0"), "Implemented local database");
        createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 16), new BigDecimal("7.5"), "Built sync mechanism");
        createWorkLog(grace, offlineMode, LocalDate.of(2024, 5, 17), new BigDecimal("7.0"), "Added conflict resolution");
        createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 20), new BigDecimal("6.5"), "Implemented offline queue");
        createWorkLog(grace, offlineMode, LocalDate.of(2024, 5, 21), new BigDecimal("6.0"), "Built background sync");
        createWorkLog(carol, offlineMode, LocalDate.of(2024, 5, 22), new BigDecimal("5.0"), "Offline mode testing");

        // Create meetings
        Meeting kickoff1 = Meeting.builder()
                .pitch(userDashboard)
                .type(MeetingType.KICKOFF)
                .dateHeld(LocalDate.now().minusDays(10))
                .dorReady(true)
                .dodReady(false)
                .notes("Defined scope and milestones")
                .build();
        meetingRepository.save(kickoff1);

        Meeting standup1 = Meeting.builder()
                .pitch(userDashboard)
                .type(MeetingType.STANDUP)
                .dateHeld(LocalDate.now().minusDays(3))
                .dorReady(true)
                .dodReady(false)
                .notes("Progress update - 70% complete")
                .build();
        meetingRepository.save(standup1);

        Meeting shaping = Meeting.builder()
                .pitch(reportModule)
                .type(MeetingType.SHAPING)
                .dateHeld(LocalDate.now().minusDays(5))
                .dorReady(false)
                .dodReady(false)
                .notes("Identified key requirements and risks")
                .build();
        meetingRepository.save(shaping);

        Meeting betting = Meeting.builder()
                .pitch(reportModule)
                .type(MeetingType.BETTING)
                .dateHeld(LocalDate.now().minusDays(3))
                .dorReady(true)
                .dodReady(false)
                .notes("Approved for next build phase")
                .build();
        meetingRepository.save(betting);

        Meeting demo = Meeting.builder()
                .pitch(mobileApp)
                .type(MeetingType.DEMO)
                .dateHeld(LocalDate.now().minusDays(1))
                .dorReady(true)
                .dodReady(true)
                .notes("Successful demo to stakeholders")
                .build();
        meetingRepository.save(demo);

        // Create evidences
        Evidence evidence1 = Evidence.builder()
                .pitch(userDashboard)
                .person(alice)
                .date(LocalDate.now().minusDays(4))
                .description("Blocker: Third-party charting library has performance issues with large datasets")
                .fileUrl(null)
                .build();
        evidenceRepository.save(evidence1);

        Evidence evidence2 = Evidence.builder()
                .pitch(mobileApp)
                .person(dave)
                .date(LocalDate.now().minusDays(6))
                .description("QA passed - All notification flows working correctly on iOS and Android")
                .fileUrl("https://example.com/qa-report.pdf")
                .build();
        evidenceRepository.save(evidence2);

        Evidence evidence3 = Evidence.builder()
                .pitch(apiIntegration)
                .person(bob)
                .date(LocalDate.now().minusDays(1))
                .description("Waiting for API credentials from payment provider - ETA 2 days")
                .fileUrl(null)
                .build();
        evidenceRepository.save(evidence3);

        // Create sample tasks for active cycle
        createTask("Setup Redux store for dashboard", "Initialize Redux state management with slices for widgets and layout", 
                TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("3.5"), activeCycle, alice, null, alice,
                LocalDate.now().plusDays(5), "frontend,redux,setup");

        createTask("Implement widget drag-and-drop", "Add react-beautiful-dnd for dashboard widget rearrangement", 
                TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("5.5"), activeCycle, alice, null, alice,
                LocalDate.now().plusDays(8), "frontend,ux,drag-drop");

        createTask("Create chart API endpoints", "Build REST endpoints for fetching chart data with time range filters", 
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("5.0"), new BigDecimal("3.0"), activeCycle, bob, null, bob,
                LocalDate.now().plusDays(3), "backend,api,charts");

        createTask("Optimize dashboard queries", "Add database indexes and implement query result caching", 
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("2.5"), activeCycle, bob, null, bob,
                LocalDate.now().plusDays(4), "backend,performance,database");

        createTask("Write dashboard integration tests", "Create Cypress tests for dashboard functionality and interactions", 
                TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("8.0"), null, activeCycle, carol, null, alice,
                LocalDate.now().plusDays(7), "testing,cypress,integration");

        createTask("Payment provider SDK integration", "Integrate Stripe SDK and configure API credentials", 
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("6.0"), new BigDecimal("4.0"), activeCycle, bob, null, bob,
                LocalDate.now().plusDays(2), "backend,payment,stripe");

        createTask("Webhook signature verification", "Implement secure webhook verification using provider signatures", 
                TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("3.0"), null, activeCycle, bob, alice, bob,
                LocalDate.now().plusDays(4), "backend,security,webhooks");

        createTask("Payment error handling", "Add comprehensive error handling and user-friendly messages", 
                TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("4.0"), null, activeCycle, bob, null, bob,
                LocalDate.now().plusDays(6), "backend,error-handling");

        createTask("Admin payment settings UI", "Create admin panel for configuring payment provider settings", 
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("3.0"), activeCycle, alice, null, bob,
                LocalDate.now().plusDays(5), "frontend,admin,settings");

        createTask("PDF report template design", "Create professional PDF templates for different report types", 
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("4.0"), activeCycle, eve, null, frank,
                LocalDate.now().plusDays(8), "design,pdf,templates");

        createTask("Report scheduler backend", "Build cron-based scheduler for automated report generation", 
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("7.0"), new BigDecimal("5.0"), activeCycle, frank, null, frank,
                LocalDate.now().plusDays(6), "backend,scheduler,cron");

        createTask("Email delivery service", "Implement email service for sending scheduled reports", 
                TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("5.0"), null, activeCycle, frank, null, frank,
                LocalDate.now().plusDays(9), "backend,email,notifications");

        createTask("Report UI components", "Create React components for report builder and viewer", 
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("8.0"), new BigDecimal("6.0"), activeCycle, dave, null, frank,
                LocalDate.now().plusDays(7), "frontend,react,components");

        createTask("Chart visualization library", "Integrate Chart.js for interactive report charts", 
                TaskStatus.TODO, TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, dave, null, frank,
                LocalDate.now().plusDays(10), "frontend,charts,visualization");

        createTask("Code review: Search feature", "Review and approve search implementation before merge", 
                TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("2.0"), new BigDecimal("1.5"), activeCycle, frank, null, alice,
                LocalDate.now().minusDays(3), "code-review,search");

        createTask("Deploy search to staging", "Deploy Elasticsearch and search API to staging environment", 
                TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("3.0"), new BigDecimal("2.5"), activeCycle, bob, null, alice,
                LocalDate.now().minusDays(2), "devops,deployment,staging");

        createTask("Write API documentation", "Document all new API endpoints with examples", 
                TaskStatus.TODO, TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, alice, bob, alice,
                LocalDate.now().plusDays(12), "documentation,api");

        createTask("Update user guide", "Add documentation for new dashboard features", 
                TaskStatus.TODO, TaskPriority.LOW, new BigDecimal("3.0"), null, activeCycle, eve, null, alice,
                LocalDate.now().plusDays(14), "documentation,user-guide");

        createTask("Security audit: Payment flow", "Conduct security review of payment integration", 
                TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("5.0"), null, activeCycle, frank, bob, frank,
                LocalDate.now().plusDays(8), "security,audit,payment");

        createTask("Performance monitoring setup", "Configure application performance monitoring tools", 
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("4.0"), new BigDecimal("2.0"), activeCycle, bob, null, frank,
                LocalDate.now().plusDays(10), "devops,monitoring,performance");

        createTask("Accessibility review", "Ensure dashboard meets WCAG 2.1 AA standards", 
                TaskStatus.BACKLOG, TaskPriority.MEDIUM, new BigDecimal("6.0"), null, activeCycle, eve, alice, eve,
                LocalDate.now().plusDays(15), "accessibility,a11y,compliance");

        createTask("Mobile responsive fixes", "Fix responsive layout issues on tablet and mobile", 
                TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("5.0"), null, activeCycle, dave, null, alice,
                LocalDate.now().plusDays(9), "frontend,responsive,mobile");

        createTask("Browser compatibility testing", "Test on Safari, Firefox, Chrome, and Edge", 
                TaskStatus.BACKLOG, TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, carol, null, alice,
                LocalDate.now().plusDays(16), "testing,browsers,qa");

        createTask("Database migration script", "Create migration for new dashboard schema changes", 
                TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("2.0"), new BigDecimal("1.5"), activeCycle, bob, null, bob,
                LocalDate.now().minusDays(5), "database,migration");

        createTask("Implement notification preferences", "Add user preferences for notification channels and frequency", 
                TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("4.5"), activeCycle, dave, null, dave,
                LocalDate.now().minusDays(8), "frontend,notifications,preferences");

        log.info("Sample data initialized successfully!");
    }

    private void createUser(String username, String password, UserRole role, Person person) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
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
        Person person = Person.builder()
                .name(name)
                .email(email)
                .skills(skills)
                .avatarUrl(avatarUrl)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        return personRepository.save(person);
    }

    private void createAssignment(Person person, Team team, TeamMemberRole role, LocalDate startDate, LocalDate endDate) {
        createAssignment(person, team, role, startDate, endDate, true);
    }

    private void createAssignment(Person person, Team team, TeamMemberRole role, LocalDate startDate, LocalDate endDate, boolean isActive) {
        TeamAssignment assignment = TeamAssignment.builder()
                .person(person)
                .team(team)
                .role(role)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(isActive)
                .build();
        teamAssignmentRepository.save(assignment);
    }

    private void createWorkLog(Person person, Pitch pitch, LocalDate date, BigDecimal hours, String note) {
        WorkLog workLog = WorkLog.builder()
                .person(person)
                .pitch(pitch)
                .date(date)
                .hoursSpent(hours)
                .note(note)
                .build();
        workLogRepository.save(workLog);
    }

    private void createTask(String title, String description, TaskStatus status, TaskPriority priority,
                           BigDecimal estimateHours, BigDecimal actualHours, Cycle cycle, Person assignee,
                           Person pairAssignee, Person createdBy, LocalDate dueDate, String tags) {
        Task task = Task.builder()
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
}
