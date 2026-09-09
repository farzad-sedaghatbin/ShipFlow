package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.BurnupPointDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.service.BurnupService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import java.time.LocalDate;
import java.util.List;
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
class BurnupControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private BurnupService burnupService;

  // BurnupController loads the Cycle from CycleRepository itself and delegates auth to
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
  void getBurnup_authenticated_viewer_returns200() throws Exception {
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(cycleWithProject(1L, 5L)));
    when(burnupService.computeBurnup(any(Cycle.class))).thenReturn(List.of());
    mockMvc.perform(get("/api/cycles/1/burnup")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurnup_projectAccessDenied_returns403() throws Exception {
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(cycleWithProject(1L, 10L)));
    doThrow(new AccessDeniedException("Access denied"))
        .when(projectService)
        .requireProjectAccess(10L);

    mockMvc.perform(get("/api/cycles/1/burnup")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurnup_cycleNotFound_returns404() throws Exception {
    when(cycleRepository.findByIdWithProject(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/cycles/99/burnup")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurnup_happyPath_returns200WithSeries() throws Exception {
    LocalDate today = LocalDate.now();
    when(cycleRepository.findByIdWithProject(2L)).thenReturn(Optional.of(cycleWithProject(2L, 5L)));
    when(burnupService.computeBurnup(any(Cycle.class)))
        .thenReturn(
            List.of(
                BurnupPointDTO.builder()
                    .date(today.minusDays(1))
                    .completedPoints(0)
                    .totalScopePoints(10)
                    .build(),
                BurnupPointDTO.builder()
                    .date(today)
                    .completedPoints(5)
                    .totalScopePoints(10)
                    .build()));

    mockMvc
        .perform(get("/api/cycles/2/burnup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].completedPoints").value(0))
        .andExpect(jsonPath("$[0].totalScopePoints").value(10))
        .andExpect(jsonPath("$[1].completedPoints").value(5));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getBurnup_noTasks_returnsEmptyList() throws Exception {
    when(cycleRepository.findByIdWithProject(3L)).thenReturn(Optional.of(cycleWithProject(3L, 5L)));
    when(burnupService.computeBurnup(any(Cycle.class))).thenReturn(List.of());

    mockMvc
        .perform(get("/api/cycles/3/burnup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
