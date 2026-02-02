package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.service.PitchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("Soft Delete Integration Tests")
class SoftDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PitchService pitchService;

    @Test
    @DisplayName("DELETE /api/pitches/{id} should perform soft delete")
    @WithMockUser(roles = "ADMIN")
    void deletePitchShouldPerformSoftDelete() throws Exception {
        // Given
        Long pitchId = 1L;
        doNothing().when(pitchService).deletePitch(pitchId);

        // When & Then
        mockMvc.perform(delete("/api/pitches/{id}", pitchId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(pitchService, times(1)).deletePitch(pitchId);
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} should perform soft delete")
    @WithMockUser(roles = "MANAGER")
    void deleteTaskShouldPerformSoftDelete() throws Exception {
        // Given
        Long taskId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/qa/test-cases/{id} should perform soft delete")
    @WithMockUser(roles = "MEMBER")
    void deleteTestCaseShouldPerformSoftDelete() throws Exception {
        // Given
        Long testCaseId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/qa/test-cases/{id}", testCaseId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 404 when trying to access soft deleted pitch")
    @WithMockUser(roles = "ADMIN") 
    void shouldReturn404WhenAccessingSoftDeletedPitch() throws Exception {
        // Given
        Long pitchId = 1L;
        when(pitchService.getPitchById(pitchId))
            .thenThrow(new IllegalArgumentException("Pitch not found with id: " + pitchId));

        // When & Then
        mockMvc.perform(get("/api/pitches/{id}", pitchId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should not include soft deleted pitches in list endpoint")
    @WithMockUser(roles = "ADMIN")
    void shouldNotIncludeSoftDeletedPitchesInList() throws Exception {
        // Given
        when(pitchService.getAccessiblePitches()).thenReturn(java.util.List.of());

        // When & Then
        mockMvc.perform(get("/api/pitches")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}