package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test for a production 500 on {@code PATCH /qa/bug-reports/{id}/move-to-project/{p}}:
 * {@code moveBugReportToProject} was the only mutating method in {@link BugReportService} without
 * {@code @Transactional}, so with {@code spring.jpa.open-in-view=false} its {@code toDTO} call
 * dereferenced the bug's lazy {@code reporter} association after the repository call's own
 * transaction had closed — throwing {@link org.hibernate.LazyInitializationException} and
 * returning 500 <em>after</em> the move had already been committed. The move looked like it had
 * failed while it had in fact succeeded.
 *
 * <p>Deliberately not {@code @Transactional}: a test-level transaction would hold the Hibernate
 * session open for the whole test and hide exactly the bug being reproduced, and a Mockito-only
 * unit test cannot catch it at all (a mocked repository returns a fully-built POJO, never a
 * proxy). Same reasoning as {@link SprintTaskSuggestionServiceLazyLoadingIntegrationTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class BugReportServiceMoveLazyLoadingIntegrationTest {

  @Autowired private BugReportService bugReportService;
  @Autowired private BugReportRepository bugReportRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private UserRepository userRepository;

  private Long bugId;
  private Long reporterId;
  private Long sourceProjectId;
  private Long targetProjectId;

  /**
   * This test is deliberately non-transactional (see the class Javadoc), so its rows really are
   * committed to the shared H2 instance and must be removed by hand. Leaving the bug report
   * behind breaks every later test class whose fixture calls {@code userRepository.deleteAll()}
   * — the orphaned {@code bug_reports.reporter_id} FK makes that delete fail.
   */
  @AfterEach
  void cleanUp() {
    if (bugId != null) {
      bugReportRepository.deleteById(bugId);
    }
    if (reporterId != null) {
      userRepository.deleteById(reporterId);
    }
    if (sourceProjectId != null) {
      projectRepository.deleteById(sourceProjectId);
    }
    if (targetProjectId != null) {
      projectRepository.deleteById(targetProjectId);
    }
  }

  @Test
  void moveBugReportToProject_doesNotThrowLazyInitializationException() {
    long unique = System.nanoTime() % 100000;

    Project source = projectRepository.save(Project.builder().name("Move Source " + unique)
        .projectKey("MVS" + unique).projectType(ProjectType.KANBAN).isActive(true).build());
    Project target = projectRepository.save(Project.builder().name("Move Target " + unique)
        .projectKey("MVT" + unique).projectType(ProjectType.KANBAN).isActive(true).build());

    User reporter = userRepository.save(User.builder().username("move.reporter." + unique)
        .email("move.reporter." + unique + "@example.com").password("x").role(UserRole.MEMBER)
        .isActive(true).build());

    BugReport bug = bugReportRepository.save(BugReport.builder().bugKey("BUG-MOVE-" + unique)
        .title("Bug filed against the wrong project").description("...").severity(BugSeverity.MAJOR)
        .status(BugStatus.OPEN).reporter(reporter).project(source).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build());

    bugId = bug.getId();
    reporterId = reporter.getId();
    sourceProjectId = source.getId();
    targetProjectId = target.getId();

    BugReportDTO moved = bugReportService.moveBugReportToProject(bug.getId(), target.getId());

    assertThat(moved.getProjectId()).isEqualTo(target.getId());
    // The lazy association that used to blow up once the repository call's transaction closed.
    assertThat(moved.getReporterName()).isEqualTo(reporter.getUsername());
    // Re-read through the service (its own read transaction) rather than dereferencing a lazy
    // association straight off the repository here — that would throw in the test itself and
    // mask whatever the service under test actually did.
    assertThat(bugReportService.getBugReportById(bug.getId()).getProjectId())
        .isEqualTo(target.getId());
  }
}
