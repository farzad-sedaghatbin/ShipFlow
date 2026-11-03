package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The scheduler's multi-replica safety rests on these queries behaving exactly as written, so they
 * are proved against a real database rather than assumed from mocks.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WorkflowAutomationRepository — scheduling queries")
class WorkflowAutomationRepositoryTest {

  private static final Instant T0 = Instant.parse("2026-11-06T09:00:00Z");

  @Autowired private WorkflowAutomationRepository repository;
  @Autowired private ProjectRepository projectRepository;
  @PersistenceContext private EntityManager em;

  private Project project;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setName("P");
    project.setProjectKey("PRJ");
    project.setProjectType(ProjectType.SHAPE_UP);
    project.setIsActive(true);
    project = projectRepository.save(project);
  }

  private WorkflowAutomation save(TriggerType trigger, boolean enabled, Instant next) {
    WorkflowAutomation a = WorkflowAutomation.builder()
        .name("A").project(project).triggerType(trigger).actionType(ActionType.AGENT_STANDUP_DIGEST)
        .enabled(enabled).nextRunAt(next).build();
    return repository.saveAndFlush(a);
  }

  private WorkflowAutomation reload(Long id) {
    em.clear();
    return repository.findById(id).orElseThrow();
  }

  @Test
  @DisplayName("Only the first of two racing claims on the same slot wins")
  void claimIsExclusive() {
    WorkflowAutomation a = save(TriggerType.SCHEDULE, true, T0);
    Instant next = T0.plusSeconds(86400);

    int first = repository.claimSlot(a.getId(), T0, next);
    int second = repository.claimSlot(a.getId(), T0, next.plusSeconds(60)); // a second replica, same stale view

    assertThat(first).isEqualTo(1);
    assertThat(second).isEqualTo(0);
    assertThat(reload(a.getId()).getNextRunAt()).isEqualTo(next);
  }

  @Test
  @DisplayName("A claim can park an automation (null next slot)")
  void claimToNull() {
    WorkflowAutomation a = save(TriggerType.SCHEDULE, true, T0);

    assertThat(repository.claimSlot(a.getId(), T0, null)).isEqualTo(1);
    assertThat(reload(a.getId()).getNextRunAt()).isNull();
  }

  @Test
  @DisplayName("findDueScheduled returns only enabled, live SCHEDULE automations whose slot has arrived")
  void dueQuery() {
    WorkflowAutomation due = save(TriggerType.SCHEDULE, true, T0);
    save(TriggerType.SCHEDULE, true, T0.plusSeconds(3600));   // slot in the future
    save(TriggerType.SCHEDULE, false, T0);                    // disabled
    save(TriggerType.TASK_CREATED, true, T0);                 // not a schedule
    save(TriggerType.SCHEDULE, true, null);                   // no slot
    WorkflowAutomation deleted = save(TriggerType.SCHEDULE, true, T0);
    deleted.setDeletedAt(LocalDateTime.now());
    repository.saveAndFlush(deleted);

    List<WorkflowAutomation> found = repository.findDueScheduled(TriggerType.SCHEDULE, T0.plusSeconds(30));

    assertThat(found).extracting(WorkflowAutomation::getId).containsExactly(due.getId());
  }

  @Test
  @DisplayName("Enabled schedules without a slot are found and can be given one exactly once")
  void initialisation() {
    WorkflowAutomation fresh = save(TriggerType.SCHEDULE, true, null);
    save(TriggerType.SCHEDULE, true, T0); // already has one
    save(TriggerType.SCHEDULE, false, null); // disabled → not initialised

    assertThat(repository.findScheduledWithoutNextRun(TriggerType.SCHEDULE))
        .extracting(WorkflowAutomation::getId).containsExactly(fresh.getId());

    assertThat(repository.initSlot(fresh.getId(), T0)).isEqualTo(1);
    assertThat(repository.initSlot(fresh.getId(), T0.plusSeconds(99))).isEqualTo(0); // never overwrites
    assertThat(reload(fresh.getId()).getNextRunAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("Counting a run touches only the counter and timestamp — the claimed slot survives")
  void recordExecutionKeepsTheClaim() {
    WorkflowAutomation a = save(TriggerType.SCHEDULE, true, T0);
    Instant claimed = T0.plusSeconds(86400);
    repository.claimSlot(a.getId(), T0, claimed);
    LocalDateTime at = LocalDateTime.of(2026, 11, 6, 9, 0, 5);

    repository.recordExecution(a.getId(), at);
    repository.recordExecution(a.getId(), at.plusSeconds(1));

    WorkflowAutomation after = reload(a.getId());
    assertThat(after.getExecutionCount()).isEqualTo(2);
    assertThat(after.getLastTriggeredAt()).isEqualTo(at.plusSeconds(1));
    assertThat(after.getNextRunAt()).isEqualTo(claimed);
  }
}
