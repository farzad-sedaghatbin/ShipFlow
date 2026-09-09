package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.ReleaseReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.ReleaseReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReleaseReportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ReleaseReportService releaseReportService;
  @MockBean private ProjectService projectService;

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void getReleaseReport_authenticated_viewer_returns200() throws Exception {
    when(releaseReportService.computeReleaseReport(1L)).thenReturn(List.of());
    mockMvc.perform(get("/api/projects/1/release-report")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getReleaseReport_projectAccessDenied_returns403() throws Exception {
    doThrow(new AccessDeniedException("Access denied"))
        .when(projectService)
        .requireProjectAccess(1L);

    mockMvc.perform(get("/api/projects/1/release-report")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getReleaseReport_happyPath_returns200WithReleasePoints() throws Exception {
    when(releaseReportService.computeReleaseReport(5L))
        .thenReturn(
            List.of(
                ReleaseReportDTO.builder()
                    .releaseId(1L)
                    .releaseName("Release 1")
                    .version("v1.0.0")
                    .status(ReleaseStatus.RELEASED)
                    .taskCount(4)
                    .completedTaskCount(4)
                    .plannedPoints(16)
                    .completedPoints(16)
                    .build(),
                ReleaseReportDTO.builder()
                    .releaseId(2L)
                    .releaseName("Release 2")
                    .version("v2.0.0")
                    .status(ReleaseStatus.PLANNING)
                    .taskCount(0)
                    .completedTaskCount(0)
                    .plannedPoints(0)
                    .completedPoints(0)
                    .build()));

    mockMvc
        .perform(get("/api/projects/5/release-report"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].releaseName").value("Release 1"))
        .andExpect(jsonPath("$[0].taskCount").value(4))
        .andExpect(jsonPath("$[0].completedPoints").value(16))
        .andExpect(jsonPath("$[1].taskCount").value(0));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getReleaseReport_noReleases_returnsEmptyList() throws Exception {
    when(releaseReportService.computeReleaseReport(7L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/projects/7/release-report"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void getReleaseReport_releaseWithZeroTasks_returns200() throws Exception {
    when(releaseReportService.computeReleaseReport(3L))
        .thenReturn(
            List.of(
                ReleaseReportDTO.builder()
                    .releaseId(9L)
                    .releaseName("Empty Release")
                    .taskCount(0)
                    .completedTaskCount(0)
                    .plannedPoints(0)
                    .completedPoints(0)
                    .build()));

    mockMvc
        .perform(get("/api/projects/3/release-report"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].taskCount").value(0));
  }
}
