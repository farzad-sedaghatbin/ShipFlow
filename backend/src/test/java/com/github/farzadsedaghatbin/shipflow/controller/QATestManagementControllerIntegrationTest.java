package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private Cycle testCycle;
    private User testUser;
    private BugReport testBugReport;

    @BeforeEach
    void setUp() {
        bugReportRepository.deleteAll();
        cycleRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user to match @WithMockUser
        User adminUser = User.builder()
                .username("admin")
                .password("password")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(adminUser);

        testCycle = Cycle.builder()
                .name("Test Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .isActive(true)
                .build();
        testCycle = cycleRepository.save(testCycle);

        testUser = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .role(UserRole.QA)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        testBugReport = BugReport.builder()
                .bugKey("BUG-001")
                .title("Test Bug")
                .description("Test bug description")
                .severity(BugSeverity.MAJOR)
                .status(BugStatus.OPEN)
                .cycle(testCycle)
                .reporter(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testBugReport = bugReportRepository.save(testBugReport);
    }

    @Test
    void getAllBugReports_ShouldReturnBugReports() throws Exception {
        mockMvc.perform(get("/api/qa/bug-reports"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].title", is("Test Bug")));
    }

    @Test
    void getBugReportsWithFilters_ByStatuses_ShouldReturnFilteredBugReports() throws Exception {
        // Create additional bug with different status
        BugReport inProgressBug = BugReport.builder()
                .bugKey("BUG-002")
                .title("In Progress Bug")
                .description("Bug in progress")
                .severity(BugSeverity.CRITICAL)
                .status(BugStatus.IN_PROGRESS)
                .cycle(testCycle)
                .reporter(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bugReportRepository.save(inProgressBug);

        mockMvc.perform(get("/api/qa/bug-reports/filter")
                        .param("statuses", "OPEN,IN_PROGRESS")
                        .param("exclude", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    void getBugReportsWithFilters_BySeverities_ShouldReturnFilteredBugReports() throws Exception {
        // Create additional bug with different severity
        BugReport criticalBug = BugReport.builder()
                .bugKey("BUG-003")
                .title("Critical Bug")
                .description("Critical issue")
                .severity(BugSeverity.CRITICAL)
                .status(BugStatus.OPEN)
                .cycle(testCycle)
                .reporter(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bugReportRepository.save(criticalBug);

        mockMvc.perform(get("/api/qa/bug-reports/filter")
                        .param("severities", "CRITICAL,BLOCKER")
                        .param("exclude", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.severity == 'CRITICAL')]", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getBugReportsWithFilters_WithExclude_ShouldReturnExcludedBugReports() throws Exception {
        // Create bugs with different statuses
        BugReport closedBug = BugReport.builder()
                .bugKey("BUG-004")
                .title("Closed Bug")
                .description("Closed bug description")
                .severity(BugSeverity.MINOR)
                .status(BugStatus.CLOSED)
                .cycle(testCycle)
                .reporter(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bugReportRepository.save(closedBug);

        mockMvc.perform(get("/api/qa/bug-reports/filter")
                        .param("statuses", "CLOSED,RESOLVED")
                        .param("exclude", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getBugReportsWithFilters_ByCycle_ShouldReturnFilteredBugReports() throws Exception {
        mockMvc.perform(get("/api/qa/bug-reports/filter")
                        .param("cycleId", testCycle.getId().toString())
                        .param("exclude", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].cycleId", is(testCycle.getId().intValue())));
    }

    @Test
    void createBugReport_WithValidData_ShouldCreateBugReport() throws Exception {
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("New Bug")
                .description("New bug description")
                .severity(BugSeverity.MAJOR)
                .status(BugStatus.OPEN)
                .cycleId(testCycle.getId())
                .build();

        mockMvc.perform(post("/api/qa/bug-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is("New Bug")))
                .andExpect(jsonPath("$.severity", is("MAJOR")))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }
}
