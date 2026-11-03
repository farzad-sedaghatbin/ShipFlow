package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.service.EpicDependencyService;
import com.github.farzadsedaghatbin.shipflow.service.EpicService;
import com.github.farzadsedaghatbin.shipflow.service.InitiativeService;
import com.github.farzadsedaghatbin.shipflow.service.ProjectService;
import com.github.farzadsedaghatbin.shipflow.service.RoadmapService;
import com.github.farzadsedaghatbin.shipflow.service.TaskService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools.RoadmapMcpTools;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoadmapMcpToolsTest {

  @Mock private RoadmapService roadmapService;
  @Mock private EpicService epicService;
  @Mock private InitiativeService initiativeService;
  @Mock private EpicDependencyService epicDependencyService;
  @Mock private TaskService taskService;
  @Mock private ProjectService projectService;

  private RoadmapMcpTools tools;

  @BeforeEach
  void setUp() {
    tools = new RoadmapMcpTools(roadmapService, epicService, initiativeService, epicDependencyService,
        taskService, projectService);
  }

  @Test
  void getRoadmap_defaultsWindowToThreeMonthsBackTwelveForward() {
    tools.getRoadmap(Map.of("projectId", 3));

    ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
    verify(roadmapService).getRoadmapTimeline(eq(3L), start.capture(), end.capture());
    assertThat(start.getValue()).isEqualTo(LocalDate.now().minusMonths(3));
    assertThat(end.getValue()).isEqualTo(LocalDate.now().plusMonths(12));
    verify(projectService).requireProjectAccess(3L);
  }

  @Test
  void getRoadmap_acceptsSnakeCaseAliasesAndExplicitDates() {
    tools.getRoadmap(Map.of("project_id", "4", "start_date", "2026-01-01", "end_date", "2026-06-30"));

    verify(roadmapService).getRoadmapTimeline(4L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
  }

  @Test
  void getRoadmap_rejectsBadDatesAndMissingProject() {
    assertThatThrownBy(() -> tools.getRoadmap(Map.of("projectId", 1, "startDate", "01/02/2026")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ISO date");
    assertThatThrownBy(() -> tools.getRoadmap(Map.of("projectId", 1, "startDate", "2026-05-01", "endDate",
        "2026-01-01"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("startDate");
    assertThatThrownBy(() -> tools.getRoadmap(Map.of())).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("projectId");
  }

  @Test
  void createEpic_mapsAllFields() {
    Map<String, Object> args = new HashMap<>();
    args.put("projectId", 2);
    args.put("name", "  Payments  ");
    args.put("description", "d");
    args.put("startDate", "2026-11-01");
    args.put("endDate", "2026-12-01");
    args.put("initiativeId", 8);
    args.put("color", "#112233");
    args.put("priority", "high");
    when(epicService.createEpic(any())).thenReturn(EpicDTO.builder().id(1L).build());

    tools.createEpic(args);

    ArgumentCaptor<CreateEpicRequest> cap = ArgumentCaptor.forClass(CreateEpicRequest.class);
    verify(epicService).createEpic(cap.capture());
    CreateEpicRequest r = cap.getValue();
    assertThat(r.getProjectId()).isEqualTo(2L);
    assertThat(r.getName()).isEqualTo("Payments");
    assertThat(r.getTargetStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(r.getTargetEndDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    assertThat(r.getInitiativeId()).isEqualTo(8L);
    assertThat(r.getPriority()).isEqualTo(BusinessValue.HIGH);
  }

  @Test
  void createEpic_validatesNameDatesAndPriority() {
    assertThatThrownBy(() -> tools.createEpic(Map.of("projectId", 2))).hasMessageContaining("name");
    assertThatThrownBy(() -> tools.createEpic(
        Map.of("projectId", 2, "name", "x", "startDate", "2026-02-01", "endDate", "2026-01-01")))
        .hasMessageContaining("startDate");
    assertThatThrownBy(() -> tools.createEpic(Map.of("projectId", 2, "name", "x", "priority", "huge")))
        .hasMessageContaining("Invalid priority");
    verify(epicService, never()).createEpic(any());
  }

  @Test
  void updateEpic_isPatch_onlyProvidedFieldsChange() {
    EpicDTO current = EpicDTO.builder().id(5L).projectId(2L).name("Old").description("keep")
        .status(EpicStatus.PLANNED).color("#000000").targetStartDate(LocalDate.of(2026, 1, 1))
        .targetEndDate(LocalDate.of(2026, 2, 1)).initiativeId(9L).priority(BusinessValue.LOW).build();
    when(epicService.getEpicById(5L)).thenReturn(current);

    tools.updateEpic(Map.of("epicId", 5, "endDate", "2026-03-01", "status", "IN_PROGRESS"));

    ArgumentCaptor<CreateEpicRequest> cap = ArgumentCaptor.forClass(CreateEpicRequest.class);
    verify(epicService).updateEpic(eq(5L), cap.capture());
    CreateEpicRequest r = cap.getValue();
    assertThat(r.getName()).isEqualTo("Old");
    assertThat(r.getDescription()).isEqualTo("keep");
    assertThat(r.getInitiativeId()).isEqualTo(9L);
    assertThat(r.getTargetStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(r.getTargetEndDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(r.getStatus()).isEqualTo(EpicStatus.IN_PROGRESS);
    assertThat(r.getPriority()).isEqualTo(BusinessValue.LOW);
  }

  @Test
  void updateEpic_canDetachInitiativeWithNull() {
    EpicDTO current = EpicDTO.builder().id(5L).projectId(2L).name("E").initiativeId(9L).build();
    when(epicService.getEpicById(5L)).thenReturn(current);
    Map<String, Object> args = new HashMap<>();
    args.put("epicId", 5);
    args.put("initiativeId", null);

    tools.updateEpic(args);

    ArgumentCaptor<CreateEpicRequest> cap = ArgumentCaptor.forClass(CreateEpicRequest.class);
    verify(epicService).updateEpic(eq(5L), cap.capture());
    assertThat(cap.getValue().getInitiativeId()).isNull();
  }

  @Test
  void addEpicDependency_defaultsToBlocksAndRequiresSameProject() {
    when(epicService.getEpicById(1L)).thenReturn(EpicDTO.builder().id(1L).projectId(10L).build());
    when(epicService.getEpicById(2L)).thenReturn(EpicDTO.builder().id(2L).projectId(10L).build());
    when(epicService.getEpicById(3L)).thenReturn(EpicDTO.builder().id(3L).projectId(11L).build());

    tools.addEpicDependency(Map.of("epicId", 1, "targetEpicId", 2));

    ArgumentCaptor<CreateEpicDependencyRequest> cap = ArgumentCaptor.forClass(CreateEpicDependencyRequest.class);
    verify(epicDependencyService).addDependency(eq(1L), cap.capture());
    assertThat(cap.getValue().getTargetEpicId()).isEqualTo(2L);
    assertThat(cap.getValue().getDependencyType()).isEqualTo(DependencyType.BLOCKS);

    assertThatThrownBy(() -> tools.addEpicDependency(Map.of("epicId", 1, "targetEpicId", 3)))
        .hasMessageContaining("same project");
  }

  @Test
  void linkTaskToEpic_nullEpicUnlinks() {
    when(taskService.getTaskById(7L)).thenReturn(TaskDTO.builder().id(7L).projectId(10L).build());
    when(taskService.setTaskEpic(eq(7L), any())).thenReturn(TaskDTO.builder().id(7L).build());

    Map<String, Object> args = new HashMap<>();
    args.put("taskId", 7);
    args.put("epicId", null);
    tools.linkTaskToEpic(args);

    verify(taskService).setTaskEpic(7L, null);
    verify(projectService).requireProjectAccess(10L);
  }

  @Test
  void definitions_steerLlmsTowardEpicsForRoadmaps() {
    assertThat((String) RoadmapMcpTools.createEpicDefinition().get("description"))
        .contains("create_task is for backlog work items");
    assertThat((String) RoadmapMcpTools.createInitiativeDefinition().get("description"))
        .contains("Do NOT use create_task");
    assertThat((String) RoadmapMcpTools.addEpicDependencyDefinition().get("description")).contains("BLOCKS");
  }
}
