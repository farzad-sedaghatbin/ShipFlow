package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.VelocityPointDTO;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.VelocityService;
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

// Note: @Transactional has no effect when all beans are @MockBean — removed.
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VelocityControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private VelocityService velocityService;
  @MockBean private ProjectService projectService;

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void getVelocity_authenticated_viewer_returns200() throws Exception {
    // RBAC is disabled in the test profile so any authenticated role can call this endpoint.
    // In production, unauthenticated requests are blocked by the JWT filter before reaching
    // method security — that contract is covered by integration tests with filters enabled.
    when(velocityService.computeVelocity(1L)).thenReturn(List.of());
    mockMvc.perform(get("/api/projects/1/velocity")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getVelocity_projectAccessDenied_returns403() throws Exception {
    doThrow(new AccessDeniedException("Access denied"))
        .when(projectService)
        .requireProjectAccess(1L);

    mockMvc.perform(get("/api/projects/1/velocity")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getVelocity_happyPath_returns200WithVelocityPoints() throws Exception {
    when(velocityService.computeVelocity(5L))
        .thenReturn(
            List.of(
                VelocityPointDTO.builder()
                    .cycleId(1L)
                    .cycleName("Sprint 1")
                    .plannedPoints(13)
                    .completedPoints(13)
                    .build(),
                VelocityPointDTO.builder()
                    .cycleId(2L)
                    .cycleName("Sprint 2")
                    .plannedPoints(16)
                    .completedPoints(16)
                    .build()));

    mockMvc
        .perform(get("/api/projects/5/velocity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].cycleName").value("Sprint 1"))
        .andExpect(jsonPath("$[0].plannedPoints").value(13))
        .andExpect(jsonPath("$[0].completedPoints").value(13))
        .andExpect(jsonPath("$[1].cycleName").value("Sprint 2"))
        .andExpect(jsonPath("$[1].completedPoints").value(16));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getVelocity_noSprints_returnsEmptyList() throws Exception {
    when(velocityService.computeVelocity(7L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/projects/7/velocity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void getVelocity_viewerRole_returns200() throws Exception {
    when(velocityService.computeVelocity(3L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/projects/3/velocity"))
        .andExpect(status().isOk());
  }
}
