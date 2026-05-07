package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.github.CreateGitHubRepositoryRequest;
import com.github.farzadsedaghatbin.shipflow.dto.github.GitHubLinkDTO;
import com.github.farzadsedaghatbin.shipflow.dto.github.GitHubRepositoryDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubIntegrationService;
import java.util.List;
import org.junit.jupiter.api.Disabled;
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

/**
 * Integration tests for GitHubIntegrationController Tests REST API endpoints for
 * GitHub integration using MockMvc with full Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class GitHubIntegrationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private GitHubIntegrationService integrationService;

  @MockBean
  private MessageService messageService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void registerRepository_WithValidRequest_ShouldReturn201() throws Exception {
    // Given
    CreateGitHubRepositoryRequest request = CreateGitHubRepositoryRequest.builder().owner("testorg")
        .name("testrepo").defaultBranch("main").autoLinkEnabled(true).autoCloseTasksOnMerge(true).build();

    // When & Then
    mockMvc.perform(post("/api/github/repositories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "USER")
  void registerRepository_AsNonAdmin_ShouldReturn403() throws Exception {
    // Given
    CreateGitHubRepositoryRequest request = CreateGitHubRepositoryRequest.builder().owner("testorg")
        .name("testrepo").build();

    // When & Then
    mockMvc.perform(post("/api/github/repositories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void getAllRepositories_ShouldReturnList() throws Exception {
    // Given
    GitHubRepositoryDTO repo = GitHubRepositoryDTO.builder().id(1L).owner("testorg").name("testrepo")
        .fullName("testorg/testrepo").isActive(true).build();

    when(integrationService.getAllRepositories()).thenReturn(List.of(repo));

    // When & Then
    mockMvc.perform(get("/api/github/repositories")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fullName").value("testorg/testrepo"));
  }

  @Test
  @WithMockUser
  void getTaskGitHubLinks_ShouldReturnLinks() throws Exception {
    // Given
    GitHubLinkDTO link = GitHubLinkDTO.builder().id(1L).linkType(GitHubLinkType.COMMIT).autoLinked(true).build();

    when(integrationService.getTaskGitHubLinks(anyLong())).thenReturn(List.of(link));

    // When & Then
    mockMvc.perform(get("/api/github/tasks/42/links")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].linkType").value("COMMIT"))
        .andExpect(jsonPath("$[0].autoLinked").value(true));
  }

  @Test
  @WithMockUser
  void getPitchGitHubLinks_ShouldReturnLinks() throws Exception {
    // Given
    GitHubLinkDTO link = GitHubLinkDTO.builder().id(1L).linkType(GitHubLinkType.PULL_REQUEST).build();

    when(integrationService.getPitchGitHubLinks(anyLong())).thenReturn(List.of(link));

    // When & Then
    mockMvc.perform(get("/api/github/pitches/5/links")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].linkType").value("PULL_REQUEST"));
  }

  @Test
  @Disabled("Security filters disabled for controller tests (addFilters=false) - security tested separately")
  void getTaskGitHubLinks_WithoutAuth_ShouldReturn401() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/github/tasks/42/links")).andExpect(status().isUnauthorized());
  }
}
