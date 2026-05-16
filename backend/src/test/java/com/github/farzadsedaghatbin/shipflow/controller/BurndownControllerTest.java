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

// Note: @Transactional has no effect when all beans are @MockBean — removed.
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BurndownControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private BurndownService burndownService;

  @Test
  void getBurndown_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/cycles/1/burndown")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_projectAccessDenied_returns403() throws Exception {
    // Project-scope auth is now enforced inside BurndownService.computeBurndown
    doThrow(new AccessDeniedException("Access denied"))
        .when(burndownService)
        .computeBurndown(1L);

    mockMvc.perform(get("/api/cycles/1/burndown")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_cycleNotFound_returns404() throws Exception {
    doThrow(new ResourceNotFoundException("Cycle not found with id: 99"))
        .when(burndownService)
        .computeBurndown(99L);

    mockMvc.perform(get("/api/cycles/99/burndown")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  void getBurndown_happyPath_returns200WithSeries() throws Exception {
    LocalDate today = LocalDate.now();
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
    when(burndownService.computeBurndown(3L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/cycles/3/burndown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
