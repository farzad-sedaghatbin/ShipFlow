package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.farzadsedaghatbin.shipflow.dto.snapshot.ProjectSnapshotDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.ProjectSnapshot;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleNarrativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectSnapshotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectSnapshotServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private CycleNarrativeRepository cycleNarrativeRepository;
  @Mock private InitiativeRepository initiativeRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private ProjectSnapshotRepository snapshotRepository;

  private ProjectSnapshotService service;

  private static final Long PROJECT_ID = 1L;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    service =
        new ProjectSnapshotService(
            projectRepository,
            cycleRepository,
            cycleNarrativeRepository,
            initiativeRepository,
            epicRepository,
            snapshotRepository,
            mapper);
  }

  // ── Helper builders ───────────────────────────────────────────────────────

  private Project project(Long id) {
    Project p = new Project();
    p.setId(id);
    p.setName("Test Project");
    p.setProjectType(ProjectType.SHAPE_UP);
    return p;
  }

  private Cycle completedCycle(Long id, int pitchCount, int doneCount) {
    List<Pitch> pitches = new ArrayList<>();
    for (int i = 0; i < pitchCount; i++) {
      Pitch pitch = new Pitch();
      pitch.setId((long) (id * 100 + i));
      pitch.setTitle("Pitch " + i);
      pitch.setStatus(i < doneCount ? PitchStatus.DONE : PitchStatus.STARTED);
      pitches.add(pitch);
    }
    return Cycle.builder()
        .id(id)
        .name("Cycle " + id)
        .startDate(LocalDate.now().minusDays(60))
        .endDate(LocalDate.now().minusDays(30))
        .phase(CyclePhase.BETTING_COOLDOWN)
        .isActive(false)
        .pitches(pitches)
        .build();
  }

  private Cycle activeCycle(Long id, int pitchCount, int doneCount) {
    List<Pitch> pitches = new ArrayList<>();
    for (int i = 0; i < pitchCount; i++) {
      Pitch pitch = new Pitch();
      pitch.setId((long) (id * 100 + i));
      pitch.setTitle("Pitch " + i);
      pitch.setStatus(i < doneCount ? PitchStatus.DONE : PitchStatus.IN_PROGRESS);
      pitches.add(pitch);
    }
    return Cycle.builder()
        .id(id)
        .name("Active Cycle")
        .startDate(LocalDate.now().minusDays(10))
        .endDate(LocalDate.now().plusDays(20))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true)
        .pitches(pitches)
        .build();
  }

  private Initiative activeInitiative(Long id, String name) {
    Initiative i = new Initiative();
    i.setId(id);
    i.setName(name);
    i.setStatus(InitiativeStatus.IN_PROGRESS);
    return i;
  }

  private Initiative completedInitiative(Long id, String name) {
    Initiative i = new Initiative();
    i.setId(id);
    i.setName(name);
    i.setStatus(InitiativeStatus.COMPLETED);
    return i;
  }

  private Epic activeEpic(Long id, String name) {
    Epic e = new Epic();
    e.setId(id);
    e.setName(name);
    e.setStatus(EpicStatus.IN_PROGRESS);
    return e;
  }

  // ── Test 1: empty project returns defaults ────────────────────────────────

  @Test
  void computeAndStore_emptyProject_returnsDefaults() {
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of());
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(
            anyLong(), any()))
        .thenReturn(List.of());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectSnapshotDTO dto = service.computeAndStore(PROJECT_ID);

    assertThat(dto.getCyclesCompleted()).isEqualTo(0);
    assertThat(dto.getAvgPitchesPerCycle()).isEqualTo(0.0);
    assertThat(dto.getAvgCompletionRate()).isEqualTo(0.0);
    assertThat(dto.getTypicalAppetiteDays()).isEqualTo(14);
    assertThat(dto.getRiskPosture()).isEqualTo("MEDIUM");
    assertThat(dto.getRoadmapThemes()).isEmpty();
    assertThat(dto.getActiveCycleName()).isNull();
  }

  // ── Test 2: velocity calculation from 3 completed cycles ─────────────────

  @Test
  void computeAndStore_withCycles_calculatesVelocity() {
    // 3 completed cycles, 4 pitches each, 2 done / 2 not → avgCompletionRate = 0.5
    Cycle c1 = completedCycle(1L, 4, 2);
    Cycle c2 = completedCycle(2L, 4, 2);
    Cycle c3 = completedCycle(3L, 4, 2);

    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID))
        .thenReturn(List.of(c1, c2, c3));
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of());
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(anyLong(), any()))
        .thenReturn(List.of());
    when(cycleNarrativeRepository.findByCycleIdAndNarrativeType(anyLong(), any()))
        .thenReturn(Optional.empty());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectSnapshotDTO dto = service.computeAndStore(PROJECT_ID);

    assertThat(dto.getCyclesCompleted()).isEqualTo(3);
    assertThat(dto.getAvgPitchesPerCycle()).isEqualTo(4.0);
    assertThat(dto.getAvgCompletionRate()).isEqualTo(0.5);
  }

  // ── Test 3: active cycle section is populated ─────────────────────────────

  @Test
  void computeAndStore_withActiveCycle_populatesActiveCycleSection() {
    Cycle active = activeCycle(10L, 4, 2); // 2 done out of 4 → 50%

    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of(active));
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(anyLong(), any()))
        .thenReturn(List.of());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectSnapshotDTO dto = service.computeAndStore(PROJECT_ID);

    assertThat(dto.getActiveCycleName()).isEqualTo("Active Cycle");
    assertThat(dto.getActiveCyclePitchCount()).isEqualTo(4);
    assertThat(dto.getActiveCycleCompletionPercent()).isEqualTo(50);
    assertThat(dto.getActiveCycleDaysRemaining()).isGreaterThan(0);
  }

  // ── Test 4: roadmap themes from initiatives and epics ────────────────────

  @Test
  void computeAndStore_withInitiativesAndEpics_populatesRoadmapThemes() {
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of());
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID))
        .thenReturn(List.of(activeInitiative(1L, "Mobile Redesign"), activeInitiative(2L, "API Overhaul")));
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID))
        .thenReturn(List.of(activeEpic(1L, "Checkout Flow"), activeEpic(2L, "Search Indexing")));
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(anyLong(), any()))
        .thenReturn(List.of());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectSnapshotDTO dto = service.computeAndStore(PROJECT_ID);

    assertThat(dto.getRoadmapThemes()).contains("Mobile Redesign", "API Overhaul", "Checkout Flow", "Search Indexing");
  }

  // ── Test 5: completed and cancelled initiatives are excluded ─────────────

  @Test
  void computeAndStore_completedInitiativesExcluded() {
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of());
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID))
        .thenReturn(
            List.of(
                activeInitiative(1L, "Active Theme"),
                completedInitiative(2L, "Completed Theme"),
                cancelledInitiative(3L, "Cancelled Theme")));
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(anyLong(), any()))
        .thenReturn(List.of());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProjectSnapshotDTO dto = service.computeAndStore(PROJECT_ID);

    assertThat(dto.getRoadmapThemes()).containsOnly("Active Theme");
    assertThat(dto.getRoadmapThemes()).doesNotContain("Completed Theme", "Cancelled Theme");
  }

  // ── Test 6: buildPromptBlock returns non-empty string ─────────────────────

  @Test
  void buildPromptBlock_returnsNonEmptyString() {
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(PROJECT_ID)).thenReturn(List.of());
    when(cycleRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID)).thenReturn(List.of());
    when(initiativeRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(epicRepository.findByProjectIdNotDeleted(PROJECT_ID)).thenReturn(List.of());
    when(cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(anyLong(), any()))
        .thenReturn(List.of());
    when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    String block = service.buildPromptBlock(PROJECT_ID);

    assertThat(block).isNotBlank();
    assertThat(block).contains("=== Project Context ===");
    assertThat(block).contains("Test Project");
  }

  // ── Test 7: buildPromptBlock never throws ─────────────────────────────────

  @Test
  void buildPromptBlock_neverThrows() {
    when(projectRepository.findById(PROJECT_ID))
        .thenThrow(new RuntimeException("DB connection lost"));

    String block = service.buildPromptBlock(PROJECT_ID);

    assertThat(block).isEmpty();
  }

  // ── Test 8: active cycle line omitted when null ───────────────────────────

  @Test
  void renderPromptBlock_omitsActiveCycleLineWhenNull() {
    ProjectSnapshotDTO dto =
        ProjectSnapshotDTO.builder()
            .projectId(PROJECT_ID)
            .projectName("My Project")
            .projectType("SHAPE_UP")
            .computedAt(LocalDateTime.now())
            .cyclesCompleted(0)
            .avgPitchesPerCycle(0.0)
            .avgCompletionRate(0.0)
            .typicalAppetiteDays(14)
            .recentShipped(List.of())
            .roadmapThemes(List.of())
            .knownPatterns(List.of())
            .riskPosture("MEDIUM")
            // activeCycleName is null
            .build();

    String block = service.renderPromptBlock(dto);

    assertThat(block).doesNotContain("Active cycle:");
    assertThat(block).contains("=== Project Context ===");
    assertThat(block).contains("========================");
  }

  // ── Test 9: roadmap line omitted when empty ───────────────────────────────

  @Test
  void renderPromptBlock_omitsRoadmapLineWhenEmpty() {
    ProjectSnapshotDTO dto =
        ProjectSnapshotDTO.builder()
            .projectId(PROJECT_ID)
            .projectName("My Project")
            .projectType("SHAPE_UP")
            .computedAt(LocalDateTime.now())
            .cyclesCompleted(0)
            .avgPitchesPerCycle(0.0)
            .avgCompletionRate(0.0)
            .typicalAppetiteDays(14)
            .recentShipped(List.of())
            .roadmapThemes(List.of())
            .knownPatterns(List.of())
            .riskPosture("MEDIUM")
            .build();

    String block = service.renderPromptBlock(dto);

    assertThat(block).doesNotContain("Roadmap themes:");
  }

  // ── Test 10: patterns line omitted when empty ─────────────────────────────

  @Test
  void renderPromptBlock_omitsPatternsLineWhenEmpty() {
    ProjectSnapshotDTO dto =
        ProjectSnapshotDTO.builder()
            .projectId(PROJECT_ID)
            .projectName("My Project")
            .projectType("SHAPE_UP")
            .computedAt(LocalDateTime.now())
            .cyclesCompleted(2)
            .avgPitchesPerCycle(3.0)
            .avgCompletionRate(0.75)
            .typicalAppetiteDays(14)
            .recentShipped(List.of())
            .roadmapThemes(List.of("Theme A"))
            .knownPatterns(List.of()) // empty patterns
            .riskPosture("LOW")
            .build();

    String block = service.renderPromptBlock(dto);

    assertThat(block).doesNotContain("Known patterns:");
    assertThat(block).contains("Roadmap themes: Theme A");
    assertThat(block).contains("Risk posture: LOW");
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private Initiative cancelledInitiative(Long id, String name) {
    Initiative i = new Initiative();
    i.setId(id);
    i.setName(name);
    i.setStatus(InitiativeStatus.CANCELLED);
    return i;
  }
}
