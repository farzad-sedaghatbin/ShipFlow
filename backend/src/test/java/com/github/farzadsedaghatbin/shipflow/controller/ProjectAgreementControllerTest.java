package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementDTO;
import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementRequest;
import com.github.farzadsedaghatbin.shipflow.service.ProjectAgreementService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ProjectAgreementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProjectAgreementService projectAgreementService;

  // ProjectAgreementController delegates project-membership auth to ProjectService
  // (requireProjectAccess) in addition to the role-based @PreAuthorize check — mock it out
  // the same way BurndownControllerTest does, rather than exercising the real bean here.
  @MockBean
  private ProjectService projectService;

  @Test
  void listAgreements_returns200WithList() throws Exception {
    var dto = ProjectAgreementDTO.builder()
        .id(1L)
        .projectId(1L)
        .title("Sprint review cadence")
        .content("Every other Friday at 2pm.")
        .agreedDate(LocalDate.of(2026, 4, 1))
        .createdByName("sara")
        .createdAt(LocalDateTime.now())
        .build();

    when(projectAgreementService.listByProject(1L)).thenReturn(List.of(dto));

    mockMvc.perform(get("/api/projects/1/agreements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Sprint review cadence"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void createAgreement_validRequest_returns201() throws Exception {
    var request = ProjectAgreementRequest.builder()
        .title("On-call rotation ownership")
        .content("Payments team owns payments on-call this cycle.")
        .build();

    var dto = ProjectAgreementDTO.builder()
        .id(2L)
        .projectId(1L)
        .title("On-call rotation ownership")
        .content("Payments team owns payments on-call this cycle.")
        .agreedDate(LocalDate.now())
        .createdByName("admin")
        .createdAt(LocalDateTime.now())
        .build();

    when(projectAgreementService.create(eq(1L), any())).thenReturn(dto);

    mockMvc.perform(post("/api/projects/1/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("On-call rotation ownership"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void createAgreement_blankTitle_returns400() throws Exception {
    var request = ProjectAgreementRequest.builder()
        .title("")
        .content("Content")
        .build();

    mockMvc.perform(post("/api/projects/1/agreements")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void updateAgreement_validRequest_returns200() throws Exception {
    var request = ProjectAgreementRequest.builder()
        .title("Updated title")
        .content("Updated content")
        .build();

    var dto = ProjectAgreementDTO.builder()
        .id(2L)
        .projectId(1L)
        .title("Updated title")
        .content("Updated content")
        .agreedDate(LocalDate.now())
        .build();

    when(projectAgreementService.update(eq(1L), eq(2L), any())).thenReturn(dto);

    mockMvc.perform(put("/api/projects/1/agreements/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated title"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void deleteAgreement_returns204() throws Exception {
    doNothing().when(projectAgreementService).delete(1L, 2L);

    mockMvc.perform(delete("/api/projects/1/agreements/2"))
        .andExpect(status().isNoContent());

    verify(projectAgreementService).delete(1L, 2L);
  }
}
