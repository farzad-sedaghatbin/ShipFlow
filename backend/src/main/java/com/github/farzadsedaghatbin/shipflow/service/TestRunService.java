package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing test runs. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestRunService {

  private final TestRunRepository testRunRepository;
  private final TestCaseRepository testCaseRepository;
  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final UserRepository userRepository;
  private final BugReportRepository bugReportRepository;
  private final LocalizationService localizationService;

  @Value("${app.qa.test-management.enabled:true}")
  private boolean testManagementEnabled;

  /** Create a new test run. */
  @Transactional
  public TestRunDTO createTestRun(CreateTestRunRequest request, Long userId) {
    checkFeatureEnabled();

    User executor =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    TestCase testCase =
        testCaseRepository
            .findById(request.getTestCaseId())
            .orElseThrow(
                () -> new RuntimeException("Test case not found: " + request.getTestCaseId()));

    TestRun testRun =
        TestRun.builder()
            .testCase(testCase)
            .status(request.getStatus())
            .executedBy(executor)
            .executedAt(
                request.getExecutedAt() != null ? request.getExecutedAt() : LocalDateTime.now())
            .durationSeconds(request.getDurationSeconds())
            .notes(request.getNotes())
            .actualResult(request.getActualResult())
            .buildVersion(request.getBuildVersion())
            .environment(request.getEnvironment())
            .attachments(request.getAttachments())
            .build();

    // Set relationships - inherit from test case if not provided
    if (request.getPitchId() != null) {
      Pitch pitch =
          pitchRepository
              .findById(request.getPitchId())
              .orElseThrow(() -> new RuntimeException("Pitch not found: " + request.getPitchId()));
      testRun.setPitch(pitch);
      testRun.setCycle(pitch.getCycle());
    } else if (testCase.getPitch() != null) {
      testRun.setPitch(testCase.getPitch());
      testRun.setCycle(testCase.getCycle());
    }

    if (request.getCycleId() != null) {
      Cycle cycle =
          cycleRepository
              .findById(request.getCycleId())
              .orElseThrow(() -> new RuntimeException("Cycle not found: " + request.getCycleId()));
      testRun.setCycle(cycle);
    } else if (testCase.getCycle() != null && testRun.getCycle() == null) {
      testRun.setCycle(testCase.getCycle());
    }

    testRun = testRunRepository.save(testRun);
    log.info(
        "Created test run for test case: {} by user: {} with status: {}",
        testCase.getTestCaseKey(),
        userId,
        request.getStatus());

    return toDTO(testRun);
  }

  /** Update test run status. */
  @Transactional
  public TestRunDTO updateTestRunStatus(Long id, TestRunStatus status, String notes) {
    checkFeatureEnabled();

    TestRun testRun =
        testRunRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Test run not found: " + id));

    testRun.setStatus(status);
    if (notes != null) {
      testRun.setNotes(notes);
    }

    testRun = testRunRepository.save(testRun);
    log.info("Updated test run {} status to: {}", id, status);

    return toDTO(testRun);
  }

  /** Delete a test run. */
  @Transactional
  public void deleteTestRun(Long id) {
    checkFeatureEnabled();

    TestRun testRun =
        testRunRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Test run not found: " + id));

    testRunRepository.delete(testRun);
    log.info("Deleted test run: {}", id);
  }

  /** Get a test run by ID. */
  @Transactional(readOnly = true)
  public TestRunDTO getTestRunById(Long id) {
    checkFeatureEnabled();

    TestRun testRun =
        testRunRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Test run not found: " + id));

    return toDTO(testRun);
  }

  /** Get all test runs. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getAllTestRuns() {
    checkFeatureEnabled();

    return testRunRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get test runs by test case. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getTestRunsByTestCase(Long testCaseId) {
    checkFeatureEnabled();

    return testRunRepository.findByTestCaseIdOrderByExecutedAtDesc(testCaseId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get test runs by pitch. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getTestRunsByPitch(Long pitchId) {
    checkFeatureEnabled();

    return testRunRepository.findByPitchIdOrderByExecutedAtDesc(pitchId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get test runs by cycle. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getTestRunsByCycle(Long cycleId) {
    checkFeatureEnabled();

    return testRunRepository.findByCycleIdOrderByExecutedAtDesc(cycleId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get latest test run for a test case. */
  @Transactional(readOnly = true)
  public TestRunDTO getLatestTestRun(Long testCaseId) {
    checkFeatureEnabled();

    TestRun testRun = testRunRepository.findLatestByTestCaseId(testCaseId);
    return testRun != null ? toDTO(testRun) : null;
  }

  /** Get test runs by executor. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getTestRunsByExecutor(Long userId) {
    checkFeatureEnabled();

    return testRunRepository.findByExecutedById(userId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /** Get test runs within date range. */
  @Transactional(readOnly = true)
  public List<TestRunDTO> getTestRunsByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
    checkFeatureEnabled();

    return testRunRepository.findByExecutedAtBetween(fromDate, toDate).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  private void checkFeatureEnabled() {
    if (!testManagementEnabled) {
      throw new RuntimeException(localizationService.getMessage("qa.feature.disabled"));
    }
  }

  /** Convert entity to DTO. */
  public TestRunDTO toDTO(TestRun testRun) {
    BugReport bugReport = testRun.getBugReport();

    return TestRunDTO.builder()
        .id(testRun.getId())
        .testCaseId(testRun.getTestCase().getId())
        .testCaseKey(testRun.getTestCase().getTestCaseKey())
        .testCaseTitle(testRun.getTestCase().getTitle())
        .cycleId(testRun.getCycle() != null ? testRun.getCycle().getId() : null)
        .cycleName(testRun.getCycle() != null ? testRun.getCycle().getName() : null)
        .pitchId(testRun.getPitch() != null ? testRun.getPitch().getId() : null)
        .pitchTitle(testRun.getPitch() != null ? testRun.getPitch().getTitle() : null)
        .status(testRun.getStatus())
        .executedById(testRun.getExecutedBy().getId())
        .executedByName(testRun.getExecutedBy().getUsername())
        .executedAt(testRun.getExecutedAt())
        .durationSeconds(testRun.getDurationSeconds())
        .notes(testRun.getNotes())
        .actualResult(testRun.getActualResult())
        .buildVersion(testRun.getBuildVersion())
        .environment(testRun.getEnvironment())
        .attachments(testRun.getAttachments())
        .bugReportId(bugReport != null ? bugReport.getId() : null)
        .bugReportKey(bugReport != null ? bugReport.getBugKey() : null)
        .createdAt(testRun.getCreatedAt())
        .build();
  }
}
