package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.ReleaseReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseReportServiceTest {

  @Mock private ReleaseRepository releaseRepository;

  @Mock private TaskRepository taskRepository;

  @InjectMocks private ReleaseReportService releaseReportService;

  @Test
  void computeReleaseReport_noReleases_returnsEmptyList() {
    when(releaseRepository.findByProjectIdNotDeleted(1L)).thenReturn(List.of());
    when(taskRepository.findByProjectIdNotDeleted(1L)).thenReturn(List.of());

    List<ReleaseReportDTO> result = releaseReportService.computeReleaseReport(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void computeReleaseReport_releaseWithNoTasks_zeroedCounts() {
    Release release = buildRelease(1L, "v1.0.0", "Release 1", ReleaseStatus.PLANNING);

    when(releaseRepository.findByProjectIdNotDeleted(10L)).thenReturn(List.of(release));
    when(taskRepository.findByProjectIdNotDeleted(10L)).thenReturn(List.of());

    List<ReleaseReportDTO> result = releaseReportService.computeReleaseReport(10L);

    assertThat(result).hasSize(1);
    ReleaseReportDTO dto = result.get(0);
    assertThat(dto.getReleaseId()).isEqualTo(1L);
    assertThat(dto.getTaskCount()).isEqualTo(0);
    assertThat(dto.getCompletedTaskCount()).isEqualTo(0);
    assertThat(dto.getPlannedPoints()).isEqualTo(0);
    assertThat(dto.getCompletedPoints()).isEqualTo(0);
  }

  @Test
  void computeReleaseReport_tasksWithPoints_correctCountsAndPoints() {
    Release release = buildRelease(5L, "v2.0.0", "Release 5", ReleaseStatus.IN_PROGRESS);

    Task done1 = buildTask(1L, 5, TaskStatus.DONE, release);
    Task done2 = buildTask(2L, 3, TaskStatus.DONE, release);
    Task inProgress = buildTask(3L, 8, TaskStatus.IN_PROGRESS, release);
    Task noPoints = buildTask(4L, null, TaskStatus.DONE, release);
    // Task targeting a different release entirely — must not leak into this release's counts
    Release otherRelease = buildRelease(6L, "v3.0.0", "Release 6", ReleaseStatus.PLANNING);
    Task otherReleaseTask = buildTask(5L, 13, TaskStatus.TODO, otherRelease);

    when(releaseRepository.findByProjectIdNotDeleted(20L)).thenReturn(List.of(release));
    when(taskRepository.findByProjectIdNotDeleted(20L))
        .thenReturn(List.of(done1, done2, inProgress, noPoints, otherReleaseTask));

    List<ReleaseReportDTO> result = releaseReportService.computeReleaseReport(20L);

    assertThat(result).hasSize(1);
    ReleaseReportDTO dto = result.get(0);
    assertThat(dto.getReleaseId()).isEqualTo(5L);
    assertThat(dto.getVersion()).isEqualTo("v2.0.0");
    assertThat(dto.getStatus()).isEqualTo(ReleaseStatus.IN_PROGRESS);
    assertThat(dto.getTaskCount()).isEqualTo(4); // otherReleaseTask excluded
    // completedTaskCount is status-based (DONE), independent of story points — includes noPoints
    assertThat(dto.getCompletedTaskCount()).isEqualTo(3);
    assertThat(dto.getPlannedPoints()).isEqualTo(16); // 5+3+8, noPoints excluded
    assertThat(dto.getCompletedPoints()).isEqualTo(8); // 5+3
  }

  @Test
  void computeReleaseReport_taskWithNoTargetRelease_excludedFromAllReleases() {
    Release release = buildRelease(1L, "v1.0.0", "Release 1", ReleaseStatus.PLANNING);
    Task noRelease = buildTask(1L, 5, TaskStatus.DONE, null);

    when(releaseRepository.findByProjectIdNotDeleted(30L)).thenReturn(List.of(release));
    when(taskRepository.findByProjectIdNotDeleted(30L)).thenReturn(List.of(noRelease));

    List<ReleaseReportDTO> result = releaseReportService.computeReleaseReport(30L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTaskCount()).isEqualTo(0);
  }

  // ---- helpers ----

  private Release buildRelease(Long id, String version, String name, ReleaseStatus status) {
    Release r = new Release();
    r.setId(id);
    r.setName(name);
    r.setVersion(version);
    r.setStatus(status);
    r.setTargetDate(LocalDate.now().plusDays(30));
    return r;
  }

  private Task buildTask(Long id, Integer points, TaskStatus status, Release targetRelease) {
    Task t = new Task();
    t.setId(id);
    t.setStoryPoints(points);
    t.setStatus(status);
    t.setTargetRelease(targetRelease);
    return t;
  }
}
