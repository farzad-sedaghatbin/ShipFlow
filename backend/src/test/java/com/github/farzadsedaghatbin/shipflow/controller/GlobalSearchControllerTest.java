package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.farzadsedaghatbin.shipflow.dto.GlobalSearchResultDTO;
import com.github.farzadsedaghatbin.shipflow.service.GlobalSearchService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class GlobalSearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GlobalSearchService globalSearchService;
  @MockBean private ProjectService projectService;

  @Test
  void globalSearch_WithValidParams_ShouldReturn200() throws Exception {
    List<GlobalSearchResultDTO> results =
        Arrays.asList(
            GlobalSearchResultDTO.builder()
                .entityType("TASK")
                .entityId(1L)
                .title("Login Page")
                .subtitle("IN_PROGRESS")
                .route("/backlog/1")
                .score(0.85)
                .matchedBy("TRIGRAM")
                .build(),
            GlobalSearchResultDTO.builder()
                .entityType("BUG_REPORT")
                .entityId(42L)
                .title("Button broken")
                .subtitle("BUG-42 · NEW")
                .route("/qa/bug-reports/42")
                .score(1.0)
                .matchedBy("EXACT_KEY")
                .build());

    when(globalSearchService.search("login", 5L, 10)).thenReturn(results);

    mockMvc
        .perform(get("/api/search/global").param("q", "login").param("projectId", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].entityType").value("TASK"))
        .andExpect(jsonPath("$[0].entityId").value(1))
        .andExpect(jsonPath("$[0].title").value("Login Page"))
        .andExpect(jsonPath("$[0].subtitle").value("IN_PROGRESS"))
        .andExpect(jsonPath("$[0].route").value("/backlog/1"))
        .andExpect(jsonPath("$[0].score").value(0.85))
        .andExpect(jsonPath("$[0].matchedBy").value("TRIGRAM"))
        .andExpect(jsonPath("$[1].entityType").value("BUG_REPORT"))
        .andExpect(jsonPath("$[1].matchedBy").value("EXACT_KEY"));
  }

  @Test
  void globalSearch_WithQueryTooShort_ShouldReturn400() throws Exception {
    mockMvc
        .perform(get("/api/search/global").param("q", "a").param("projectId", "5"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(globalSearchService);
  }

  @Test
  void globalSearch_WithEmptyQuery_ShouldReturn400() throws Exception {
    mockMvc
        .perform(get("/api/search/global").param("q", "").param("projectId", "5"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(globalSearchService);
  }

  @Test
  void globalSearch_WithCustomLimit_ShouldPassLimitToService() throws Exception {
    when(globalSearchService.search("test", 3L, 25)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/search/global")
                .param("q", "test")
                .param("projectId", "3")
                .param("limit", "25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(globalSearchService).search("test", 3L, 25);
  }

  @Test
  void globalSearch_WithLimitExceeding50_ShouldDefaultTo10() throws Exception {
    when(globalSearchService.search("test", 3L, 10)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/search/global")
                .param("q", "test")
                .param("projectId", "3")
                .param("limit", "100"))
        .andExpect(status().isOk());

    verify(globalSearchService).search("test", 3L, 10);
  }

  @Test
  void globalSearch_WithLimitBelowOne_ShouldDefaultTo10() throws Exception {
    when(globalSearchService.search("test", 3L, 10)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/search/global")
                .param("q", "test")
                .param("projectId", "3")
                .param("limit", "0"))
        .andExpect(status().isOk());

    verify(globalSearchService).search("test", 3L, 10);
  }

  @Test
  void globalSearch_WithDefaultLimit_ShouldUse10() throws Exception {
    when(globalSearchService.search("test", 1L, 10)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/search/global").param("q", "test").param("projectId", "1"))
        .andExpect(status().isOk());

    verify(globalSearchService).search("test", 1L, 10);
  }

  @Test
  void globalSearch_WithNoResults_ShouldReturnEmptyArray() throws Exception {
    when(globalSearchService.search("nonexistent", 1L, 10)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/search/global").param("q", "nonexistent").param("projectId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void globalSearch_WithMissingProjectId_ShouldReturnError() throws Exception {
    mockMvc
        .perform(get("/api/search/global").param("q", "test"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void globalSearch_WithMissingQuery_ShouldReturnError() throws Exception {
    mockMvc
        .perform(get("/api/search/global").param("projectId", "1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void globalSearch_WithAllEntityTypes_ShouldReturnAll() throws Exception {
    List<GlobalSearchResultDTO> results =
        Arrays.asList(
            GlobalSearchResultDTO.builder()
                .entityType("TASK")
                .entityId(1L)
                .title("Task")
                .subtitle("TODO")
                .route("/backlog/1")
                .score(0.9)
                .matchedBy("TRIGRAM")
                .build(),
            GlobalSearchResultDTO.builder()
                .entityType("SUBTASK")
                .entityId(2L)
                .title("Subtask")
                .subtitle("IN_PROGRESS")
                .route("/backlog/2")
                .score(0.8)
                .matchedBy("TRIGRAM")
                .build(),
            GlobalSearchResultDTO.builder()
                .entityType("BUG_REPORT")
                .entityId(3L)
                .title("Bug")
                .subtitle("BUG-3 · NEW")
                .route("/qa/bug-reports/3")
                .score(0.7)
                .matchedBy("EXACT_KEY")
                .build(),
            GlobalSearchResultDTO.builder()
                .entityType("PITCH")
                .entityId(4L)
                .title("Pitch")
                .subtitle("SHAPED")
                .route("/pitches/4")
                .score(0.6)
                .matchedBy("TRIGRAM")
                .build(),
            GlobalSearchResultDTO.builder()
                .entityType("EPIC")
                .entityId(5L)
                .title("Epic")
                .subtitle("IN_PROGRESS")
                .route("/epics/5")
                .score(0.5)
                .matchedBy("TRIGRAM")
                .build());

    when(globalSearchService.search("test", 1L, 10)).thenReturn(results);

    mockMvc
        .perform(get("/api/search/global").param("q", "test").param("projectId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].entityType").value("TASK"))
        .andExpect(jsonPath("$[1].entityType").value("SUBTASK"))
        .andExpect(jsonPath("$[2].entityType").value("BUG_REPORT"))
        .andExpect(jsonPath("$[3].entityType").value("PITCH"))
        .andExpect(jsonPath("$[4].entityType").value("EPIC"));
  }
}
