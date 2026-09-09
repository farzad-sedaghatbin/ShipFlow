package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.SprintReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.SprintReportService;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
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
class SprintReportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SprintReportService sprintReportService;

  // SprintReportController loads the Cycle from CycleRepository itself and delegates auth to
  // ProjectService. Both beans must be available in the test context.
  @MockBean private ProjectService projectService;
  @MockBean private CycleRepository cycleRepository;

  /** Helper: builds a minimal Cycle with a Project for stubbing CycleRepository. */
  private Cycle cycleWithProject(Long cycleId, Long projectId) {
    Project project = Project.builder().build();
    project.setId(projectId);
    Cycle cycle = new Cycle();
    cycle.setId(cycleId);
    cycle.setProject(project);
    cycle.setStartDate(LocalDate.now().minusDays(3));
    cycle.setEndDate(LocalDate.now().plusDays(4));
    return cycle;
  }

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void getSprintReport_authenticated_viewer_returns200() throws Exception {
    when(cycleRepository.findByIdWithProject(1L))
        .thenReturn(Optional.of(cycleWithProject(1L, 5L)));
    when(sprintReportService.computeSprintReport(any(Cycle.class)))
        .thenReturn(SprintReportDTO.builder().cycleId(1L).build());
    mockMvc.perform(get("/api/cycles/1/sprint-report")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getSprintReport_projectAccessDenied_returns403() throws Exception {
    when(cycleRepository.findByIdWithProject(1L))
        .thenReturn(Optional.of(cycleWithProject(1L, 10L)));
    doThrow(new AccessDeniedException("Access denied"))
        .when(projectService)
        .requireProjectAccess(10L);

    mockMvc.perform(get("/api/cycles/1/sprint-report")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getSprintReport_cycleNotFound_returns404() throws Exception {
    when(cycleRepository.findByIdWithProject(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/cycles/99/sprint-report")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getSprintReport_happyPath_returns200WithReport() throws Exception {
    when(cycleRepository.findByIdWithProject(2L))
        .thenReturn(Optional.of(cycleWithProject(2L, 5L)));
    when(sprintReportService.computeSprintReport(any(Cycle.class)))
        .thenReturn(
            SprintReportDTO.builder()
                .cycleId(2L)
                .cycleName("Sprint 2")
                .plannedPoints(16)
                .completedPoints(8)
                .completionRate(0.5)
                .taskCountByStatus(Map.of("DONE", 2, "TODO", 1))
                .scopeAddedTaskCount(1)
                .scopeAddedPoints(3)
                .build());

    mockMvc
        .perform(get("/api/cycles/2/sprint-report"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleName").value("Sprint 2"))
        .andExpect(jsonPath("$.plannedPoints").value(16))
        .andExpect(jsonPath("$.completedPoints").value(8))
        .andExpect(jsonPath("$.completionRate").value(0.5))
        .andExpect(jsonPath("$.scopeAddedTaskCount").value(1))
        .andExpect(jsonPath("$.scopeAddedPoints").value(3));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getSprintReport_noPlannedPoints_completionRateIsZero() throws Exception {
    when(cycleRepository.findByIdWithProject(3L))
        .thenReturn(Optional.of(cycleWithProject(3L, 5L)));
    when(sprintReportService.computeSprintReport(any(Cycle.class)))
        .thenReturn(
            SprintReportDTO.builder()
                .cycleId(3L)
                .plannedPoints(0)
                .completedPoints(0)
                .completionRate(0.0)
                .build());

    mockMvc
        .perform(get("/api/cycles/3/sprint-report"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completionRate").value(0.0));
  }
}
