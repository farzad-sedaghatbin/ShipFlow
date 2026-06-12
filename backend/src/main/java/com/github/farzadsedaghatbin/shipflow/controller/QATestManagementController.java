package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for QA Test Management endpoints. Provides CRUD operations
 * for test cases, bug reports, and test runs.
 */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QA Test Management", description = "Test case management, bug tracking, and test execution")
public class QATestManagementController {

  private final TestCaseService testCaseService;
  private final BugReportService bugReportService;
  private final TestRunService testRunService;
  private final QADashboardService qaDashboardService;
  private final QATestGenerationService qaTestGenerationService;
  private final UserRepository userRepository;
  private final MessageService messageService;
  private final AuditService auditService;

  @Value("${app.qa.test-management.enabled:true}")
  private boolean testManagementEnabled;

  // ========== Status ==========

  @GetMapping("/test-management/status")
  @Operation(summary = "Get QA Test Management status", description = "Returns the status of QA test management features")
  public ResponseEntity<QATestManagementStatusDTO> getTestManagementStatus() {
    return ResponseEntity.ok(qaDashboardService.getStatus());
  }

  // ========== Test Cases ==========

  @GetMapping("/test-cases")
  @Operation(summary = "Get all test cases", description = "Returns all test cases")
  public ResponseEntity<List<TestCaseDTO>> getAllTestCases() {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getAllTestCases());
  }

  @GetMapping("/test-cases/{id}")
  @Operation(summary = "Get test case by ID", description = "Returns a test case by its ID")
  public ResponseEntity<TestCaseDTO> getTestCaseById(@PathVariable Long id) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getTestCaseById(id));
  }

  @GetMapping("/test-cases/{id}/history")
  public ResponseEntity<Page<EntityHistoryDTO>> getTestCaseHistory(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    checkFeatureEnabled();
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(auditService.getTestCaseHistory(id, pageable));
  }

  @GetMapping("/test-cases/key/{key}")
  @Operation(summary = "Get test case by key", description = "Returns a test case by its unique key")
  public ResponseEntity<TestCaseDTO> getTestCaseByKey(@PathVariable String key) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getTestCaseByKey(key));
  }

  @GetMapping("/test-cases/pitch/{pitchId}")
  @Operation(summary = "Get test cases by pitch", description = "Returns all test cases for a pitch")
  public ResponseEntity<List<TestCaseDTO>> getTestCasesByPitch(@PathVariable Long pitchId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getTestCasesByPitch(pitchId));
  }

  @GetMapping("/test-cases/cycle/{cycleId}")
  @Operation(summary = "Get test cases by cycle", description = "Returns all test cases for a cycle")
  public ResponseEntity<List<TestCaseDTO>> getTestCasesByCycle(@PathVariable Long cycleId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getTestCasesByCycle(cycleId));
  }

  @GetMapping("/test-cases/status/{status}")
  @Operation(summary = "Get test cases by status", description = "Returns test cases filtered by status")
  public ResponseEntity<List<TestCaseDTO>> getTestCasesByStatus(@PathVariable TestCaseStatus status) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testCaseService.getTestCasesByStatus(status));
  }

  @GetMapping("/test-cases/filter")
  @Operation(summary = "Get test cases with multi-selection filters", description = "Filter test cases by cycle, pitch, statuses, types, and priorities")
  public ResponseEntity<List<TestCaseDTO>> getTestCasesWithFilters(@RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long pitchId, @RequestParam(required = false) List<TestCaseStatus> statuses,
      @RequestParam(required = false) List<TestCaseType> types,
      @RequestParam(required = false) List<TestCasePriority> priorities) {
    checkFeatureEnabled();
    return ResponseEntity
        .ok(testCaseService.getTestCasesWithFilters(cycleId, pitchId, statuses, types, priorities));
  }

  @PostMapping("/test-cases")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Create a test case", description = "Creates a new test case")
  public ResponseEntity<TestCaseDTO> createTestCase(@Valid @RequestBody CreateTestCaseRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(testCaseService.createTestCase(request, userId));
  }

  @PutMapping("/test-cases/{id}")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Update a test case", description = "Updates an existing test case")
  public ResponseEntity<TestCaseDTO> updateTestCase(@PathVariable Long id,
      @Valid @RequestBody UpdateTestCaseRequest request, @AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(testCaseService.updateTestCase(id, request, userId));
  }

  @DeleteMapping("/test-cases/{id}")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Delete a test case", description = "Deletes a test case")
  public ResponseEntity<Map<String, String>> deleteTestCase(@PathVariable Long id) {
    checkFeatureEnabled();
    testCaseService.deleteTestCase(id);
    return ResponseEntity.ok(Map.of("message", messageService.getMessage("qa.test.case.deleted")));
  }

  // ========== Bug Reports ==========

  @GetMapping("/bug-reports")
  @Operation(summary = "Get all bug reports", description = "Returns all bug reports with pagination")
  public ResponseEntity<Page<BugReportDTO>> getAllBugReports(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    checkFeatureEnabled();
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    return ResponseEntity.ok(bugReportService.getAllBugReports(pageable));
  }

  @GetMapping("/bug-reports/filter")
  @Operation(summary = "Get bug reports with multi-selection filters", description = "Filter bug reports by project, cycle, pitch, multiple statuses, severities, and assignees with optional exclusion")
  public ResponseEntity<Page<BugReportDTO>> getBugReportsWithFilters(@RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long cycleId, @RequestParam(required = false) Long pitchId,
      @RequestParam(required = false) List<BugStatus> statuses,
      @RequestParam(required = false) List<BugSeverity> severities,
      @RequestParam(required = false) List<Long> assigneeIds,
      @RequestParam(required = false, defaultValue = "false") Boolean exclude,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    checkFeatureEnabled();
    Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    return ResponseEntity.ok(bugReportService.getBugReportsWithFilters(projectId, cycleId, pitchId, statuses,
        severities, assigneeIds, exclude, pageable));
  }

  @GetMapping("/bug-reports/{id}")
  @Operation(summary = "Get bug report by ID", description = "Returns a bug report by its ID")
  public ResponseEntity<BugReportDTO> getBugReportById(@PathVariable Long id) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportById(id));
  }

  @GetMapping("/bug-reports/{id}/history")
  public ResponseEntity<Page<EntityHistoryDTO>> getBugReportHistory(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    checkFeatureEnabled();
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(auditService.getBugReportHistory(id, pageable));
  }

  @GetMapping("/bug-reports/key/{key}")
  @Operation(summary = "Get bug report by key", description = "Returns a bug report by its unique key")
  public ResponseEntity<BugReportDTO> getBugReportByKey(@PathVariable String key) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportByKey(key));
  }

  @GetMapping("/bug-reports/pitch/{pitchId}")
  @Operation(summary = "Get bug reports by pitch", description = "Returns all bug reports for a pitch")
  public ResponseEntity<List<BugReportDTO>> getBugReportsByPitch(@PathVariable Long pitchId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportsByPitch(pitchId));
  }

  @GetMapping("/bugs/pitch/{pitchId}")
  @Operation(summary = "Get bugs by pitch (alias)", description = "Returns all bug reports for a pitch")
  public ResponseEntity<List<BugReportDTO>> getBugsByPitch(@PathVariable Long pitchId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportsByPitch(pitchId));
  }

  @GetMapping("/bug-reports/cycle/{cycleId}")
  @Operation(summary = "Get bug reports by cycle", description = "Returns all bug reports for a cycle")
  public ResponseEntity<List<BugReportDTO>> getBugReportsByCycle(@PathVariable Long cycleId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportsByCycle(cycleId));
  }

  @GetMapping("/bugs/cycle/{cycleId}")
  @Operation(summary = "Get bugs by cycle (alias)", description = "Returns all bug reports for a cycle")
  public ResponseEntity<List<BugReportDTO>> getBugsByCycle(@PathVariable Long cycleId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getBugReportsByCycle(cycleId));
  }

  @GetMapping("/bug-reports/open")
  @Operation(summary = "Get open bug reports", description = "Returns all open bug reports")
  public ResponseEntity<List<BugReportDTO>> getOpenBugReports() {
    checkFeatureEnabled();
    return ResponseEntity.ok(bugReportService.getOpenBugReports());
  }

  @GetMapping("/bug-reports/my-assigned")
  @Operation(summary = "Get my assigned bugs", description = "Returns bug reports assigned to current user")
  public ResponseEntity<List<BugReportDTO>> getMyAssignedBugs(@AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(bugReportService.getBugReportsByAssignee(userId));
  }

  @GetMapping("/bug-reports/my-reported")
  @Operation(summary = "Get bugs I reported", description = "Returns bug reports created by current user")
  public ResponseEntity<List<BugReportDTO>> getMyReportedBugs(@AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(bugReportService.getBugReportsByReporter(userId));
  }

  @PostMapping("/bug-reports")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN')")
  @Operation(summary = "Create a bug report", description = "Creates a new bug report")
  public ResponseEntity<BugReportDTO> createBugReport(@Valid @RequestBody CreateBugReportRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(bugReportService.createBugReport(request, userId));
  }

  @PutMapping("/bug-reports/{id}")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN')")
  @Operation(summary = "Update a bug report", description = "Updates an existing bug report")
  public ResponseEntity<BugReportDTO> updateBugReport(@PathVariable Long id,
      @Valid @RequestBody UpdateBugReportRequest request, @AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(bugReportService.updateBugReport(id, request, userId));
  }

  @DeleteMapping("/bug-reports/{id}")
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  @Operation(summary = "Delete a bug report", description = "Deletes a bug report")
  public ResponseEntity<Map<String, String>> deleteBugReport(@PathVariable Long id) {
    checkFeatureEnabled();
    bugReportService.deleteBugReport(id);
    return ResponseEntity.ok(Map.of("message", messageService.getMessage("qa.bug.deleted")));
  }

  // ========== Test Runs ==========

  @GetMapping("/test-runs")
  @Operation(summary = "Get all test runs", description = "Returns all test runs")
  public ResponseEntity<List<TestRunDTO>> getAllTestRuns() {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.getAllTestRuns());
  }

  @GetMapping("/test-runs/{id}")
  @Operation(summary = "Get test run by ID", description = "Returns a test run by its ID")
  public ResponseEntity<TestRunDTO> getTestRunById(@PathVariable Long id) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.getTestRunById(id));
  }

  @GetMapping("/test-runs/test-case/{testCaseId}")
  @Operation(summary = "Get test runs by test case", description = "Returns all test runs for a test case")
  public ResponseEntity<List<TestRunDTO>> getTestRunsByTestCase(@PathVariable Long testCaseId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.getTestRunsByTestCase(testCaseId));
  }

  @GetMapping("/test-runs/pitch/{pitchId}")
  @Operation(summary = "Get test runs by pitch", description = "Returns all test runs for a pitch")
  public ResponseEntity<List<TestRunDTO>> getTestRunsByPitch(@PathVariable Long pitchId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.getTestRunsByPitch(pitchId));
  }

  @GetMapping("/test-runs/cycle/{cycleId}")
  @Operation(summary = "Get test runs by cycle", description = "Returns all test runs for a cycle")
  public ResponseEntity<List<TestRunDTO>> getTestRunsByCycle(@PathVariable Long cycleId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.getTestRunsByCycle(cycleId));
  }

  @GetMapping("/test-runs/latest/{testCaseId}")
  @Operation(summary = "Get latest test run", description = "Returns the most recent test run for a test case")
  public ResponseEntity<TestRunDTO> getLatestTestRun(@PathVariable Long testCaseId) {
    checkFeatureEnabled();
    TestRunDTO testRun = testRunService.getLatestTestRun(testCaseId);
    return testRun != null ? ResponseEntity.ok(testRun) : ResponseEntity.notFound().build();
  }

  @PostMapping("/test-runs")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Create a test run", description = "Records a new test run execution")
  public ResponseEntity<TestRunDTO> createTestRun(@Valid @RequestBody CreateTestRunRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    checkFeatureEnabled();
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(testRunService.createTestRun(request, userId));
  }

  @PatchMapping("/test-runs/{id}/status")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Update test run status", description = "Updates the status of a test run")
  public ResponseEntity<TestRunDTO> updateTestRunStatus(@PathVariable Long id, @RequestParam TestRunStatus status,
      @RequestParam(required = false) String notes) {
    checkFeatureEnabled();
    return ResponseEntity.ok(testRunService.updateTestRunStatus(id, status, notes));
  }

  @DeleteMapping("/test-runs/{id}")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Delete a test run", description = "Deletes a test run")
  public ResponseEntity<Map<String, String>> deleteTestRun(@PathVariable Long id) {
    checkFeatureEnabled();
    testRunService.deleteTestRun(id);
    return ResponseEntity.ok(Map.of("message", messageService.getMessage("qa.test.run.deleted")));
  }

  // ========== Coverage & Dashboard ==========

  @GetMapping("/coverage/pitch/{pitchId}")
  @Operation(summary = "Get test coverage for pitch", description = "Returns test coverage statistics for a pitch")
  public ResponseEntity<TestCoverageDTO> getTestCoverageByPitch(@PathVariable Long pitchId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(qaDashboardService.getTestCoverageByPitch(pitchId));
  }

  @GetMapping("/dashboard/cycle/{cycleId}")
  @Operation(summary = "Get QA dashboard for cycle", description = "Returns comprehensive QA dashboard for a cycle")
  public ResponseEntity<QADashboardDTO> getQADashboardByCycle(@PathVariable Long cycleId) {
    checkFeatureEnabled();
    return ResponseEntity.ok(qaDashboardService.getQADashboardByCycle(cycleId));
  }

  // ========== AI Test Generation ==========

  @PostMapping("/generate-test-cases")
  @PreAuthorize("hasAnyRole('MEMBER', 'MANAGER', 'ADMIN', 'QA')")
  @Operation(summary = "Generate test cases with AI", description = "Generates test case suggestions using AI based on pitch context")
  public ResponseEntity<GenerateTestCasesResponse> generateTestCases(
      @Valid @RequestBody GenerateTestCasesRequest request) {
    checkFeatureEnabled();
    return ResponseEntity.ok(qaTestGenerationService.generateTestCases(request));
  }

  // ========== Helpers ==========

  private Long getUserId(UserDetails userDetails) {
    User user = userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    return user.getId();
  }

  private void checkFeatureEnabled() {
    if (!testManagementEnabled) {
      throw new RuntimeException("QA Test Management feature is not enabled");
    }
  }
}
