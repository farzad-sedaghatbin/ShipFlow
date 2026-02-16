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
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    when(pitchRepository.countByTargetReleaseIdNotDeleted(any())).thenReturn(3L);
    when(pitchRepository.countByTargetReleaseIdAndStatusNotDeleted(any(), eq(PitchStatus.DONE))).thenReturn(1L);

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
    when(pitchRepository.countByTargetReleaseIdNotDeleted(1L)).thenReturn(5L);
    when(pitchRepository.countByTargetReleaseIdAndStatusNotDeleted(1L, PitchStatus.DONE)).thenReturn(2L);

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
}
