package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN", "QA"})
class QATestManagementControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BugReportRepository bugReportRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private TestCaseRepository testCaseRepository;

  @Autowired
  private TestRunRepository testRunRepository;

  @Autowired
  private ProjectRepository projectRepository;

  private Cycle testCycle;
  private User testUser;
  private BugReport testBugReport;

  @BeforeEach
  void setUp() {
    bugReportRepository.deleteAll();
    cycleRepository.deleteAll();
    userRepository.deleteAll();

    // Create admin user to match @WithMockUser
    User adminUser = User.builder().username("admin").password("password").email("admin@example.com")
        .role(UserRole.ADMIN).isActive(true).createdAt(LocalDateTime.now()).build();
    userRepository.save(adminUser);

    testCycle = Cycle.builder().name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testUser = User.builder().username("qa-test-user").password("password").email("test@example.com")
        .role(UserRole.MEMBER).isActive(true).createdAt(LocalDateTime.now()).build();
    testUser = userRepository.save(testUser);

    testBugReport = BugReport.builder().bugKey("BUG-001").title("Test Bug").description("Test bug description")
        .severity(BugSeverity.MAJOR).status(BugStatus.OPEN).cycle(testCycle).reporter(testUser)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    testBugReport = bugReportRepository.save(testBugReport);
  }

  @Test
  void getAllBugReports_ShouldReturnBugReports() throws Exception {
    mockMvc.perform(get("/api/qa/bug-reports")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].title", is("Test Bug")));
  }

  @Test
  void getBugReportsWithFilters_ByStatuses_ShouldReturnFilteredBugReports() throws Exception {
    // Create additional bug with different status
    BugReport inProgressBug = BugReport.builder().bugKey("BUG-002").title("In Progress Bug")
        .description("Bug in progress").severity(BugSeverity.CRITICAL).status(BugStatus.IN_PROGRESS)
        .cycle(testCycle).reporter(testUser).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
        .build();
    bugReportRepository.save(inProgressBug);

    mockMvc.perform(
        get("/api/qa/bug-reports/filter").param("statuses", "OPEN,IN_PROGRESS").param("exclude", "false"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)));
  }

  @Test
  void getBugReportsWithFilters_BySeverities_ShouldReturnFilteredBugReports() throws Exception {
    // Create additional bug with different severity
    BugReport criticalBug = BugReport.builder().bugKey("BUG-003").title("Critical Bug")
        .description("Critical issue").severity(BugSeverity.CRITICAL).status(BugStatus.OPEN).cycle(testCycle)
        .reporter(testUser).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    bugReportRepository.save(criticalBug);

    mockMvc.perform(
        get("/api/qa/bug-reports/filter").param("severities", "CRITICAL,BLOCKER").param("exclude", "false"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[?(@.severity == 'CRITICAL')]", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  void getBugReportsWithFilters_WithExclude_ShouldReturnExcludedBugReports() throws Exception {
    // Create bugs with different statuses
    BugReport closedBug = BugReport.builder().bugKey("BUG-004").title("Closed Bug")
        .description("Closed bug description").severity(BugSeverity.MINOR).status(BugStatus.CLOSED)
        .cycle(testCycle).reporter(testUser).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
        .build();
    bugReportRepository.save(closedBug);

    mockMvc.perform(get("/api/qa/bug-reports/filter").param("statuses", "CLOSED,RESOLVED").param("exclude", "true"))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  void getBugReportsWithFilters_ByCycle_ShouldReturnFilteredBugReports() throws Exception {
    mockMvc.perform(get("/api/qa/bug-reports/filter").param("cycleId", testCycle.getId().toString())
        .param("exclude", "false")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$.content[0].cycleId", is(testCycle.getId().intValue())));
  }

  @Test
  void getBugReportsWithFilters_ByReporters_ShouldReturnOnlyMatchingReporter() throws Exception {
    // Second reporter with their own bug
    User otherReporter = User.builder().username("other-reporter").password("password")
        .email("other@example.com").role(UserRole.MEMBER).isActive(true).createdAt(LocalDateTime.now()).build();
    otherReporter = userRepository.save(otherReporter);

    BugReport otherBug = BugReport.builder().bugKey("BUG-R02").title("Other Reporter Bug")
        .description("Reported by someone else").severity(BugSeverity.MINOR).status(BugStatus.OPEN)
        .cycle(testCycle).reporter(otherReporter).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
        .build();
    bugReportRepository.save(otherBug);

    // Filtering by testUser's id must return only their bug, not otherReporter's.
    mockMvc.perform(get("/api/qa/bug-reports/filter").param("reporterIds", testUser.getId().toString())
        .param("exclude", "false")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].reporterId", is(testUser.getId().intValue())));
  }

  @Test
  void getBugStats_ByReporters_ShouldRespectReporterFilter() throws Exception {
    User otherReporter = User.builder().username("stats-reporter").password("password")
        .email("stats@example.com").role(UserRole.MEMBER).isActive(true).createdAt(LocalDateTime.now()).build();
    otherReporter = userRepository.save(otherReporter);

    BugReport otherBug = BugReport.builder().bugKey("BUG-R03").title("Stats Reporter Bug")
        .description("Reported by someone else").severity(BugSeverity.MINOR).status(BugStatus.OPEN)
        .cycle(testCycle).reporter(otherReporter).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
        .build();
    bugReportRepository.save(otherBug);

    // Stats scoped to testUser must count only their single bug.
    mockMvc.perform(get("/api/qa/bug-reports/stats").param("reporterIds", testUser.getId().toString()))
        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.total", is(1)));
  }

  @Test
  void createBugReport_WithValidData_ShouldCreateBugReport() throws Exception {
    CreateBugReportRequest request = CreateBugReportRequest.builder().title("New Bug")
        .description("New bug description").severity(BugSeverity.MAJOR).status(BugStatus.OPEN)
        .cycleId(testCycle.getId()).build();

    mockMvc.perform(post("/api/qa/bug-reports").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.title", is("New Bug"))).andExpect(jsonPath("$.severity", is("MAJOR")))
        .andExpect(jsonPath("$.status", is("OPEN")));
  }

  @Test
  void recordTestRunsBulk_ShouldCreateOneRunPerTestCase() throws Exception {
    TestCase tc1 = testCaseRepository.save(TestCase.builder().testCaseKey("TC-001").title("Login works")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.HIGH).status(TestCaseStatus.READY)
        .aiGenerated(false).cycle(testCycle).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());
    TestCase tc2 = testCaseRepository.save(TestCase.builder().testCaseKey("TC-002").title("Logout works")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.MEDIUM).status(TestCaseStatus.READY)
        .aiGenerated(true).cycle(testCycle).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());

    String body = "{\"testCaseIds\":[" + tc1.getId() + "," + tc2.getId()
        + "],\"status\":\"PASSED\",\"environment\":\"Chrome / staging\"}";

    mockMvc.perform(post("/api/qa/test-runs/bulk").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].status", is("PASSED")))
        .andExpect(jsonPath("$[0].environment", is("Chrome / staging")));
  }

  @Test
  void linkAndUnlinkDefect_ShouldAttachMultipleBugsToARun() throws Exception {
    TestCase tc = testCaseRepository.save(TestCase.builder().testCaseKey("TC-100").title("Checkout")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.HIGH).status(TestCaseStatus.READY)
        .aiGenerated(false).cycle(testCycle).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());
    TestRun run = testRunRepository.save(TestRun.builder().testCase(tc).status(TestRunStatus.FAILED)
        .executedBy(testUser).executedAt(LocalDateTime.now()).build());
    BugReport bug1 = bugReportRepository.save(BugReport.builder().bugKey("BUG-201").title("Crash on pay")
        .description("d").severity(BugSeverity.MAJOR).status(BugStatus.OPEN).reporter(testUser)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    BugReport bug2 = bugReportRepository.save(BugReport.builder().bugKey("BUG-202").title("Wrong total")
        .description("d").severity(BugSeverity.MINOR).status(BugStatus.OPEN).reporter(testUser)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

    mockMvc.perform(patch("/api/qa/test-runs/{runId}/defects/{bugId}", run.getId(), bug1.getId()))
        .andExpect(status().isOk());
    mockMvc.perform(patch("/api/qa/test-runs/{runId}/defects/{bugId}", run.getId(), bug2.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.linkedBugs", hasSize(2)));

    // Unlink one — the other remains
    mockMvc.perform(delete("/api/qa/test-runs/{runId}/defects/{bugId}", run.getId(), bug1.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.linkedBugs", hasSize(1)))
        .andExpect(jsonPath("$.linkedBugs[0].bugKey", is("BUG-202")));
  }

  @Test
  void getTestCasesWithFilters_ByProject_ShouldReturnOnlyThatProjectsCases() throws Exception {
    Project project = projectRepository.save(Project.builder().name("Alpha").projectKey("ALPHA")
        .isActive(true).createdAt(LocalDateTime.now()).build());
    testCaseRepository.save(TestCase.builder().testCaseKey("TC-300").title("In project")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.LOW).status(TestCaseStatus.READY)
        .aiGenerated(false).project(project).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());
    testCaseRepository.save(TestCase.builder().testCaseKey("TC-301").title("No project")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.LOW).status(TestCaseStatus.READY)
        .aiGenerated(false).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());

    mockMvc.perform(get("/api/qa/test-cases/filter").param("projectId", project.getId().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].testCaseKey", is("TC-300")))
        .andExpect(jsonPath("$[0].projectKey", is("ALPHA")));
  }

  @Test
  void getTestCasesWithFilters_ByAiGenerated_ShouldReturnOnlyAiCases() throws Exception {
    testCaseRepository.save(TestCase.builder().testCaseKey("TC-010").title("Manual case")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.LOW).status(TestCaseStatus.READY)
        .aiGenerated(false).cycle(testCycle).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());
    testCaseRepository.save(TestCase.builder().testCaseKey("TC-011").title("AI case")
        .type(TestCaseType.FUNCTIONAL).priority(TestCasePriority.LOW).status(TestCaseStatus.READY)
        .aiGenerated(true).cycle(testCycle).createdBy(testUser).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());

    mockMvc.perform(get("/api/qa/test-cases/filter").param("aiGenerated", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.aiGenerated == true)]", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[?(@.aiGenerated == false)]", hasSize(0)));
  }

  @Test
  void updateBugQaAssignee_ShouldAssignAndUnassignQaTester() throws Exception {
    Person qaPerson = personRepository.save(Person.builder().name("Mina QA").email("mina-qa@example.com")
        .isActive(true).createdAt(LocalDateTime.now()).build());

    // Assign
    mockMvc.perform(patch("/api/qa/bug-reports/{id}/qa-assignee", testBugReport.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"qaAssigneeId\":" + qaPerson.getId() + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qaAssigneeId", is(qaPerson.getId().intValue())))
        .andExpect(jsonPath("$.qaAssigneeName", is("Mina QA")));

    // Unassign (null clears it)
    mockMvc.perform(patch("/api/qa/bug-reports/{id}/qa-assignee", testBugReport.getId())
        .contentType(MediaType.APPLICATION_JSON).content("{\"qaAssigneeId\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qaAssigneeId").doesNotExist());
  }
}
