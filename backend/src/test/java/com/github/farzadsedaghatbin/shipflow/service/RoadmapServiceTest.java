package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.RoadmapTimelineDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

  @Mock
  private InitiativeRepository initiativeRepository;

  @Mock
  private EpicRepository epicRepository;

  @Mock
  private ReleaseRepository releaseRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private BugReportRepository bugReportRepository;

  @Mock
  private com.github.farzadsedaghatbin.shipflow.repository.EpicDependencyRepository epicDependencyRepository;

  @Mock
  private com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository projectRepository;

  @InjectMocks
  private RoadmapService roadmapService;

  private Project testProject;
  private Initiative testInitiative;
  private Epic testEpic;
  private Release testRelease;
  private Pitch testPitch;

  @BeforeEach
  void setUp() {
    testProject = Project.builder()
        .id(1L)
        .name("Test Project")
        .projectKey("TST")
        .isActive(true)
        .build();

    testPitch = Pitch.builder()
        .id(1L)
        .title("Mobile Checkout")
        .status(PitchStatus.DONE)
        .appetiteDays(5)
        .build();

    testEpic = Epic.builder()
        .id(1L)
        .name("Mobile Checkout Redesign")
        .description("Redesign mobile checkout flow")
        .status(EpicStatus.IN_PROGRESS)
        .color("#8B5CF6")
        .targetStartDate(LocalDate.of(2026, 2, 1))
        .targetEndDate(LocalDate.of(2026, 4, 30))
        .project(testProject)
        .sortOrder(1)
        .pitches(Arrays.asList(testPitch))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testInitiative = Initiative.builder()
        .id(1L)
        .name("Mobile Experience 2026")
        .description("Strategic mobile initiative")
        .status(InitiativeStatus.IN_PROGRESS)
        .color("#3B82F6")
        .targetStartDate(LocalDate.of(2026, 1, 1))
        .targetEndDate(LocalDate.of(2026, 6, 30))
        .project(testProject)
        .sortOrder(1)
        .epics(Arrays.asList(testEpic))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testEpic.setInitiative(testInitiative);

    testRelease = Release.builder()
        .id(1L)
        .name("February 2026 Release")
        .version("v2.4.0")
        .description("Major feature release")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.MEDIUM)
        .targetDate(LocalDate.of(2026, 2, 28))
        .project(testProject)
        .cycles(new HashSet<>())
        .sortOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  void getRoadmapTimeline_ShouldReturnTimelineData() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList(testRelease));
    when(pitchRepository.countByTargetReleaseIdsNotDeleted(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 3L}));
    when(pitchRepository.countByTargetReleaseIdsAndStatusNotDeleted(any(), eq(PitchStatus.DONE))).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 1L}));
    when(taskRepository.countByTargetReleaseIdsNotDeleted(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 5L}));
    when(bugReportRepository.countByTargetReleaseIds(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 2L}));

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    assertThat(result).isNotNull();
    assertThat(result.getInitiatives()).hasSize(1);
    assertThat(result.getReleases()).hasSize(1);
  }

  @Test
  void getRoadmapTimeline_WithEmptyResults_ShouldReturnEmptyLists() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList());
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    assertThat(result).isNotNull();
    assertThat(result.getInitiatives()).isEmpty();
    assertThat(result.getReleases()).isEmpty();
  }

  @Test
  void getRoadmapTimeline_ShouldCorrectlyMapInitiativeFields() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    assertThat(result.getInitiatives()).hasSize(1);
    RoadmapTimelineDTO.TimelineInitiative initiative = result.getInitiatives().get(0);
    assertThat(initiative.getId()).isEqualTo(1L);
    assertThat(initiative.getName()).isEqualTo("Mobile Experience 2026");
    assertThat(initiative.getStatus()).isEqualTo("IN_PROGRESS");
    assertThat(initiative.getColor()).isEqualTo("#3B82F6");
  }

  @Test
  void getRoadmapTimeline_ShouldIncludeChildEpicsInInitiative() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    RoadmapTimelineDTO.TimelineInitiative initiative = result.getInitiatives().get(0);
    assertThat(initiative.getEpics()).hasSize(1);
    assertThat(initiative.getEpics().get(0).getName()).isEqualTo("Mobile Checkout Redesign");
  }

  @Test
  void getRoadmapTimeline_ShouldCorrectlyMapReleaseFields() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList());
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList(testRelease));
    when(pitchRepository.countByTargetReleaseIdsNotDeleted(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 5L}));
    when(pitchRepository.countByTargetReleaseIdsAndStatusNotDeleted(any(), eq(PitchStatus.DONE))).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 2L}));
    when(taskRepository.countByTargetReleaseIdsNotDeleted(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 8L}));
    when(bugReportRepository.countByTargetReleaseIds(any())).thenReturn(Arrays.<Object[]>asList(new Object[]{1L, 3L}));

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31)
    );

    assertThat(result.getReleases()).hasSize(1);
    RoadmapTimelineDTO.TimelineRelease release = result.getReleases().get(0);
    assertThat(release.getId()).isEqualTo(1L);
    assertThat(release.getName()).isEqualTo("February 2026 Release");
    assertThat(release.getVersion()).isEqualTo("v2.4.0");
    assertThat(release.getStatus()).isEqualTo("PLANNING");
    assertThat(release.getRiskLevel()).isEqualTo("MEDIUM");
    assertThat(release.getProgressPercentage()).isEqualTo(40.0);
    assertThat(release.getPitchCount()).isEqualTo(5L);
    assertThat(release.getTaskCount()).isEqualTo(8L);
    assertThat(release.getBugCount()).isEqualTo(3L);
  }

  @Test
  void getQuarterlyRoadmap_ShouldCallGetRoadmapTimelineWithCorrectDates() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(
        eq(1L),
        eq(LocalDate.of(2026, 1, 1)),
        eq(LocalDate.of(2026, 3, 31))
    )).thenReturn(Arrays.asList());
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(1L))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getQuarterlyRoadmap(1L, 2026, 1);

    assertThat(result).isNotNull();
    verify(initiativeRepository).findByProjectIdAndDateRangeNotDeleted(
        eq(1L),
        eq(LocalDate.of(2026, 1, 1)),
        eq(LocalDate.of(2026, 3, 31))
    );
  }

  @Test
  void getYearlyRoadmap_ShouldCallGetRoadmapTimelineWithFullYear() {
    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(
        eq(1L),
        eq(LocalDate.of(2026, 1, 1)),
        eq(LocalDate.of(2026, 12, 31))
    )).thenReturn(Arrays.asList());
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(1L))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getYearlyRoadmap(1L, 2026);

    assertThat(result).isNotNull();
    verify(initiativeRepository).findByProjectIdAndDateRangeNotDeleted(
        eq(1L),
        eq(LocalDate.of(2026, 1, 1)),
        eq(LocalDate.of(2026, 12, 31))
    );
  }

  @Test
  void getRoadmapTimeline_ShouldComputeSingleQuarterLabel() {
    testInitiative.setTargetStartDate(LocalDate.of(2026, 4, 1));
    testInitiative.setTargetEndDate(LocalDate.of(2026, 6, 15));

    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getInitiatives().get(0).getQuarterLabel()).isEqualTo("Q2 2026");
  }

  @Test
  void getRoadmapTimeline_ShouldComputeMultiQuarterSameYearLabel() {
    testInitiative.setTargetStartDate(LocalDate.of(2026, 4, 1));
    testInitiative.setTargetEndDate(LocalDate.of(2026, 9, 30));

    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getInitiatives().get(0).getQuarterLabel()).isEqualTo("Q2 – Q3 2026");
  }

  @Test
  void getRoadmapTimeline_ShouldComputeCrossYearQuarterLabel() {
    testInitiative.setTargetStartDate(LocalDate.of(2026, 10, 1));
    testInitiative.setTargetEndDate(LocalDate.of(2027, 3, 31));

    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31));

    assertThat(result.getInitiatives().get(0).getQuarterLabel()).isEqualTo("Q4 2026 – Q1 2027");
  }

  @Test
  void getRoadmapTimeline_ShouldExcludeOrphanEpicsWithNoDates() {
    Epic noDatesEpic = Epic.builder()
        .id(99L)
        .name("No Dates Epic")
        .status(EpicStatus.DRAFT)
        .project(testProject)
        .pitches(Arrays.asList())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList());
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList(noDatesEpic));
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getOrphanEpics()).isEmpty();
  }

  @Test
  void getRoadmapTimeline_ShouldExcludeChildEpicsOutsideDateRange() {
    Epic outOfRangeEpic = Epic.builder()
        .id(50L)
        .name("October Epic")
        .status(EpicStatus.PLANNED)
        .color("#FF0000")
        .targetStartDate(LocalDate.of(2026, 10, 1))
        .targetEndDate(LocalDate.of(2026, 12, 15))
        .project(testProject)
        .pitches(Arrays.asList())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    outOfRangeEpic.setInitiative(testInitiative);
    testInitiative.setEpics(Arrays.asList(testEpic, outOfRangeEpic));

    when(initiativeRepository.findByProjectIdAndDateRangeNotDeleted(any(), any(), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());
    when(releaseRepository.findByProjectIdNotDeleted(any()))
        .thenReturn(Arrays.asList());

    // Q2 date range: April 1 - June 30
    RoadmapTimelineDTO result = roadmapService.getRoadmapTimeline(
        1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));

    RoadmapTimelineDTO.TimelineInitiative init = result.getInitiatives().get(0);
    assertThat(init.getEpics()).hasSize(1);
    assertThat(init.getEpics().get(0).getName()).isEqualTo("Mobile Checkout Redesign");
  }

  // ========== Tasks under epics, combined progress, dependencies, project type ==========

  private com.github.farzadsedaghatbin.shipflow.entity.Task task(long id, Epic epic,
      com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus status,
      com.github.farzadsedaghatbin.shipflow.entity.Task parent) {
    return com.github.farzadsedaghatbin.shipflow.entity.Task.builder().id(id).title("Task " + id).status(status)
        .priority(com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority.MEDIUM).epic(epic)
        .parentTask(parent).build();
  }

  private Epic kanbanEpic(long id, Project project) {
    return Epic.builder().id(id).name("Kanban epic " + id).status(EpicStatus.IN_PROGRESS)
        .targetStartDate(LocalDate.of(2026, 2, 1)).targetEndDate(LocalDate.of(2026, 4, 30))
        .project(project).pitches(new java.util.ArrayList<>()).build();
  }

  @Test
  void getRoadmapTimeline_KanbanEpic_ShowsRootTasksAndCombinedProgress() {
    testProject.setProjectType(com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType.KANBAN);
    Epic epic = kanbanEpic(10L, testProject);
    var done = task(1L, epic, com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus.DONE, null);
    var todo = task(2L, epic, com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus.TODO, null);
    var subtask = task(3L, epic, com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus.DONE, todo);

    when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(testProject));
    when(epicRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));
    when(taskRepository.findByEpicIdInNotDeleted(any())).thenReturn(Arrays.asList(done, todo, subtask));

    RoadmapTimelineDTO result =
        roadmapService.getRoadmapTimeline(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getProjectType()).isEqualTo("KANBAN");
    assertThat(result.getOrphanEpics()).hasSize(1);
    RoadmapTimelineDTO.TimelineEpic te = result.getOrphanEpics().get(0);
    // subtasks are excluded; 1 of 2 root tasks done
    assertThat(te.getTasks()).extracting(RoadmapTimelineDTO.TimelineTask::getId).containsExactly(1L, 2L);
    assertThat(te.getTasks().get(0).getProgress()).isEqualTo(100.0);
    assertThat(te.getTasks().get(1).getProgress()).isEqualTo(0.0);
    assertThat(te.getTasks().get(0).getEpicId()).isEqualTo(10L);
    assertThat(te.getProgress()).isEqualTo(50.0);
    // one bulk query regardless of epic count
    verify(taskRepository, times(1)).findByEpicIdInNotDeleted(any());
  }

  @Test
  void getRoadmapTimeline_EpicProgressCombinesPitchesAndTasks() {
    Epic epic = kanbanEpic(11L, testProject);
    epic.setPitches(Arrays.asList(testPitch)); // DONE pitch
    var todo = task(5L, epic, com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus.TODO, null);

    when(epicRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));
    when(taskRepository.findByEpicIdInNotDeleted(any())).thenReturn(Arrays.asList(todo));

    RoadmapTimelineDTO result =
        roadmapService.getRoadmapTimeline(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getOrphanEpics().get(0).getProgress()).isEqualTo(50.0);
  }

  @Test
  void getRoadmapTimeline_EpicWithNeitherPitchesNorTasks_FallsBackToStatus() {
    Epic epic = kanbanEpic(12L, testProject);
    epic.setStatus(EpicStatus.COMPLETED);
    when(epicRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(epic));

    RoadmapTimelineDTO result =
        roadmapService.getRoadmapTimeline(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getOrphanEpics().get(0).getProgress()).isEqualTo(100.0);
    assertThat(result.getOrphanEpics().get(0).getTasks()).isEmpty();
  }

  @Test
  void getRoadmapTimeline_IncludesDependenciesBetweenProjectEpics() {
    Epic a = kanbanEpic(20L, testProject);
    Epic b = kanbanEpic(21L, testProject);
    com.github.farzadsedaghatbin.shipflow.entity.EpicDependency dep =
        com.github.farzadsedaghatbin.shipflow.entity.EpicDependency.builder().id(7L).sourceEpic(a).targetEpic(b)
            .dependencyType(com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType.BLOCKS).build();

    when(epicRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(a, b));
    when(epicRepository.findOrphanEpicsByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(a, b));
    when(epicDependencyRepository.findBySourceEpicIdInAndTargetEpicIdIn(any(), any()))
        .thenReturn(Arrays.asList(dep));

    RoadmapTimelineDTO result =
        roadmapService.getRoadmapTimeline(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result.getDependencies()).hasSize(1);
    RoadmapTimelineDTO.TimelineDependency d = result.getDependencies().get(0);
    assertThat(d.getId()).isEqualTo(7L);
    assertThat(d.getSourceEpicId()).isEqualTo(20L);
    assertThat(d.getTargetEpicId()).isEqualTo(21L);
    assertThat(d.getDependencyType()).isEqualTo("BLOCKS");
    // Only epics of this project are ever queried for dependencies
    org.mockito.ArgumentCaptor<java.util.Collection<Long>> ids = org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
    verify(epicDependencyRepository).findBySourceEpicIdInAndTargetEpicIdIn(ids.capture(), ids.capture());
    assertThat(ids.getAllValues().get(0)).containsExactlyInAnyOrder(20L, 21L);
  }

  @Test
  void getRoadmapTimeline_NoEpics_ReturnsEmptyDependencies() {
    RoadmapTimelineDTO result =
        roadmapService.getRoadmapTimeline(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    assertThat(result.getDependencies()).isEmpty();
    verifyNoInteractions(epicDependencyRepository);
  }
}
