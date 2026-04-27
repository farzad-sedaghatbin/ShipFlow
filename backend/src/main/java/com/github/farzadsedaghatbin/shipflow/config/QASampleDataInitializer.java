package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds sample QA data for demonstration purposes. Runs after
 * SampleDataInitializer (Order 1.5) to ensure base data exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after SampleDataInitializer but before KnowledgeSeeder
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class QASampleDataInitializer implements CommandLineRunner {

  private final TestCaseRepository testCaseRepository;
  private final BugReportRepository bugReportRepository;
  private final TestRunRepository testRunRepository;
  private final PitchRepository pitchRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void run(String... args) {
    if (testCaseRepository.count() > 0) {
      log.info("QA sample data already exists, skipping initialization");
      return;
    }

    log.info("Initializing QA sample data...");

    // Get existing entities from SampleDataInitializer
    List<Pitch> pitches = pitchRepository.findAll();
    if (pitches.isEmpty()) {
      log.warn("No pitches found. Run SampleDataInitializer first.");
      return;
    }

    User qaUser = userRepository.findByUsername("carol").orElseGet(() -> userRepository.findAll().stream()
        .filter(u -> u.getRole() == UserRole.MEMBER).findFirst().orElse(null));

    if (qaUser == null) {
      log.warn("No QA user found. Skipping QA sample data.");
      return;
    }

    // Create test cases for the first few pitches
    for (Pitch pitch : pitches.subList(0, Math.min(3, pitches.size()))) {
      createTestCasesForPitch(pitch, qaUser);
    }

    // Create bug reports
    Pitch firstPitch = pitches.get(0);
    createBugReports(firstPitch, qaUser);

    log.info("QA sample data initialized successfully!");
  }

  private void createTestCasesForPitch(Pitch pitch, User qaUser) {
    Cycle cycle = pitch.getCycle();
    Team team = pitch.getTeam();

    // Test Case 1 - Functional test
    TestCase tc1 = testCaseRepository.save(TestCase.builder().testCaseKey(generateTestCaseKey(pitch.getId(), 1))
        .title("Verify " + pitch.getTitle() + " - Happy Path")
        .description("Test the main functionality of " + pitch.getTitle())
        .preconditions("User is logged in\nFeature is enabled")
        .steps("1. Navigate to the feature\n2. Perform main action\n3. Verify expected result")
        .expectedResult("Feature works as expected without errors").type(TestCaseType.FUNCTIONAL)
        .priority(TestCasePriority.HIGH).status(TestCaseStatus.APPROVED).pitch(pitch).cycle(cycle).team(team)
        .createdBy(qaUser).aiGenerated(false).build());

    // Test Case 2 - Edge case
    TestCase tc2 = testCaseRepository.save(TestCase.builder().testCaseKey(generateTestCaseKey(pitch.getId(), 2))
        .title("Verify " + pitch.getTitle() + " - Edge Cases")
        .description("Test edge cases and boundary conditions").preconditions("Test environment is set up")
        .steps("1. Test with empty input\n2. Test with maximum values\n3. Test with special characters")
        .expectedResult("System handles edge cases gracefully").type(TestCaseType.FUNCTIONAL)
        .priority(TestCasePriority.MEDIUM).status(TestCaseStatus.READY).pitch(pitch).cycle(cycle).team(team)
        .createdBy(qaUser).aiGenerated(false).build());

    // Test Case 3 - Integration test
    TestCase tc3 = testCaseRepository.save(TestCase.builder().testCaseKey(generateTestCaseKey(pitch.getId(), 3))
        .title("Verify " + pitch.getTitle() + " - API Integration")
        .description("Test API integration and data flow").preconditions("API endpoints are available")
        .steps("1. Call API endpoint\n2. Verify response format\n3. Verify data persistence")
        .expectedResult("API returns correct data and persists changes").type(TestCaseType.INTEGRATION)
        .priority(TestCasePriority.HIGH).status(TestCaseStatus.APPROVED).pitch(pitch).cycle(cycle).team(team)
        .createdBy(qaUser).aiGenerated(false).build());

    // Test Case 4 - AI Generated
    testCaseRepository.save(TestCase.builder().testCaseKey(generateTestCaseKey(pitch.getId(), 4))
        .title("Verify " + pitch.getTitle() + " - Performance under load")
        .description("AI-generated test case for performance validation")
        .preconditions("Load testing tools configured")
        .steps("1. Simulate concurrent users\n2. Monitor response times\n3. Check resource usage")
        .expectedResult("Response time < 2s under normal load").type(TestCaseType.REGRESSION)
        .priority(TestCasePriority.MEDIUM).status(TestCaseStatus.DRAFT).pitch(pitch).cycle(cycle).team(team)
        .createdBy(qaUser).aiGenerated(true).build());

    // Create test runs for some test cases
    createTestRuns(tc1, qaUser, Arrays.asList(TestRunStatus.PASSED, TestRunStatus.PASSED));
    createTestRuns(tc2, qaUser, Arrays.asList(TestRunStatus.PASSED, TestRunStatus.FAILED, TestRunStatus.PASSED));
    createTestRuns(tc3, qaUser, Arrays.asList(TestRunStatus.PASSED));
  }

  private void createTestRuns(TestCase testCase, User executedBy, List<TestRunStatus> statuses) {
    int daysAgo = statuses.size();
    for (TestRunStatus status : statuses) {
      testRunRepository.save(TestRun.builder().testCase(testCase).status(status).executedBy(executedBy)
          .executedAt(LocalDateTime.now().minusDays(daysAgo--))
          .durationSeconds((int) (Math.random() * 300 + 10))
          .notes(status == TestRunStatus.PASSED ? "Test passed successfully" : "Test failed - see bug report")
          .build());
    }
  }

  private void createBugReports(Pitch pitch, User reporter) {
    Cycle cycle = pitch.getCycle();
    Team team = pitch.getTeam();

    // Bug 1 - Critical
    bugReportRepository.save(BugReport.builder().bugKey(generateBugKey(pitch.getId(), 1))
        .title("Application crashes when submitting empty form")
        .description(
            "The application throws an unhandled exception when user submits the form without filling required fields")
        .stepsToReproduce(
            "1. Navigate to the form\n2. Leave all fields empty\n3. Click Submit\n4. Application crashes")
        .expectedBehavior("Form validation should display error messages")
        .actualBehavior("Application crashes with NullPointerException").severity(BugSeverity.CRITICAL)
        .status(BugStatus.IN_PROGRESS).environment("Production, Chrome 120.0").tags("crash,validation,form")
        .pitch(pitch).cycle(cycle).team(team).reporter(reporter).build());

    // Bug 2 - Major
    bugReportRepository.save(BugReport.builder().bugKey(generateBugKey(pitch.getId(), 2))
        .title("Data not persisting after page refresh")
        .description("Changes made by user are lost when the page is refreshed")
        .stepsToReproduce("1. Make changes to data\n2. Refresh the page\n3. Changes are gone")
        .expectedBehavior("Data should be persisted and available after refresh")
        .actualBehavior("Data reverts to previous state").severity(BugSeverity.MAJOR).status(BugStatus.OPEN)
        .environment("Staging, Firefox 121.0").tags("data-loss,persistence").pitch(pitch).cycle(cycle)
        .team(team).reporter(reporter).build());

    // Bug 3 - Minor - Resolved
    bugReportRepository.save(BugReport.builder().bugKey(generateBugKey(pitch.getId(), 3))
        .title("Typo in error message").description("Error message contains spelling mistake")
        .stepsToReproduce("1. Trigger error condition\n2. Observe error message")
        .expectedBehavior("'An error occurred' message").actualBehavior("'An errror occured' message")
        .severity(BugSeverity.MINOR).status(BugStatus.RESOLVED).environment("All").tags("typo,ui").pitch(pitch)
        .cycle(cycle).team(team).reporter(reporter).resolvedAt(LocalDateTime.now().minusDays(1)).build());

    // Bug 4 - Blocker
    bugReportRepository.save(BugReport.builder().bugKey(generateBugKey(pitch.getId(), 4))
        .title("Cannot login with valid credentials")
        .description("Users are unable to login even with correct username and password")
        .stepsToReproduce("1. Go to login page\n2. Enter valid credentials\n3. Click Login\n4. Login fails")
        .expectedBehavior("User should be logged in and redirected to dashboard")
        .actualBehavior("Error: Invalid credentials (even though credentials are correct)")
        .severity(BugSeverity.BLOCKER).status(BugStatus.OPEN).environment("Production, Safari 17.2")
        .tags("login,authentication,blocker").pitch(pitch).cycle(cycle).team(team).reporter(reporter).build());
  }

  private String generateTestCaseKey(Long pitchId, int sequence) {
    return String.format("TC-%03d", (pitchId * 10) + sequence);
  }

  private String generateBugKey(Long pitchId, int sequence) {
    return String.format("BUG-%03d", (pitchId * 10) + sequence);
  }
}
