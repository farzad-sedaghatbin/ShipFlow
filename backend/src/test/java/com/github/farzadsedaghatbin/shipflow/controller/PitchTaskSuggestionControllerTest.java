package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SuggestionSource;
import com.github.farzadsedaghatbin.shipflow.service.PitchTaskSuggestionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link PitchTaskSuggestionController}. Tests REST API endpoints and
 * {@code @PreAuthorize} role gating using MockMvc with full Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class PitchTaskSuggestionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PitchTaskSuggestionService pitchTaskSuggestionService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void generate_AsAdmin_ShouldReturn200() throws Exception {
    TaskSuggestionResponseDTO response = TaskSuggestionResponseDTO.builder()
        .suggestions(List.of(TaskSuggestionDTO.builder()
            .title("Build settings API")
            .description("Backend and mobile collaborate on this.")
            .sourceContext(SuggestionSource.PITCH)
            .disciplines(List.of(Discipline.BACKEND, Discipline.MOBILE))
            .build()))
        .figmaContextUsed(false)
        .build();
    when(pitchTaskSuggestionService.suggestTasks(anyLong())).thenReturn(response);

    mockMvc.perform(post("/api/ai/pitch-task-suggestions/1/generate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions[0].title").value("Build settings API"))
        .andExpect(jsonPath("$.figmaContextUsed").value(false));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void generate_AsMember_ShouldReturn200() throws Exception {
    TaskSuggestionResponseDTO response =
        TaskSuggestionResponseDTO.builder().suggestions(List.of()).figmaContextUsed(false).build();
    when(pitchTaskSuggestionService.suggestTasks(anyLong())).thenReturn(response);

    mockMvc.perform(post("/api/ai/pitch-task-suggestions/1/generate")).andExpect(status().isOk());
  }

  @Test
  void status_NoAuth_ShouldReturn200() throws Exception {
    when(pitchTaskSuggestionService.isAvailable()).thenReturn(true);

    mockMvc.perform(get("/api/ai/pitch-task-suggestions/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true));
  }
}
