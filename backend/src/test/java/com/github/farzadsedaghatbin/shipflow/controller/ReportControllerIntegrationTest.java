package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.report.EnhancedCycleReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.report.RiskDistributionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReportController endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ReportController Integration Tests")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Autowired
    private com.github.farzadsedaghatbin.shipflow.repository.PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonRepository personRepository;

    @BeforeEach
    void setUp() {
        // Clear all permissions before each test
        userRepository.deleteAll();
        personRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create test person and user
        Person testPerson = Person.builder()
                .name("Test User")
                .email("testuser@example.com")
                .build();
        testPerson = personRepository.save(testPerson);

        User testUser = User.builder()
                .username("testuser")
                .password("password")
                .role(UserRole.MEMBER)
                .person(testPerson)
                .isActive(true)
                .build();
        userRepository.save(testUser);

        // Set up permissions for MEMBER role to access reports and cycles
        Permission cycleReadPermission = Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.CYCLE)
                .permissionType(PermissionType.READ)
                .build();

        Permission cycleUpdatePermission = Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.CYCLE)
                .permissionType(PermissionType.UPDATE)
                .build();

        Permission reportViewPermission = Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.REPORT)
                .permissionType(PermissionType.READ)
                .build();

        permissionRepository.save(cycleReadPermission);
        permissionRepository.save(cycleUpdatePermission);
        permissionRepository.save(reportViewPermission);
    }

    @Nested
    @DisplayName("GET /api/reports/cycle/{cycleId}/enhanced")
    class GetEnhancedReportTests {

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should return enhanced cycle report with 200 OK")
        void shouldReturnEnhancedReport() throws Exception {
            // Given
            EnhancedCycleReportDTO report = createSampleEnhancedReport();
            when(reportService.getEnhancedCycleReport(1L)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/enhanced")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.cycleId").value(1))
                    .andExpect(jsonPath("$.cycleName").value("Q1 2026"))
                    .andExpect(jsonPath("$.projectName").value("Test Project"))
                    .andExpect(jsonPath("$.totalPitches").value(3))
                    .andExpect(jsonPath("$.completedPitches").value(2))
                    .andExpect(jsonPath("$.inProgressPitches").value(1))
                    .andExpect(jsonPath("$.totalAppetiteHours").value(96.0))
                    .andExpect(jsonPath("$.totalActualHours").value(85.5))
                    .andExpect(jsonPath("$.varianceHours").value(-10.5))
                    .andExpect(jsonPath("$.efficiencyPercentage").value(89.06))
                    .andExpect(jsonPath("$.riskDistribution").exists())
                    .andExpect(jsonPath("$.riskDistribution.lowRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.mediumRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.highRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.averageRiskScore").value(45.0))
                    .andExpect(jsonPath("$.totalTeamMembers").value(3))
                    .andExpect(jsonPath("$.averageHoursPerMember").value(28.5))
                    .andExpect(jsonPath("$.topPerformers").isArray())
                    .andExpect(jsonPath("$.overBudgetPitches").isArray());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should return risk distribution details")
        void shouldReturnRiskDistribution() throws Exception {
            // Given
            EnhancedCycleReportDTO report = createSampleEnhancedReport();
            when(reportService.getEnhancedCycleReport(1L)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/enhanced"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riskDistribution.lowRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.mediumRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.highRiskCount").value(1))
                    .andExpect(jsonPath("$.riskDistribution.criticalRiskCount").value(0))
                    .andExpect(jsonPath("$.riskDistribution.averageRiskScore").value(45.0))
                    .andExpect(jsonPath("$.riskDistribution.maxRiskScore").value(65.0))
                    .andExpect(jsonPath("$.riskDistribution.minRiskScore").value(20.0));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should return team member statistics")
        void shouldReturnTeamMemberStats() throws Exception {
            // Given
            EnhancedCycleReportDTO report = createSampleEnhancedReport();
            when(reportService.getEnhancedCycleReport(1L)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/enhanced"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTeamMembers").value(3))
                    .andExpect(jsonPath("$.averageHoursPerMember").value(28.5))
                    .andExpect(jsonPath("$.maxHoursPerMember").value(45.0))
                    .andExpect(jsonPath("$.minHoursPerMember").value(12.0));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should return variance analysis metrics")
        void shouldReturnVarianceMetrics() throws Exception {
            // Given
            EnhancedCycleReportDTO report = createSampleEnhancedReport();
            when(reportService.getEnhancedCycleReport(1L)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/enhanced"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.varianceHours").value(-10.5))
                    .andExpect(jsonPath("$.variancePercentage").exists())
                    .andExpect(jsonPath("$.efficiencyPercentage").value(89.06));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should return out-of-scope work metrics")
        void shouldReturnOutOfScopeMetrics() throws Exception {
            // Given
            EnhancedCycleReportDTO report = createSampleEnhancedReport();
            when(reportService.getEnhancedCycleReport(1L)).thenReturn(report);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/enhanced"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTasks").value(10))
                    .andExpect(jsonPath("$.completedTasks").value(7))
                    .andExpect(jsonPath("$.totalTaskEstimateHours").value(50.0))
                    .andExpect(jsonPath("$.totalTaskActualHours").value(45.0));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/reports/cycle/1/enhanced"))
                    .andExpect(status().is3xxRedirection()); // Spring Security redirects to login
        }

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should handle non-existent cycle gracefully")
        void shouldHandleNonExistentCycle() throws Exception {
            // Given
            when(reportService.getEnhancedCycleReport(999L))
                    .thenThrow(new RuntimeException("Cycle not found with id: 999"));

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/999/enhanced"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should export CSV with correct content type")
        void shouldExportCsvWithCorrectContentType() throws Exception {
            // Given
            String csvContent = "Cycle Report: Q1 2026\n\nSummary\nTotal Pitches,3\n";
            when(reportService.exportCycleReportCsv(1L)).thenReturn(csvContent);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"cycle_report_1.csv\""))
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(content().string(csvContent));
        }
    }

    @Nested
    @DisplayName("PDF Export Tests")
    class PdfExportTests {

        @Test
        @WithMockUser(username = "testuser", roles = "MEMBER")
        @DisplayName("Should export PDF with correct content type")
        void shouldExportPdfWithCorrectContentType() throws Exception {
            // Given
            byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46}; // PDF header
            when(reportService.exportCycleReportPdf(1L)).thenReturn(pdfContent);

            // When & Then
            mockMvc.perform(get("/api/reports/cycle/1/export/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"cycle_report_1.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(pdfContent));
        }
    }

    // Helper method to create sample report
    private EnhancedCycleReportDTO createSampleEnhancedReport() {
        RiskDistributionDTO riskDist = RiskDistributionDTO.builder()
                .lowRiskCount(1)
                .mediumRiskCount(1)
                .highRiskCount(1)
                .criticalRiskCount(0)
                .averageRiskScore(45.0)
                .maxRiskScore(65.0)
                .minRiskScore(20.0)
                .build();

        return EnhancedCycleReportDTO.builder()
                .cycleId(1L)
                .cycleName("Q1 2026")
                .projectName("Test Project")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 2, 12))
                .totalPitches(3)
                .completedPitches(2)
                .inProgressPitches(1)
                .notStartedPitches(0)
                .totalAppetiteHours(96.0)
                .totalActualHours(85.5)
                .varianceHours(-10.5)
                .variancePercentage(-10.94)
                .efficiencyPercentage(89.06)
                .totalTasks(10)
                .completedTasks(7)
                .totalTaskEstimateHours(50.0)
                .totalTaskActualHours(45.0)
                .riskDistribution(riskDist)
                .totalTeamMembers(3)
                .averageHoursPerMember(28.5)
                .maxHoursPerMember(45.0)
                .minHoursPerMember(12.0)
                .pitchReports(Collections.emptyList())
                .memberReports(Collections.emptyList())
                .topPerformers(Arrays.asList("Alice Johnson", "Bob Smith"))
                .overBudgetPitches(Collections.singletonList("API Integration"))
                .build();
    }
}
