package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.BurndownPointDTO;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.service.BurndownService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import java.time.LocalDate;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class BurndownControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private BurndownService burndownService;
  @MockBean private ProjectService projectService;

  @Test
  void getBurndown_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/cycles/1/burndown")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_projectAccessDenied_returns403() throws Exception {
    when(burndownService.resolveProjectId(1L)).thenReturn(10L);
    doThrow(new AccessDeniedException("Access denied"))
        .when(projectService)
        .requireProjectAccess(10L);

    mockMvc.perform(get("/api/cycles/1/burndown")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_cycleNotFound_returns404() throws Exception {
    when(burndownService.resolveProjectId(99L))
        .thenThrow(new ResourceNotFoundException("Cycle not found with id: 99"));

    mockMvc.perform(get("/api/cycles/99/burndown")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_happyPath_returns200WithSeries() throws Exception {
    LocalDate today = LocalDate.now();
    when(burndownService.resolveProjectId(2L)).thenReturn(5L);
    when(burndownService.computeBurndown(2L))
        .thenReturn(
            List.of(
                BurndownPointDTO.builder()
                    .date(today.minusDays(1))
                    .remainingPoints(10)
                    .idealPoints(10)
                    .build(),
                BurndownPointDTO.builder()
                    .date(today)
                    .remainingPoints(5)
                    .idealPoints(5)
                    .build()));

    mockMvc
        .perform(get("/api/cycles/2/burndown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].remainingPoints").value(10))
        .andExpect(jsonPath("$[0].idealPoints").value(10))
        .andExpect(jsonPath("$[1].remainingPoints").value(5));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  void getBurndown_noTasks_returnsEmptyList() throws Exception {
    when(burndownService.resolveProjectId(3L)).thenReturn(7L);
    when(burndownService.computeBurndown(3L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/cycles/3/burndown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
