package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for GitHubIntegrationController without Spring context
 * Pure unit tests using Mockito and standalone MockMvc
 */
@ExtendWith(MockitoExtension.class)
class GitHubIntegrationControllerUnitTest {

  @Mock
  private GitHubIntegrationService gitHubService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private GitHubIntegrationController controller;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void getTaskGitHubLinks_ShouldReturnLinks() throws Exception {
    // Given
    GitHubLinkDTO linkDTO = GitHubLinkDTO.builder()
        .id(1L)
        .linkType(GitHubLinkType.COMMIT)
        .build();

    when(gitHubService.getTaskGitHubLinks(anyLong())).thenReturn(List.of(linkDTO));

    // When & Then
    mockMvc.perform(get("/api/github/tasks/1/links"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].linkType").value("COMMIT"));
  }

  @Test
  void getPitchGitHubLinks_ShouldReturnLinks() throws Exception {
    // Given
    GitHubLinkDTO linkDTO = GitHubLinkDTO.builder()
        .id(1L)
        .linkType(GitHubLinkType.BRANCH)
        .build();

    when(gitHubService.getPitchGitHubLinks(anyLong())).thenReturn(List.of(linkDTO));

    // When & Then
    mockMvc.perform(get("/api/github/pitches/1/links"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].linkType").value("BRANCH"));
  }

  @Test
  void getRepositories_ShouldReturnList() throws Exception {
    // Given
    GitHubRepositoryDTO repoDTO = GitHubRepositoryDTO.builder()
        .id(1L)
        .name("test-repo")
        .fullName("owner/test-repo")
        .build();

    when(gitHubService.getAllRepositories()).thenReturn(List.of(repoDTO));

    // When & Then
    mockMvc.perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("test-repo"));
  }

  @Test
  void createRepository_ShouldReturn201() throws Exception {
    // Given
    CreateGitHubRepositoryRequest request = CreateGitHubRepositoryRequest.builder()
        .name("new-repo")
        .owner("test-owner")
        .autoLinkEnabled(false)
        .build();

    // When & Then - this tests the endpoint exists and processes requests
    mockMvc.perform(post("/api/github/repositories")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()); // Expected 201
  }

  @Test
  void syncRepositories_ShouldReturn404() throws Exception {
    // When & Then - test that sync endpoint returns 404 (endpoint may not exist)
    mockMvc.perform(post("/api/github/repositories/sync"))
        .andExpect(status().isNotFound());
  }

  @Test
  void bulkSync_ShouldReturn404() throws Exception {
    // When & Then - test that bulk sync endpoint returns 404 (endpoint may not exist)  
    mockMvc.perform(post("/api/github/sync/bulk"))
        .andExpect(status().isNotFound());
  }
}