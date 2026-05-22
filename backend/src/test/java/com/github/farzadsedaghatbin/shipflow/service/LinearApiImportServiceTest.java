package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

/**
 * Pure unit tests for LinearApiImportService — covers mapping helpers.
 * No Spring context, no HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class LinearApiImportServiceTest {

  @Mock private LinearOAuthService linearOAuthService;
  @Mock private ProjectRepository projectRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private ImportJobRepository importJobRepository;
  @Mock private UserRepository userRepository;
  @Mock private PersonRepository personRepository;
  @Mock private RestTemplate restTemplate;

  private LinearApiImportService service;

  @BeforeEach
  void setUp() {
    service =
        new LinearApiImportService(
            linearOAuthService,
            projectRepository,
            cycleRepository,
            epicRepository,
            taskRepository,
            importJobRepository,
            userRepository,
            personRepository,
            restTemplate,
            new ObjectMapper());
  }

  // ── mapLinearStateType ────────────────────────────────────────────────────

  @Test
  @DisplayName("mapLinearStateType: 'backlog' → BACKLOG")
  void stateType_backlog() {
    assertThat(service.mapLinearStateType("backlog")).isEqualTo(TaskStatus.BACKLOG);
  }

  @Test
  @DisplayName("mapLinearStateType: 'unstarted' → TODO")
  void stateType_unstarted() {
    assertThat(service.mapLinearStateType("unstarted")).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapLinearStateType: 'started' → IN_PROGRESS")
  void stateType_started() {
    assertThat(service.mapLinearStateType("started")).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("mapLinearStateType: 'completed' → DONE")
  void stateType_completed() {
    assertThat(service.mapLinearStateType("completed")).isEqualTo(TaskStatus.DONE);
  }

  @Test
  @DisplayName("mapLinearStateType: 'cancelled' → CANCELLED")
  void stateType_cancelled() {
    assertThat(service.mapLinearStateType("cancelled")).isEqualTo(TaskStatus.CANCELLED);
  }

  @Test
  @DisplayName("mapLinearStateType: unknown string → TODO")
  void stateType_unknown() {
    assertThat(service.mapLinearStateType("someWeirdType")).isEqualTo(TaskStatus.TODO);
  }

  @Test
  @DisplayName("mapLinearStateType: null → TODO")
  void stateType_null() {
    assertThat(service.mapLinearStateType(null)).isEqualTo(TaskStatus.TODO);
  }

  // ── mapLinearPriority ─────────────────────────────────────────────────────

  @Test
  @DisplayName("mapLinearPriority: 0 (no priority) → MEDIUM")
  void priority_noPriority() {
    assertThat(service.mapLinearPriority(0)).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapLinearPriority: 1 (urgent) → URGENT")
  void priority_urgent() {
    assertThat(service.mapLinearPriority(1)).isEqualTo(TaskPriority.URGENT);
  }

  @Test
  @DisplayName("mapLinearPriority: 2 (high) → HIGH")
  void priority_high() {
    assertThat(service.mapLinearPriority(2)).isEqualTo(TaskPriority.HIGH);
  }

  @Test
  @DisplayName("mapLinearPriority: 3 (medium) → MEDIUM")
  void priority_medium() {
    assertThat(service.mapLinearPriority(3)).isEqualTo(TaskPriority.MEDIUM);
  }

  @Test
  @DisplayName("mapLinearPriority: 4 (low) → LOW")
  void priority_low() {
    assertThat(service.mapLinearPriority(4)).isEqualTo(TaskPriority.LOW);
  }
}
