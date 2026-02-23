package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO.FeedbackRating;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run before KnowledgeSeeder
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements CommandLineRunner {

  private final ProjectRepository projectRepository;
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
    Person frank = createPerson("Frank Miller", "frank.miller@example.com", "Architecture, DevOps, Leadership",
        null);
    Person grace = createPerson("Grace Lee", "grace.lee@example.com", "Python, Data Analysis, Machine Learning",
        null);
    Person henry = createPerson("Henry Wilson", "henry.wilson@example.com", "Android, Kotlin, Mobile Development",
        null);

    // Create users for sample persons
    createUser("alice", "password", UserRole.MEMBER, alice);
    createUser("bob", "password", UserRole.MEMBER, bob);
    createUser("carol", "password", UserRole.MEMBER, carol);
    createUser("dave", "password", UserRole.MEMBER, dave);
    createUser("eve", "password", UserRole.MEMBER, eve);
    createUser("frank", "password", UserRole.MANAGER, frank);
    createUser("grace", "password", UserRole.MEMBER, grace);
    createUser("henry", "password", UserRole.MEMBER, henry);

    // Create projects first
    User aliceUser = userRepository.findByUsername("alice").orElse(null);
    User frankUser = userRepository.findByUsername("frank").orElse(null);

    Project mainProject = Project.builder().name("ShipFlow").projectKey("SUT")
        .description("Main product development - Shape Up tracking and collaboration platform").color("#3B82F6")
        .owner(aliceUser).isActive(true).enableRetrospectives(true)
        .createdAt(LocalDateTime.now().minusMonths(12)).build();
    projectRepository.save(mainProject);

    Project internalToolsProject = Project.builder().name("Internal Tools").projectKey("IT")
        .description("Suite of internal tools for team productivity").color("#10B981").owner(aliceUser)
        .isActive(true).enableRetrospectives(true).createdAt(LocalDateTime.now().minusMonths(8)).build();
    projectRepository.save(internalToolsProject);

    Project mobileAppProject = Project.builder().name("Mobile App").projectKey("MA")
        .description("Native mobile application for iOS and Android").color("#8B5CF6").owner(frankUser)
        .isActive(true).enableRetrospectives(true).createdAt(LocalDateTime.now().minusMonths(6)).build();
    projectRepository.save(mobileAppProject);

    // Create active cycle for main project
    Cycle activeCycle = Cycle.builder().project(mainProject).name("Q1 2025 - Feature Sprint")
        .startDate(LocalDate.of(2025, 1, 6)).endDate(LocalDate.of(2025, 2, 14)).phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true).build();
    cycleRepository.save(activeCycle);

    // Create completed cycles for main project
    Cycle completedCycle1 = Cycle.builder().project(mainProject).name("Q4 2024 - Holiday Release")
        .startDate(LocalDate.of(2024, 11, 4)).endDate(LocalDate.of(2024, 12, 13)).phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false).build();
    cycleRepository.save(completedCycle1);

    Cycle completedCycle2 = Cycle.builder().project(mainProject).name("Q3 2024 - Summer Sprint")
        .startDate(LocalDate.of(2024, 8, 5)).endDate(LocalDate.of(2024, 9, 20)).phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false).build();
    cycleRepository.save(completedCycle2);

    Cycle completedCycle3 = Cycle.builder().project(mainProject).name("Q2 2024 - Mobile Expansion")
        .startDate(LocalDate.of(2024, 5, 6)).endDate(LocalDate.of(2024, 6, 21)).phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false).build();
    cycleRepository.save(completedCycle3);

    // Create cycles for Internal Tools project
    Cycle itCycle1 = Cycle.builder().project(internalToolsProject).name("IT Q1 2025 - Dashboard & Analytics")
        .startDate(LocalDate.of(2025, 1, 6)).endDate(LocalDate.of(2025, 2, 14)).phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true).build();
    cycleRepository.save(itCycle1);

    Cycle itCycle2 = Cycle.builder().project(internalToolsProject).name("IT Q4 2024 - Automation Tools")
        .startDate(LocalDate.of(2024, 11, 4)).endDate(LocalDate.of(2024, 12, 13)).phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false).build();
    cycleRepository.save(itCycle2);

    // Create cycles for Mobile App project
    Cycle maCycle1 = Cycle.builder().project(mobileAppProject).name("MA Q1 2025 - iOS & Android Launch")
        .startDate(LocalDate.of(2025, 1, 6)).endDate(LocalDate.of(2025, 2, 28)).phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true).build();
    cycleRepository.save(maCycle1);

    Cycle maCycle2 = Cycle.builder().project(mobileAppProject).name("MA Q4 2024 - Beta Testing")
        .startDate(LocalDate.of(2024, 10, 7)).endDate(LocalDate.of(2024, 12, 20)).phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false).build();
    cycleRepository.save(maCycle2);

    // Create Roadmap data - Initiatives, Epics, and Releases
    createRoadmapData(mainProject, internalToolsProject, mobileAppProject, aliceUser, frankUser,
        activeCycle, itCycle1, maCycle1);

    // Create teams for active cycle
    Team alphaTeam = Team.builder().name("Alpha Team").build();
    teamRepository.save(alphaTeam);

    Team betaTeam = Team.builder().name("Beta Team").build();
    teamRepository.save(betaTeam);

    // Create teams for past cycles
    Team pastTeam1 = Team.builder().name("Release Team").build();
    teamRepository.save(pastTeam1);

    Team pastTeam2 = Team.builder().name("Summer Squad").build();
    teamRepository.save(pastTeam2);

    Team pastTeam3 = Team.builder().name("Mobile Team").build();
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
    createAssignment(alice, pastTeam1, TeamMemberRole.TECH_LEAD, completedCycle1.getStartDate(),
        completedCycle1.getEndDate(), false);
    createAssignment(bob, pastTeam1, TeamMemberRole.BACKEND, completedCycle1.getStartDate(),
        completedCycle1.getEndDate(), false);
    createAssignment(grace, pastTeam1, TeamMemberRole.FULLSTACK, completedCycle1.getStartDate(),
        completedCycle1.getEndDate(), false);

    createAssignment(dave, pastTeam2, TeamMemberRole.FRONTEND, completedCycle2.getStartDate(),
        completedCycle2.getEndDate(), false);
    createAssignment(eve, pastTeam2, TeamMemberRole.DESIGNER, completedCycle2.getStartDate(),
        completedCycle2.getEndDate(), false);
    createAssignment(frank, pastTeam2, TeamMemberRole.TECH_LEAD, completedCycle2.getStartDate(),
        completedCycle2.getEndDate(), false);

    createAssignment(henry, pastTeam3, TeamMemberRole.FULLSTACK, completedCycle3.getStartDate(),
        completedCycle3.getEndDate(), false);
    createAssignment(grace, pastTeam3, TeamMemberRole.BACKEND, completedCycle3.getStartDate(),
        completedCycle3.getEndDate(), false);
    createAssignment(carol, pastTeam3, TeamMemberRole.QA, completedCycle3.getStartDate(),
        completedCycle3.getEndDate(), false);

    // Create pitches for active cycle with varied statuses
    Pitch userDashboard = Pitch.builder().title("User Dashboard Redesign")
        .description("Complete redesign of the user dashboard with new analytics widgets and improved UX")
        .appetiteDays(6).cycle(activeCycle).team(alphaTeam).status(PitchStatus.IN_PROGRESS)
        .createdAt(LocalDateTime.now().minusDays(10)).updatedAt(LocalDateTime.now()).build();
    pitchRepository.save(userDashboard);

    Pitch apiIntegration = Pitch.builder().title("Third-party API Integration")
        .description("Integrate with external payment provider API and implement webhook handlers")
        .appetiteDays(4).cycle(activeCycle).team(alphaTeam).status(PitchStatus.IN_PROGRESS)
        .createdAt(LocalDateTime.now().minusDays(5)).updatedAt(LocalDateTime.now()).build();
    pitchRepository.save(apiIntegration);

    Pitch mobileApp = Pitch.builder().title("Mobile App Notification System")
        .description("Push notification system for mobile app with customizable preferences").appetiteDays(5)
        .cycle(activeCycle).team(betaTeam).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(15))
        .updatedAt(LocalDateTime.now().minusDays(1)).build();
    pitchRepository.save(mobileApp);

    Pitch reportModule = Pitch.builder().title("Advanced Reporting Module")
        .description("New reporting module with PDF export and scheduled email delivery").appetiteDays(6)
        .cycle(activeCycle).team(betaTeam).status(PitchStatus.IN_PROGRESS)
        .createdAt(LocalDateTime.now().minusDays(8)).updatedAt(LocalDateTime.now()).build();
    pitchRepository.save(reportModule);

    Pitch searchFeature = Pitch.builder().title("Enhanced Search Functionality")
        .description("Add full-text search with filters and autocomplete").appetiteDays(4).cycle(activeCycle)
        .team(alphaTeam).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(12))
        .updatedAt(LocalDateTime.now().minusDays(2)).build();
    pitchRepository.save(searchFeature);

    // Create pitches for completed cycles with full work history
    Pitch darkMode = Pitch.builder().title("Dark Mode Support")
        .description("Implement dark mode across all UI components").appetiteDays(3).cycle(completedCycle1)
        .team(pastTeam1).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(60))
        .updatedAt(LocalDateTime.now().minusDays(45)).build();
    pitchRepository.save(darkMode);

    Pitch performanceOpt = Pitch.builder().title("Performance Optimization")
        .description("Database query optimization and caching implementation").appetiteDays(5)
        .cycle(completedCycle1).team(pastTeam1).status(PitchStatus.DONE)
        .createdAt(LocalDateTime.now().minusDays(58)).updatedAt(LocalDateTime.now().minusDays(43)).build();
    pitchRepository.save(performanceOpt);

    Pitch socialIntegration = Pitch.builder().title("Social Media Integration")
        .description("Share content to Twitter, LinkedIn, and Facebook").appetiteDays(4).cycle(completedCycle2)
        .team(pastTeam2).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(100))
        .updatedAt(LocalDateTime.now().minusDays(85)).build();
    pitchRepository.save(socialIntegration);

    Pitch dataExport = Pitch.builder().title("Data Export Feature")
        .description("Export user data in CSV and Excel formats").appetiteDays(3).cycle(completedCycle2)
        .team(pastTeam2).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(95))
        .updatedAt(LocalDateTime.now().minusDays(82)).build();
    pitchRepository.save(dataExport);

    Pitch mobileOnboarding = Pitch.builder().title("Mobile Onboarding Flow")
        .description("Improved first-time user experience on mobile").appetiteDays(5).cycle(completedCycle3)
        .team(pastTeam3).status(PitchStatus.DONE).createdAt(LocalDateTime.now().minusDays(140))
        .updatedAt(LocalDateTime.now().minusDays(125)).build();
    pitchRepository.save(mobileOnboarding);

    Pitch offlineMode = Pitch.builder().title("Offline Mode Support")
        .description("Enable app functionality without internet connection").appetiteDays(6)
        .cycle(completedCycle3).team(pastTeam3).status(PitchStatus.DONE)
        .createdAt(LocalDateTime.now().minusDays(138)).updatedAt(LocalDateTime.now().minusDays(123)).build();
    pitchRepository.save(offlineMode);

    // Create work logs for active cycle pitches
    createWorkLog(alice, userDashboard, LocalDate.now().minusDays(5), new BigDecimal("6.5"),
        "Designed new widget layout");
    createWorkLog(alice, userDashboard, LocalDate.now().minusDays(4), new BigDecimal("7.0"),
        "Implemented chart components");
    createWorkLog(alice, userDashboard, LocalDate.now().minusDays(3), new BigDecimal("5.5"),
        "Added drag-and-drop functionality");
    createWorkLog(alice, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("6.0"),
        "Fixed responsive layout issues");
    createWorkLog(alice, userDashboard, LocalDate.now().minusDays(1), new BigDecimal("5.0"),
        "Integrated with analytics API");
    createWorkLog(bob, userDashboard, LocalDate.now().minusDays(4), new BigDecimal("8.0"),
        "Built API endpoints for widgets");
    createWorkLog(bob, userDashboard, LocalDate.now().minusDays(3), new BigDecimal("6.0"),
        "Optimized database queries");
    createWorkLog(bob, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("7.0"), "Added caching layer");
    createWorkLog(carol, userDashboard, LocalDate.now().minusDays(2), new BigDecimal("4.0"), "Created test cases");
    createWorkLog(carol, userDashboard, LocalDate.now().minusDays(1), new BigDecimal("5.0"),
        "Performed integration testing");

    createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(4), new BigDecimal("7.5"),
        "Set up payment provider SDK");
    createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(3), new BigDecimal("6.0"),
        "Implemented webhook handlers");
    createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(2), new BigDecimal("6.5"),
        "Added error handling and retry logic");
    createWorkLog(bob, apiIntegration, LocalDate.now().minusDays(1), new BigDecimal("5.0"),
        "Writing integration tests");
    createWorkLog(alice, apiIntegration, LocalDate.now().minusDays(2), new BigDecimal("4.0"),
        "Built admin UI for payment settings");

    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(14), new BigDecimal("8.0"),
        "Set up notification service");
    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(13), new BigDecimal("7.0"),
        "Built notification preferences UI");
    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(12), new BigDecimal("6.5"),
        "Integrated with Firebase");
    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(11), new BigDecimal("6.0"),
        "Implemented push notification handlers");
    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(10), new BigDecimal("5.5"),
        "Added notification scheduling");
    createWorkLog(eve, mobileApp, LocalDate.now().minusDays(9), new BigDecimal("5.0"),
        "Designed notification templates");
    createWorkLog(eve, mobileApp, LocalDate.now().minusDays(8), new BigDecimal("4.5"),
        "Created user preference flows");
    createWorkLog(frank, mobileApp, LocalDate.now().minusDays(7), new BigDecimal("4.0"),
        "Code review and architecture refinement");
    createWorkLog(dave, mobileApp, LocalDate.now().minusDays(3), new BigDecimal("6.0"), "Bug fixes and polish");

    createWorkLog(frank, reportModule, LocalDate.now().minusDays(7), new BigDecimal("7.0"),
        "Designed report templates");
    createWorkLog(frank, reportModule, LocalDate.now().minusDays(6), new BigDecimal("6.5"),
        "Implemented PDF generation");
    createWorkLog(frank, reportModule, LocalDate.now().minusDays(5), new BigDecimal("7.0"),
        "Built report scheduler");
    createWorkLog(dave, reportModule, LocalDate.now().minusDays(4), new BigDecimal("5.5"),
        "Created report UI components");
    createWorkLog(dave, reportModule, LocalDate.now().minusDays(3), new BigDecimal("6.0"),
        "Added chart visualizations");
    createWorkLog(eve, reportModule, LocalDate.now().minusDays(2), new BigDecimal("4.0"),
        "Designed report layouts");

    createWorkLog(alice, searchFeature, LocalDate.now().minusDays(11), new BigDecimal("7.5"),
        "Integrated Elasticsearch");
    createWorkLog(alice, searchFeature, LocalDate.now().minusDays(10), new BigDecimal("6.0"),
        "Built search API endpoints");
    createWorkLog(alice, searchFeature, LocalDate.now().minusDays(9), new BigDecimal("6.5"),
        "Implemented autocomplete");
    createWorkLog(bob, searchFeature, LocalDate.now().minusDays(8), new BigDecimal("5.5"), "Added search filters");
    createWorkLog(bob, searchFeature, LocalDate.now().minusDays(7), new BigDecimal("6.0"),
        "Optimized search performance");
    createWorkLog(alice, searchFeature, LocalDate.now().minusDays(6), new BigDecimal("5.0"), "Built search UI");
    createWorkLog(carol, searchFeature, LocalDate.now().minusDays(3), new BigDecimal("4.5"), "QA testing");

    // Work logs for completed cycle 1 (Q4 2024)
    createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 5), new BigDecimal("7.0"),
        "Designed dark theme color palette");
    createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 6), new BigDecimal("8.0"), "Implemented theme switching");
    createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 7), new BigDecimal("6.5"), "Updated all components");
    createWorkLog(bob, darkMode, LocalDate.of(2024, 11, 8), new BigDecimal("5.0"), "Fixed theme persistence");
    createWorkLog(grace, darkMode, LocalDate.of(2024, 11, 11), new BigDecimal("4.5"), "Added user preferences");
    createWorkLog(alice, darkMode, LocalDate.of(2024, 11, 12), new BigDecimal("5.5"), "Final polish and testing");

    createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 6), new BigDecimal("8.0"), "Analyzed slow queries");
    createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 7), new BigDecimal("7.5"), "Added database indexes");
    createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 8), new BigDecimal("7.0"),
        "Implemented Redis caching");
    createWorkLog(grace, performanceOpt, LocalDate.of(2024, 11, 11), new BigDecimal("6.5"),
        "Optimized API endpoints");
    createWorkLog(bob, performanceOpt, LocalDate.of(2024, 11, 12), new BigDecimal("6.0"),
        "Added query result caching");
    createWorkLog(grace, performanceOpt, LocalDate.of(2024, 11, 13), new BigDecimal("5.5"),
        "Load testing and tuning");
    createWorkLog(alice, performanceOpt, LocalDate.of(2024, 11, 14), new BigDecimal("4.5"),
        "Performance monitoring setup");

    // Work logs for completed cycle 2 (Q3 2024)
    createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 6), new BigDecimal("7.0"),
        "Set up OAuth integrations");
    createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 7), new BigDecimal("6.5"),
        "Built Twitter integration");
    createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 8), new BigDecimal("6.5"),
        "Built LinkedIn integration");
    createWorkLog(eve, socialIntegration, LocalDate.of(2024, 8, 9), new BigDecimal("5.5"),
        "Designed share dialogs");
    createWorkLog(dave, socialIntegration, LocalDate.of(2024, 8, 12), new BigDecimal("6.0"),
        "Built Facebook integration");
    createWorkLog(frank, socialIntegration, LocalDate.of(2024, 8, 13), new BigDecimal("5.0"),
        "Added analytics tracking");

    createWorkLog(eve, dataExport, LocalDate.of(2024, 8, 14), new BigDecimal("6.5"), "Designed export UI");
    createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 15), new BigDecimal("7.0"), "Implemented CSV export");
    createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 16), new BigDecimal("6.5"), "Implemented Excel export");
    createWorkLog(dave, dataExport, LocalDate.of(2024, 8, 19), new BigDecimal("5.5"), "Added export filters");
    createWorkLog(frank, dataExport, LocalDate.of(2024, 8, 20), new BigDecimal("5.0"), "Performance optimization");

    // Work logs for completed cycle 3 (Q2 2024)
    createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 7), new BigDecimal("7.5"),
        "Designed onboarding flow");
    createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 8), new BigDecimal("7.0"),
        "Built welcome screens");
    createWorkLog(grace, mobileOnboarding, LocalDate.of(2024, 5, 9), new BigDecimal("6.5"),
        "Implemented tutorial steps");
    createWorkLog(henry, mobileOnboarding, LocalDate.of(2024, 5, 10), new BigDecimal("6.0"),
        "Added skip functionality");
    createWorkLog(grace, mobileOnboarding, LocalDate.of(2024, 5, 13), new BigDecimal("5.5"),
        "Built progress indicators");
    createWorkLog(carol, mobileOnboarding, LocalDate.of(2024, 5, 14), new BigDecimal("4.5"), "QA and testing");

    createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 15), new BigDecimal("8.0"),
        "Implemented local database");
    createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 16), new BigDecimal("7.5"), "Built sync mechanism");
    createWorkLog(grace, offlineMode, LocalDate.of(2024, 5, 17), new BigDecimal("7.0"),
        "Added conflict resolution");
    createWorkLog(henry, offlineMode, LocalDate.of(2024, 5, 20), new BigDecimal("6.5"),
        "Implemented offline queue");
    createWorkLog(grace, offlineMode, LocalDate.of(2024, 5, 21), new BigDecimal("6.0"), "Built background sync");
    createWorkLog(carol, offlineMode, LocalDate.of(2024, 5, 22), new BigDecimal("5.0"), "Offline mode testing");

    // Create meetings
    Meeting kickoff1 = Meeting.builder().pitch(userDashboard).type("KICKOFF")
        .dateHeld(LocalDate.now().minusDays(10)).dorReady(true).dodReady(false)
        .notes("Defined scope and milestones").build();
    meetingRepository.save(kickoff1);

    Meeting standup1 = Meeting.builder().pitch(userDashboard).type("STANDUP")
        .dateHeld(LocalDate.now().minusDays(3)).dorReady(true).dodReady(false)
        .notes("Progress update - 70% complete").build();
    meetingRepository.save(standup1);

    Meeting shaping = Meeting.builder().pitch(reportModule).type("SHAPING")
        .dateHeld(LocalDate.now().minusDays(5)).dorReady(false).dodReady(false)
        .notes("Identified key requirements and risks").build();
    meetingRepository.save(shaping);

    Meeting betting = Meeting.builder().pitch(reportModule).type("BETTING")
        .dateHeld(LocalDate.now().minusDays(3)).dorReady(true).dodReady(false)
        .notes("Approved for next build phase").build();
    meetingRepository.save(betting);

    Meeting demo = Meeting.builder().pitch(mobileApp).type("DEMO").dateHeld(LocalDate.now().minusDays(1))
        .dorReady(true).dodReady(true).notes("Successful demo to stakeholders").build();
    meetingRepository.save(demo);

    // Create evidences
    Evidence evidence1 = Evidence.builder().pitch(userDashboard).person(alice).date(LocalDate.now().minusDays(4))
        .description("Blocker: Third-party charting library has performance issues with large datasets")
        .fileUrl(null).build();
    evidenceRepository.save(evidence1);

    Evidence evidence2 = Evidence.builder().pitch(mobileApp).person(dave).date(LocalDate.now().minusDays(6))
        .description("QA passed - All notification flows working correctly on iOS and Android")
        .fileUrl("https://example.com/qa-report.pdf").build();
    evidenceRepository.save(evidence2);

    Evidence evidence3 = Evidence.builder().pitch(apiIntegration).person(bob).date(LocalDate.now().minusDays(1))
        .description("Waiting for API credentials from payment provider - ETA 2 days").fileUrl(null).build();
    evidenceRepository.save(evidence3);

    // Create sample tasks for active cycle
    createTask("Setup Redux store for dashboard",
        "Initialize Redux state management with slices for widgets and layout", TaskStatus.DONE,
        TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("3.5"), activeCycle, alice, null, alice,
        LocalDate.now().plusDays(5), "frontend,redux,setup");

    createTask("Implement widget drag-and-drop", "Add react-beautiful-dnd for dashboard widget rearrangement",
        TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("5.5"), activeCycle, alice,
        null, alice, LocalDate.now().plusDays(8), "frontend,ux,drag-drop");

    createTask("Create chart API endpoints", "Build REST endpoints for fetching chart data with time range filters",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("5.0"), new BigDecimal("3.0"), activeCycle,
        bob, null, bob, LocalDate.now().plusDays(3), "backend,api,charts");

    createTask("Optimize dashboard queries", "Add database indexes and implement query result caching",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("4.0"), new BigDecimal("2.5"), activeCycle,
        bob, null, bob, LocalDate.now().plusDays(4), "backend,performance,database");

    createTask("Write dashboard integration tests",
        "Create Cypress tests for dashboard functionality and interactions", TaskStatus.TODO,
        TaskPriority.MEDIUM, new BigDecimal("8.0"), null, activeCycle, carol, null, alice,
        LocalDate.now().plusDays(7), "testing,cypress,integration");

    createTask("Payment provider SDK integration", "Integrate Stripe SDK and configure API credentials",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("6.0"), new BigDecimal("4.0"), activeCycle,
        bob, null, bob, LocalDate.now().plusDays(2), "backend,payment,stripe");

    createTask("Webhook signature verification", "Implement secure webhook verification using provider signatures",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("3.0"), null, activeCycle, bob, alice, bob,
        LocalDate.now().plusDays(4), "backend,security,webhooks");

    createTask("Payment error handling", "Add comprehensive error handling and user-friendly messages",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("4.0"), null, activeCycle, bob, null, bob,
        LocalDate.now().plusDays(6), "backend,error-handling");

    createTask("Admin payment settings UI", "Create admin panel for configuring payment provider settings",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("3.0"), activeCycle,
        alice, null, bob, LocalDate.now().plusDays(5), "frontend,admin,settings");

    createTask("PDF report template design", "Create professional PDF templates for different report types",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("4.0"), activeCycle,
        eve, null, frank, LocalDate.now().plusDays(8), "design,pdf,templates");

    createTask("Report scheduler backend", "Build cron-based scheduler for automated report generation",
        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, new BigDecimal("7.0"), new BigDecimal("5.0"), activeCycle,
        frank, null, frank, LocalDate.now().plusDays(6), "backend,scheduler,cron");

    createTask("Email delivery service", "Implement email service for sending scheduled reports", TaskStatus.TODO,
        TaskPriority.MEDIUM, new BigDecimal("5.0"), null, activeCycle, frank, null, frank,
        LocalDate.now().plusDays(9), "backend,email,notifications");

    createTask("Report UI components", "Create React components for report builder and viewer",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("8.0"), new BigDecimal("6.0"), activeCycle,
        dave, null, frank, LocalDate.now().plusDays(7), "frontend,react,components");

    createTask("Chart visualization library", "Integrate Chart.js for interactive report charts", TaskStatus.TODO,
        TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, dave, null, frank,
        LocalDate.now().plusDays(10), "frontend,charts,visualization");

    createTask("Code review: Search feature", "Review and approve search implementation before merge",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("2.0"), new BigDecimal("1.5"), activeCycle, frank,
        null, alice, LocalDate.now().minusDays(3), "code-review,search");

    createTask("Deploy search to staging", "Deploy Elasticsearch and search API to staging environment",
        TaskStatus.DONE, TaskPriority.HIGH, new BigDecimal("3.0"), new BigDecimal("2.5"), activeCycle, bob,
        null, alice, LocalDate.now().minusDays(2), "devops,deployment,staging");

    createTask("Write API documentation", "Document all new API endpoints with examples", TaskStatus.TODO,
        TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, alice, bob, alice,
        LocalDate.now().plusDays(12), "documentation,api");

    createTask("Update user guide", "Add documentation for new dashboard features", TaskStatus.TODO,
        TaskPriority.LOW, new BigDecimal("3.0"), null, activeCycle, eve, null, alice,
        LocalDate.now().plusDays(14), "documentation,user-guide");

    createTask("Security audit: Payment flow", "Conduct security review of payment integration", TaskStatus.TODO,
        TaskPriority.HIGH, new BigDecimal("5.0"), null, activeCycle, frank, bob, frank,
        LocalDate.now().plusDays(8), "security,audit,payment");

    createTask("Performance monitoring setup", "Configure application performance monitoring tools",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("4.0"), new BigDecimal("2.0"), activeCycle,
        bob, null, frank, LocalDate.now().plusDays(10), "devops,monitoring,performance");

    createTask("Accessibility review", "Ensure dashboard meets WCAG 2.1 AA standards", TaskStatus.BACKLOG,
        TaskPriority.MEDIUM, new BigDecimal("6.0"), null, activeCycle, eve, alice, eve,
        LocalDate.now().plusDays(15), "accessibility,a11y,compliance");

    createTask("Mobile responsive fixes", "Fix responsive layout issues on tablet and mobile", TaskStatus.TODO,
        TaskPriority.MEDIUM, new BigDecimal("5.0"), null, activeCycle, dave, null, alice,
        LocalDate.now().plusDays(9), "frontend,responsive,mobile");

    createTask("Browser compatibility testing", "Test on Safari, Firefox, Chrome, and Edge", TaskStatus.BACKLOG,
        TaskPriority.LOW, new BigDecimal("4.0"), null, activeCycle, carol, null, alice,
        LocalDate.now().plusDays(16), "testing,browsers,qa");

    createTask("Database migration script", "Create migration for new dashboard schema changes", TaskStatus.DONE,
        TaskPriority.HIGH, new BigDecimal("2.0"), new BigDecimal("1.5"), activeCycle, bob, null, bob,
        LocalDate.now().minusDays(5), "database,migration");

    createTask("Implement notification preferences", "Add user preferences for notification channels and frequency",
        TaskStatus.DONE, TaskPriority.MEDIUM, new BigDecimal("5.0"), new BigDecimal("4.5"), activeCycle, dave,
        null, dave, LocalDate.now().minusDays(8), "frontend,notifications,preferences");

    // Create technical debt tasks
    createTask("Refactor authentication service",
        "Technical Debt: Auth service has grown too complex, needs splitting into separate concerns",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("8.0"), null, activeCycle, bob, null, frank,
        LocalDate.now().plusDays(20), "backend,technical-debt,refactoring");

    createTask("Update deprecated React lifecycle methods",
        "Technical Debt: Replace componentWillMount and componentWillReceiveProps with hooks",
        TaskStatus.IN_PROGRESS, TaskPriority.LOW, new BigDecimal("6.0"), new BigDecimal("2.0"), activeCycle,
        alice, null, alice, LocalDate.now().plusDays(15), "frontend,technical-debt,react");

    createTask("Add missing database indexes",
        "Technical Debt: Several query performance issues due to missing indexes on foreign keys",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("4.0"), null, activeCycle, bob, null, bob,
        LocalDate.now().plusDays(10), "backend,technical-debt,database,performance");

    createTask("Remove duplicate code in services",
        "Technical Debt: Pitch and Task services have duplicate validation logic", TaskStatus.BACKLOG,
        TaskPriority.LOW, new BigDecimal("5.0"), null, activeCycle, bob, null, frank,
        LocalDate.now().plusDays(25), "backend,technical-debt,code-quality");

    createTask("Upgrade to latest Spring Boot version",
        "Technical Debt: Running on Spring Boot 3.1, need to upgrade to 3.2 for security patches",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("10.0"), null, activeCycle, bob, alice, frank,
        LocalDate.now().plusDays(18), "backend,technical-debt,dependencies,security");

    createTask("Fix inconsistent error handling",
        "Technical Debt: API endpoints return different error formats, need standardization",
        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, new BigDecimal("6.0"), new BigDecimal("3.5"), activeCycle,
        bob, null, bob, LocalDate.now().plusDays(12), "backend,technical-debt,api,error-handling");

    createTask("Improve test coverage for utils",
        "Technical Debt: Utility classes have only 45% test coverage, target is 80%", TaskStatus.TODO,
        TaskPriority.LOW, new BigDecimal("8.0"), null, activeCycle, carol, null, carol,
        LocalDate.now().plusDays(22), "testing,technical-debt,coverage");

    createTask("Remove unused npm dependencies",
        "Technical Debt: Package.json has 15+ unused dependencies increasing bundle size", TaskStatus.BACKLOG,
        TaskPriority.LOW, new BigDecimal("2.0"), null, activeCycle, dave, null, alice,
        LocalDate.now().plusDays(30), "frontend,technical-debt,dependencies");

    createTask("Migrate to React Query",
        "Technical Debt: Replace custom data fetching hooks with React Query for better caching",
        TaskStatus.TODO, TaskPriority.MEDIUM, new BigDecimal("12.0"), null, activeCycle, alice, dave, alice,
        LocalDate.now().plusDays(16), "frontend,technical-debt,refactoring,data-fetching");

    createTask("Add API rate limiting", "Technical Debt: API endpoints lack rate limiting, vulnerable to abuse",
        TaskStatus.TODO, TaskPriority.HIGH, new BigDecimal("5.0"), null, activeCycle, bob, null, frank,
        LocalDate.now().plusDays(8), "backend,technical-debt,security,api");

    createTask("Consolidate CSS styles",
        "Technical Debt: Multiple CSS files with duplicate styles and unused rules", TaskStatus.BACKLOG,
        TaskPriority.LOW, new BigDecimal("7.0"), null, activeCycle, dave, null, eve,
        LocalDate.now().plusDays(28), "frontend,technical-debt,css,cleanup");

    // Create hill chart points for all active pitches
    createHillChartPoints(userDashboard, alice);
    createHillChartPoints(apiIntegration, bob);
    createHillChartPoints(mobileApp, dave);
    createHillChartPoints(reportModule, frank);
    createHillChartPoints(searchFeature, alice);

    // Create retrospectives for completed cycles
    createRetrospectives(completedCycle1, pastTeam1, alice, bob, grace);
    createRetrospectives(completedCycle2, pastTeam2, dave, eve, frank);

    // Create manual notes for active pitches
    createManualNotes(userDashboard, alice);
    createManualNotes(apiIntegration, bob);
    createManualNotes(reportModule, frank);

    // Create custom dashboards for users
    createCustomDashboards(alice, bob, frank);

    // Create user preferences
    createUserPreferences(alice, bob, carol, dave, eve, frank);

    // Create risk feedback for pitches
    createRiskFeedback(userDashboard, alice, bob);
    createRiskFeedback(apiIntegration, bob, alice);
    createRiskFeedback(reportModule, frank, dave);

    // Create dashboard notifications
    createDashboardNotifications(alice, bob, carol, dave, frank);

    // Create WISE Architecture advice history
    User bobUser = userRepository.findByUsername("bob").orElse(null);
    User adminUser = userRepository.findByUsername("admin").orElse(null);
    createWiseArchitectureHistory(aliceUser, bobUser, frankUser, adminUser, userDashboard, performanceOpt, apiIntegration, searchFeature, reportModule);

    log.info("Sample data initialized successfully!");
  }

  private void createUser(String username, String password, UserRole role, Person person) {
    if (!userRepository.existsByUsername(username)) {
      User user = User.builder().username(username).password(passwordEncoder.encode(password)).role(role)
          .person(person).isActive(true).build();
      userRepository.save(user);
    }
  }

  private Person createPerson(String name, String email, String skills, String avatarUrl) {
    Person person = Person.builder().name(name).email(email).skills(skills).avatarUrl(avatarUrl).isActive(true)
        .createdAt(LocalDateTime.now()).build();
    return personRepository.save(person);
  }

  private void createAssignment(Person person, Team team, TeamMemberRole role, LocalDate startDate,
      LocalDate endDate) {
    createAssignment(person, team, role, startDate, endDate, true);
  }

  private void createAssignment(Person person, Team team, TeamMemberRole role, LocalDate startDate, LocalDate endDate,
      boolean isActive) {
    TeamAssignment assignment = TeamAssignment.builder().person(person).team(team).role(role).startDate(startDate)
        .endDate(endDate).isActive(isActive).build();
    teamAssignmentRepository.save(assignment);
  }

  private void createWorkLog(Person person, Pitch pitch, LocalDate date, BigDecimal hours, String note) {
    WorkLog workLog = WorkLog.builder().person(person).pitch(pitch).date(date).hoursSpent(hours).note(note).build();
    workLogRepository.save(workLog);
  }

  private void createTask(String title, String description, TaskStatus status, TaskPriority priority,
      BigDecimal estimateHours, BigDecimal actualHours, Cycle cycle, Person assignee, Person pairAssignee,
      Person createdBy, LocalDate dueDate, String tags) {
    Task task = Task.builder().title(title).description(description).status(status).priority(priority)
        .estimateHours(estimateHours).actualHours(actualHours).cycle(cycle).assignee(assignee)
        .pairAssignee(pairAssignee).createdBy(createdBy).dueDate(dueDate).tags(tags).build();

    if (status == TaskStatus.DONE) {
      task.setCompletedAt(LocalDateTime.now().minusDays(1));
    }

    taskRepository.save(task);
  }

  private void createHillChartPoints(Pitch pitch, Person creator) {
    // Create 3-5 hill chart points per pitch to demonstrate scopes
    HillChartPoint point1 = HillChartPoint.builder().pitch(pitch).scope("Backend Development")
        .description("API endpoints and business logic for " + pitch.getTitle())
        .position(pitch.getStatus() == PitchStatus.DONE ? 95 : 75).build();
    hillChartPointRepository.save(point1);

    HillChartPoint point2 = HillChartPoint.builder().pitch(pitch).scope("Frontend Implementation")
        .description("UI components and user interactions for " + pitch.getTitle())
        .position(pitch.getStatus() == PitchStatus.DONE ? 90 : 60).build();
    hillChartPointRepository.save(point2);

    HillChartPoint point3 = HillChartPoint.builder().pitch(pitch).scope("Testing & QA")
        .description("Test coverage and quality assurance")
        .position(pitch.getStatus() == PitchStatus.DONE ? 100 : 35).build();
    hillChartPointRepository.save(point3);

    HillChartPoint point4 = HillChartPoint.builder().pitch(pitch).scope("Integration")
        .description("Third-party integrations and API connections")
        .position(pitch.getStatus() == PitchStatus.DONE ? 85 : 45).build();
    hillChartPointRepository.save(point4);
  }

  private void createRetrospectives(Cycle cycle, Team team, Person... participants) {
    // Note: Retrospective requires Project but we don't have that entity created
    // yet
    // Skip retrospective creation for now or create with minimal data
    // Commenting out to avoid compilation errors
    /*
     * Retrospective retro = Retrospective.builder() .cycle(cycle)
     * .title("Retrospective for " + cycle.getName())
     * .notes("Team retrospective discussion") .status(RetroStatus.COMPLETED)
     * .build(); retrospectiveRepository.save(retro);
     *
     * // Retro items would go here but retrospective creation is disabled
     */
  }

  private void createManualNotes(Pitch pitch, Person creator) {
    ManualNote note1 = ManualNote.builder().title("API Coordination")
        .content("Important: Need to coordinate with backend team on API contract changes").contextType("pitch")
        .contextId(pitch.getId()).pitchId(pitch.getId()).authorId(creator.getId()).includeInKnowledge(true)
        .build();
    manualNoteRepository.save(note1);

    ManualNote note2 = ManualNote.builder().title("Rate Limiting Risk")
        .content("Risk identified: Third-party service might have rate limiting issues under high load")
        .contextType("pitch").contextId(pitch.getId()).pitchId(pitch.getId()).authorId(creator.getId())
        .includeInKnowledge(true).build();
    manualNoteRepository.save(note2);

    ManualNote note3 = ManualNote.builder().title("Technology Decision")
        .content("Decision made: Use React Query for data fetching and caching").contextType("pitch")
        .contextId(pitch.getId()).pitchId(pitch.getId()).authorId(creator.getId()).includeInKnowledge(true)
        .build();
    manualNoteRepository.save(note3);
  }

  private void createCustomDashboards(Person... users) {
    for (Person user : users) {
      User userEntity = userRepository.findByPersonId(user.getId()).orElse(null);
      if (userEntity == null)
        continue;

      CustomDashboard dashboard = CustomDashboard.builder().user(userEntity).name(user.getName() + "'s Dashboard")
          .isDefault(true).build();
      customDashboardRepository.save(dashboard);
    }
  }

  private void createUserPreferences(Person... users) {
    for (Person user : users) {
      User userEntity = userRepository.findByPersonId(user.getId()).orElse(null);
      if (userEntity == null)
        continue;

      UserPreference pref = UserPreference.builder().user(userEntity).build();
      userPreferenceRepository.save(pref);
    }
  }

  private void createDashboardNotifications(Person... users) {
    for (int i = 0; i < users.length; i++) {
      User userEntity = userRepository.findByPersonId(users[i].getId()).orElse(null);
      if (userEntity == null)
        continue;

      // Unread notification
      DashboardNotification notif1 = DashboardNotification.builder().user(userEntity).type("TASK_ASSIGNED")
          .title("New Task Assigned").message("You have been assigned a new task: Review API documentation")
          .isRead(false).createdAt(LocalDateTime.now().minusHours(i + 1)).build();
      dashboardNotificationRepository.save(notif1);

      // Read notification
      DashboardNotification notif2 = DashboardNotification.builder().user(userEntity).type("PITCH_STATUS_CHANGED")
          .title("Pitch Status Updated")
          .message("Pitch 'User Dashboard Redesign' status changed to In Progress").isRead(true)
          .createdAt(LocalDateTime.now().minusDays(i + 1)).build();
      dashboardNotificationRepository.save(notif2);

      // Meeting notification
      if (i % 2 == 0) {
        DashboardNotification notif3 = DashboardNotification.builder().user(userEntity)
            .type("MEETING_SCHEDULED").title("Meeting Scheduled")
            .message("Stand-up meeting scheduled for tomorrow at 10:00 AM").isRead(false)
            .createdAt(LocalDateTime.now().minusHours(2)).build();
        dashboardNotificationRepository.save(notif3);
      }
    }
  }

  private void createRiskFeedback(Pitch pitch, Person... reviewers) {
    for (int i = 0; i < reviewers.length; i++) {
      User userEntity = userRepository.findByPersonId(reviewers[i].getId()).orElse(null);
      if (userEntity == null)
        continue;

      RiskFeedback feedback = RiskFeedback.builder().pitch(pitch).user(userEntity)
          .originalRiskScore(i == 0 ? 65 : 45)
          .rating(i == 0 ? FeedbackRating.ACCURATE : FeedbackRating.ACCURATE)
          .suggestedRiskScore(i == 0 ? 70 : null)
          .notes(i == 0
              ? "Timeline might be tight given the scope. Consider reducing features or extending deadline."
              : "Overall looking good. Main dependencies are identified and team has necessary skills.")
          .missedFactors(i == 0
              ? "Break down large features into smaller milestones for better tracking"
              : "Keep monitoring third-party API availability")
          .build();
      riskFeedbackRepository.save(feedback);
    }
  }

  private void createRoadmapData(Project mainProject, Project internalToolsProject, 
      Project mobileAppProject, User aliceUser, User frankUser,
      Cycle activeCycle, Cycle itCycle, Cycle maCycle) {
    log.info("Creating roadmap data (Initiatives, Epics, Releases)...");

    // === INITIATIVES FOR MAIN PROJECT (ShipFlow) ===
    Initiative coreProductInitiative = Initiative.builder()
        .name("Core Product Enhancement 2025")
        .description("Strategic initiative to enhance core features and improve user experience")
        .status(InitiativeStatus.IN_PROGRESS)
        .color("#3B82F6")
        .targetStartDate(LocalDate.of(2025, 1, 1))
        .targetEndDate(LocalDate.of(2025, 6, 30))
        .project(mainProject)
        .owner(aliceUser)
        .sortOrder(1)
        .build();
    initiativeRepository.save(coreProductInitiative);

    Initiative enterpriseInitiative = Initiative.builder()
        .name("Enterprise Features 2025")
        .description("Build enterprise-grade features for larger organizations")
        .status(InitiativeStatus.PLANNED)
        .color("#8B5CF6")
        .targetStartDate(LocalDate.of(2025, 4, 1))
        .targetEndDate(LocalDate.of(2025, 12, 31))
        .project(mainProject)
        .owner(frankUser)
        .sortOrder(2)
        .build();
    initiativeRepository.save(enterpriseInitiative);

    // === EPICS FOR CORE PRODUCT INITIATIVE ===
    Epic dashboardEpic = Epic.builder()
        .name("Dashboard Modernization")
        .description("Complete overhaul of the dashboard with real-time analytics and customizable widgets")
        .status(EpicStatus.IN_PROGRESS)
        .color("#10B981")
        .targetStartDate(LocalDate.of(2025, 1, 1))
        .targetEndDate(LocalDate.of(2025, 3, 31))
        .project(mainProject)
        .initiative(coreProductInitiative)
        .owner(aliceUser)
        .sortOrder(1)
        .build();
    epicRepository.save(dashboardEpic);

    Epic reportingEpic = Epic.builder()
        .name("Advanced Reporting")
        .description("Build comprehensive reporting system with export capabilities and scheduling")
        .status(EpicStatus.PLANNED)
        .color("#F59E0B")
        .targetStartDate(LocalDate.of(2025, 2, 1))
        .targetEndDate(LocalDate.of(2025, 5, 31))
        .project(mainProject)
        .initiative(coreProductInitiative)
        .owner(frankUser)
        .sortOrder(2)
        .build();
    epicRepository.save(reportingEpic);

    // === EPICS FOR ENTERPRISE INITIATIVE ===
    Epic ssoEpic = Epic.builder()
        .name("SSO & SAML Integration")
        .description("Enterprise single sign-on with SAML 2.0 and OAuth 2.0 support")
        .status(EpicStatus.DRAFT)
        .color("#EF4444")
        .targetStartDate(LocalDate.of(2025, 4, 1))
        .targetEndDate(LocalDate.of(2025, 6, 30))
        .project(mainProject)
        .initiative(enterpriseInitiative)
        .owner(frankUser)
        .sortOrder(1)
        .build();
    epicRepository.save(ssoEpic);

    Epic auditEpic = Epic.builder()
        .name("Audit Trail & Compliance")
        .description("Comprehensive audit logging for SOC 2 and GDPR compliance")
        .status(EpicStatus.DRAFT)
        .color("#6366F1")
        .targetStartDate(LocalDate.of(2025, 5, 1))
        .targetEndDate(LocalDate.of(2025, 8, 31))
        .project(mainProject)
        .initiative(enterpriseInitiative)
        .owner(aliceUser)
        .sortOrder(2)
        .build();
    epicRepository.save(auditEpic);

    // === STANDALONE EPIC (NOT LINKED TO INITIATIVE) ===
    Epic techDebtEpic = Epic.builder()
        .name("Technical Debt Reduction")
        .description("Address critical technical debt items and improve codebase maintainability")
        .status(EpicStatus.IN_PROGRESS)
        .color("#78716C")
        .targetStartDate(LocalDate.of(2025, 1, 15))
        .targetEndDate(LocalDate.of(2025, 12, 31))
        .project(mainProject)
        .initiative(null)  // Orphan epic
        .owner(aliceUser)
        .sortOrder(99)
        .build();
    epicRepository.save(techDebtEpic);

    // === INITIATIVE FOR MOBILE PROJECT ===
    Initiative mobileInitiative = Initiative.builder()
        .name("Mobile 2.0 Launch")
        .description("Launch the next generation mobile application with enhanced features")
        .status(InitiativeStatus.IN_PROGRESS)
        .color("#EC4899")
        .targetStartDate(LocalDate.of(2025, 1, 1))
        .targetEndDate(LocalDate.of(2025, 6, 30))
        .project(mobileAppProject)
        .owner(frankUser)
        .sortOrder(1)
        .build();
    initiativeRepository.save(mobileInitiative);

    Epic mobileUxEpic = Epic.builder()
        .name("Mobile UX Redesign")
        .description("Redesign mobile user experience based on user feedback")
        .status(EpicStatus.IN_PROGRESS)
        .color("#14B8A6")
        .targetStartDate(LocalDate.of(2025, 1, 6))
        .targetEndDate(LocalDate.of(2025, 3, 31))
        .project(mobileAppProject)
        .initiative(mobileInitiative)
        .owner(frankUser)
        .sortOrder(1)
        .build();
    epicRepository.save(mobileUxEpic);

    // === RELEASES ===
    Release q1Release = Release.builder()
        .name("Q1 2025 Release")
        .version("v2.5.0")
        .description("First major release of 2025 with dashboard improvements")
        .status(ReleaseStatus.IN_PROGRESS)
        .riskLevel(ReleaseRiskLevel.MEDIUM)
        .targetDate(LocalDate.of(2025, 3, 31))
        .releaseDate(null)
        .project(mainProject)
        .sortOrder(1)
        .build();
    q1Release.getCycles().add(activeCycle);
    releaseRepository.save(q1Release);

    Release q2Release = Release.builder()
        .name("Q2 2025 Release")
        .version("v2.6.0")
        .description("Enterprise features and reporting enhancements")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.LOW)
        .targetDate(LocalDate.of(2025, 6, 30))
        .releaseDate(null)
        .project(mainProject)
        .sortOrder(2)
        .build();
    releaseRepository.save(q2Release);

    Release mobileRelease = Release.builder()
        .name("Mobile 2.0 Launch")
        .version("v2.0.0")
        .description("Major mobile app launch with new features")
        .status(ReleaseStatus.IN_PROGRESS)
        .riskLevel(ReleaseRiskLevel.HIGH)
        .targetDate(LocalDate.of(2025, 2, 28))
        .releaseDate(null)
        .project(mobileAppProject)
        .sortOrder(1)
        .build();
    mobileRelease.getCycles().add(maCycle);
    releaseRepository.save(mobileRelease);

    Release itRelease = Release.builder()
        .name("Internal Tools v1.2")
        .version("v1.2.0")
        .description("Dashboard and analytics improvements")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.LOW)
        .targetDate(LocalDate.of(2025, 2, 14))
        .releaseDate(null)
        .project(internalToolsProject)
        .sortOrder(1)
        .build();
    itRelease.getCycles().add(itCycle);
    releaseRepository.save(itRelease);

    log.info("Roadmap data created: 4 initiatives, 6 epics, 4 releases");
  }

  private void createWiseArchitectureHistory(User aliceUser, User bobUser, User frankUser, User adminUser,
      Pitch userDashboard, Pitch performanceOpt, Pitch apiIntegration, Pitch searchFeature, Pitch reportModule) {

    // ---- Conversation 1: Alice on User Dashboard Redesign ----
    String conv1Id = "conv-wise-001";
    LocalDateTime conv1Start = LocalDateTime.now().minusDays(5);

    WiseArchitectureAdvice advice1Init = WiseArchitectureAdvice.builder()
        .conversationId(conv1Id)
        .pitch(userDashboard)
        .userId(aliceUser.getId())
        .messageType("INITIAL_SOLUTION")
        .userMessage("Initial solution request")
        .aiResponse("## Frontend React\n\n" +
            "A component-driven dashboard redesign using React 18 with server components.\n\n" +
            "### Key Components\n- **DashboardGrid**: Responsive grid layout using CSS Grid\n" +
            "- **WidgetContainer**: Draggable widget wrapper with react-beautiful-dnd\n" +
            "- **AnalyticsChart**: Chart.js integration for metrics visualisation\n\n" +
            "### Implementation Steps\n1. **Setup layout system** (~4h)\n   Implement CSS Grid-based responsive layout\n" +
            "2. **Build widget components** (~8h)\n   Create reusable widget shell components\n" +
            "3. **Add drag-and-drop** (~5h)\n   Integrate react-beautiful-dnd for customisation\n" +
            "4. **Connect analytics API** (~4h)\n   Wire up Chart.js to backend endpoints\n\n" +
            "### Recommended Libraries\n- **react-beautiful-dnd** v13 — Drag-and-drop\n" +
            "- **chart.js** v4 — Charting library\n- **@tanstack/react-query** v5 — Data fetching")
        .techStacks("FRONTEND_REACT")
        .hasFigmaContext(true)
        .hasGitHubContext(true)
        .hasRoadmapContext(false)
        .processingTimeMs(4823L)
        .feedbackHelpful(true)
        .feedbackText("Very helpful! The drag-and-drop suggestion saved us a lot of time.")
        .feedbackAt(conv1Start.plusHours(2))
        .createdAt(conv1Start)
        .updatedAt(conv1Start.plusHours(2))
        .build();
    wiseArchitectureAdviceRepository.save(advice1Init);

    WiseArchitectureAdvice advice1FollowUpQ = WiseArchitectureAdvice.builder()
        .conversationId(conv1Id)
        .pitch(userDashboard)
        .userId(aliceUser.getId())
        .messageType("FOLLOW_UP_QUESTION")
        .userMessage("How should we handle real-time updates for chart widgets without overloading the server?")
        .aiResponse(null)
        .techStacks("FRONTEND_REACT")
        .hasFigmaContext(false)
        .hasGitHubContext(false)
        .hasRoadmapContext(false)
        .processingTimeMs(null)
        .createdAt(conv1Start.plusDays(1))
        .updatedAt(conv1Start.plusDays(1))
        .build();
    wiseArchitectureAdviceRepository.save(advice1FollowUpQ);

    WiseArchitectureAdvice advice1FollowUpA = WiseArchitectureAdvice.builder()
        .conversationId(conv1Id)
        .pitch(userDashboard)
        .userId(aliceUser.getId())
        .messageType("FOLLOW_UP_ANSWER")
        .userMessage("How should we handle real-time updates for chart widgets without overloading the server?")
        .aiResponse("For real-time chart updates without server overload, use a **WebSocket with SSE fallback** approach:\n\n" +
            "1. **Server-Sent Events (SSE)** for read-only dashboards — lightweight, auto-reconnect, works with HTTP/2\n" +
            "2. **Debounced polling** (30–60s) for less critical metrics\n" +
            "3. **Optimistic UI updates** — update locally first, sync in background\n\n" +
            "In Spring Boot use `SseEmitter`. On the frontend use the browser `EventSource` API. " +
            "This keeps connections manageable and avoids overwhelming the server during peak usage.")
        .techStacks("FRONTEND_REACT")
        .hasFigmaContext(false)
        .hasGitHubContext(false)
        .hasRoadmapContext(false)
        .processingTimeMs(3102L)
        .createdAt(conv1Start.plusDays(1).plusMinutes(2))
        .updatedAt(conv1Start.plusDays(1).plusMinutes(2))
        .build();
    wiseArchitectureAdviceRepository.save(advice1FollowUpA);

    // ---- Conversation 2: Bob on Performance Optimization ----
    String conv2Id = "conv-wise-002";
    LocalDateTime conv2Start = LocalDateTime.now().minusDays(3);

    WiseArchitectureAdvice advice2Init = WiseArchitectureAdvice.builder()
        .conversationId(conv2Id)
        .pitch(performanceOpt)
        .userId(bobUser.getId())
        .messageType("INITIAL_SOLUTION")
        .userMessage("Initial solution request")
        .aiResponse("## Backend Spring\n\n" +
            "A multi-layer performance strategy for the Spring Boot backend.\n\n" +
            "### Key Areas\n- **Database**: Add composite indexes, rewrite N+1 queries with JOIN FETCH\n" +
            "- **Caching**: Redis L2 cache for read-heavy endpoints (TTL 5 min)\n" +
            "- **Connection Pooling**: Tune HikariCP pool size to match DB capacity\n\n" +
            "### Implementation Steps\n1. **Profiling baseline** (~2h)\n   Use Spring Boot Actuator + Micrometer to capture slow endpoints\n" +
            "2. **Database indexes** (~4h)\n   Analyse slow_query_log and add missing indexes on FK columns\n" +
            "3. **Redis caching** (~6h)\n   Add @Cacheable to frequently called service methods\n" +
            "4. **Connection pool tuning** (~2h)\n   Configure HikariCP max-pool-size based on load tests\n\n" +
            "### Recommended Libraries\n- **spring-boot-starter-data-redis** — Redis cache integration\n" +
            "- **micrometer-registry-prometheus** — Metrics export\n- **p6spy** — SQL query logging for profiling")
        .techStacks("BACKEND_SPRING")
        .hasFigmaContext(false)
        .hasGitHubContext(true)
        .hasRoadmapContext(true)
        .processingTimeMs(5241L)
        .createdAt(conv2Start)
        .updatedAt(conv2Start)
        .build();
    wiseArchitectureAdviceRepository.save(advice2Init);

    WiseArchitectureAdvice advice2FollowUpQ = WiseArchitectureAdvice.builder()
        .conversationId(conv2Id)
        .pitch(performanceOpt)
        .userId(bobUser.getId())
        .messageType("FOLLOW_UP_QUESTION")
        .userMessage("What is the best cache eviction strategy when the underlying data changes frequently?")
        .aiResponse(null)
        .techStacks("BACKEND_SPRING")
        .hasFigmaContext(false)
        .hasGitHubContext(false)
        .hasRoadmapContext(false)
        .processingTimeMs(null)
        .createdAt(conv2Start.plusDays(1))
        .updatedAt(conv2Start.plusDays(1))
        .build();
    wiseArchitectureAdviceRepository.save(advice2FollowUpQ);

    WiseArchitectureAdvice advice2FollowUpA = WiseArchitectureAdvice.builder()
        .conversationId(conv2Id)
        .pitch(performanceOpt)
        .userId(bobUser.getId())
        .messageType("FOLLOW_UP_ANSWER")
        .userMessage("What is the best cache eviction strategy when the underlying data changes frequently?")
        .aiResponse("For frequently changing data, combine **Cache-Aside with event-driven eviction**:\n\n" +
            "- **@CacheEvict on write operations** — immediately invalidate stale entries when data is mutated\n" +
            "- **Short TTL (30–120s)** — safety net for missed evictions\n" +
            "- **@CachePut for hot-write paths** — update cache on write instead of evicting (write-through)\n\n" +
            "If multiple services write to the same data, use **Redis Pub/Sub** to broadcast invalidation events " +
            "so all instances evict consistently. Avoid long TTLs on mutable data — prefer accuracy over cache hit rate.")
        .techStacks("BACKEND_SPRING")
        .hasFigmaContext(false)
        .hasGitHubContext(false)
        .hasRoadmapContext(false)
        .processingTimeMs(2867L)
        .createdAt(conv2Start.plusDays(1).plusMinutes(3))
        .updatedAt(conv2Start.plusDays(1).plusMinutes(3))
        .build();
    wiseArchitectureAdviceRepository.save(advice2FollowUpA);

    // ---- Conversation 3: Frank on Third-party API Integration ----
    String conv3Id = "conv-wise-003";
    LocalDateTime conv3Start = LocalDateTime.now().minusDays(7);

    WiseArchitectureAdvice advice3Init = WiseArchitectureAdvice.builder()
        .conversationId(conv3Id)
        .pitch(apiIntegration)
        .userId(frankUser.getId())
        .messageType("INITIAL_SOLUTION")
        .userMessage("Initial solution request")
        .aiResponse("## Backend Spring\n\n" +
            "Resilient third-party payment API integration using a hexagonal architecture adapter pattern.\n\n" +
            "### Architecture\n- **PaymentPort** interface — decouples business logic from provider details\n" +
            "- **StripeAdapter** — concrete implementation encapsulating SDK calls\n" +
            "- **OutboxPattern** — reliable event delivery even if provider is temporarily unavailable\n\n" +
            "### Implementation Steps\n1. **Define port interface** (~2h)\n   Create `PaymentPort` with `charge()`, `refund()`, `getStatus()` methods\n" +
            "2. **Implement Stripe adapter** (~6h)\n   Wrap Stripe Java SDK with proper error mapping\n" +
            "3. **Webhook verification** (~3h)\n   Validate Stripe-Signature header with HMAC-SHA256\n" +
            "4. **Outbox pattern** (~5h)\n   Persist payment events to outbox table, replay on recovery\n" +
            "5. **Retry with exponential backoff** (~2h)\n   Use Resilience4j Retry for transient failures\n\n" +
            "### Recommended Libraries\n- **stripe-java** v24 — Official Stripe SDK\n" +
            "- **resilience4j-spring-boot3** — Circuit breaker and retry\n- **spring-retry** — Declarative retry support")
        .techStacks("BACKEND_SPRING")
        .hasFigmaContext(false)
        .hasGitHubContext(true)
        .hasRoadmapContext(false)
        .processingTimeMs(6103L)
        .feedbackHelpful(false)
        .feedbackText("The Outbox pattern seems overkill for our scale. Would have preferred a simpler approach first.")
        .feedbackAt(conv3Start.plusHours(5))
        .createdAt(conv3Start)
        .updatedAt(conv3Start.plusHours(5))
        .build();
    wiseArchitectureAdviceRepository.save(advice3Init);

    // ---- Conversation 4: Admin on Enhanced Search Functionality ----
    if (adminUser != null) {
      String conv4Id = "conv-wise-004";
      LocalDateTime conv4Start = LocalDateTime.now().minusDays(2);

      WiseArchitectureAdvice advice4Init = WiseArchitectureAdvice.builder()
          .conversationId(conv4Id)
          .pitch(searchFeature)
          .userId(adminUser.getId())
          .messageType("INITIAL_SOLUTION")
          .userMessage("Initial solution request")
          .aiResponse("## Backend Spring\n\n" +
              "Full-text search architecture using Elasticsearch with a Spring Data integration layer.\n\n" +
              "### Architecture\n- **SearchService** — abstracts Elasticsearch queries behind a clean API\n" +
              "- **IndexingPipeline** — event-driven index updates via Spring ApplicationEvents\n" +
              "- **QueryBuilder** — composable filter/sort/pagination DSL\n\n" +
              "### Implementation Steps\n1. **Elasticsearch setup** (~3h)\n   Configure `spring-boot-starter-data-elasticsearch`, define index mappings\n" +
              "2. **Entity indexing** (~4h)\n   Map Pitch and Task entities to Elasticsearch documents\n" +
              "3. **Search API endpoint** (~3h)\n   Build `/api/search?q=...&type=...&page=...` with relevance scoring\n" +
              "4. **Autocomplete** (~2h)\n   Use Elasticsearch completion suggester for prefix matching\n" +
              "5. **Sync strategy** (~3h)\n   Dual-write on save + nightly full reindex job for consistency\n\n" +
              "### Recommended Libraries\n- **spring-data-elasticsearch** — Repository abstraction\n" +
              "- **co.elastic.clients:elasticsearch-java** v8 — Official Java client\n" +
              "- **spring-batch** — Scheduled bulk reindex jobs")
          .techStacks("BACKEND_SPRING")
          .hasFigmaContext(false)
          .hasGitHubContext(true)
          .hasRoadmapContext(true)
          .processingTimeMs(5820L)
          .createdAt(conv4Start)
          .updatedAt(conv4Start)
          .build();
      wiseArchitectureAdviceRepository.save(advice4Init);

      WiseArchitectureAdvice advice4FollowUpQ = WiseArchitectureAdvice.builder()
          .conversationId(conv4Id)
          .pitch(searchFeature)
          .userId(adminUser.getId())
          .messageType("FOLLOW_UP_QUESTION")
          .userMessage("How do we keep the Elasticsearch index in sync when records are updated or deleted?")
          .aiResponse(null)
          .techStacks("BACKEND_SPRING")
          .hasFigmaContext(false)
          .hasGitHubContext(false)
          .hasRoadmapContext(false)
          .processingTimeMs(null)
          .createdAt(conv4Start.plusHours(3))
          .updatedAt(conv4Start.plusHours(3))
          .build();
      wiseArchitectureAdviceRepository.save(advice4FollowUpQ);

      WiseArchitectureAdvice advice4FollowUpA = WiseArchitectureAdvice.builder()
          .conversationId(conv4Id)
          .pitch(searchFeature)
          .userId(adminUser.getId())
          .messageType("FOLLOW_UP_ANSWER")
          .userMessage("How do we keep the Elasticsearch index in sync when records are updated or deleted?")
          .aiResponse("Use a **CDC (Change Data Capture) + outbox** approach for reliable sync:\n\n" +
              "1. **@EntityListeners** — hook JPA `@PostPersist`, `@PostUpdate`, `@PostRemove` to publish `EntityChangedEvent`\n" +
              "2. **Async listener** — `@EventListener(condition = ...)` with `@Async` applies the change to Elasticsearch\n" +
              "3. **Soft-delete guard** — never hard-delete from the index; mark as `status=DELETED` and filter in queries\n" +
              "4. **Nightly reconciliation job** — Spring Batch job compares DB checksums with ES docs and re-indexes diverged records\n\n" +
              "This gives near-real-time sync for normal operations and safety-net consistency for edge cases.")
          .techStacks("BACKEND_SPRING")
          .hasFigmaContext(false)
          .hasGitHubContext(false)
          .hasRoadmapContext(false)
          .processingTimeMs(3418L)
          .feedbackHelpful(true)
          .feedbackText("Perfect, the outbox approach is exactly what we needed.")
          .feedbackAt(conv4Start.plusHours(4))
          .createdAt(conv4Start.plusHours(3).plusMinutes(2))
          .updatedAt(conv4Start.plusHours(4))
          .build();
      wiseArchitectureAdviceRepository.save(advice4FollowUpA);

      // ---- Conversation 5: Admin on Advanced Reporting Module ----
      String conv5Id = "conv-wise-005";
      LocalDateTime conv5Start = LocalDateTime.now().minusDays(1);

      WiseArchitectureAdvice advice5Init = WiseArchitectureAdvice.builder()
          .conversationId(conv5Id)
          .pitch(reportModule)
          .userId(adminUser.getId())
          .messageType("INITIAL_SOLUTION")
          .userMessage("Initial solution request")
          .aiResponse("## Backend Spring\n\n" +
              "A scalable reporting engine using async PDF generation and scheduled delivery.\n\n" +
              "### Architecture\n- **ReportDefinition** entity — stores template config (fields, filters, schedule)\n" +
              "- **ReportGenerationService** — async job triggered via `@Async` or Spring Batch\n" +
              "- **PdfRenderEngine** — JasperReports or iText for PDF output\n" +
              "- **SchedulerService** — `@Scheduled` cron-based report dispatch\n\n" +
              "### Implementation Steps\n1. **Report definition schema** (~3h)\n   Design flexible JSON-configurable report template model\n" +
              "2. **PDF generation** (~5h)\n   Integrate JasperReports, build template for cycle summaries\n" +
              "3. **Async execution** (~3h)\n   Run generation in background thread pool, stream result to S3\n" +
              "4. **Email dispatch** (~3h)\n   Send finished reports via Spring Mail\n" +
              "5. **Scheduler** (~2h)\n   Cron-based trigger with per-report configurable schedule\n\n" +
              "### Recommended Libraries\n- **jasperreports** v6 — PDF/XLSX report templates\n" +
              "- **spring-boot-starter-mail** — Email delivery\n" +
              "- **spring-batch** — Batch report generation pipeline")
          .techStacks("BACKEND_SPRING")
          .hasFigmaContext(false)
          .hasGitHubContext(false)
          .hasRoadmapContext(true)
          .processingTimeMs(4990L)
          .createdAt(conv5Start)
          .updatedAt(conv5Start)
          .build();
      wiseArchitectureAdviceRepository.save(advice5Init);
    }

    log.info("WISE Architecture advice history created: 5 conversations, 12 entries");
  }
}
