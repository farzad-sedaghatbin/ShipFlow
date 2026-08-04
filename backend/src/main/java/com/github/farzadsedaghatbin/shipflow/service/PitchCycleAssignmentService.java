package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralizes pitch↔cycle assignment so every place that bets, un-bets, or re-bets a pitch
 * propagates the change to the pitch's tasks and records an audit-trail row for each, instead
 * of each call site independently setting {@code pitch.cycle} and leaving the pitch's tasks
 * pointing at a stale cycle.
 *
 * <p>Runs synchronously, inline, in the same transaction as the pitch mutation that calls it —
 * intentionally NOT async/event-based. This is a direct write, not a listener reading back a
 * row the current transaction just wrote, so the {@code @TransactionalEventListener(AFTER_COMMIT)}
 * rule (see CLAUDE.md) doesn't apply here; making this async would reintroduce exactly the
 * audit-trail inconsistency window this feature exists to prevent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PitchCycleAssignmentService {

  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final TaskCycleHistoryService taskCycleHistoryService;

  /**
   * Assign (or clear) a pitch's cycle and propagate the change to every non-deleted task linked
   * to the pitch.
   *
   * @param pitch          the pitch being (re)bet or un-bet — mutated and saved in place
   * @param newCycleOrNull the cycle to assign, or {@code null} to un-bet the pitch
   */
  public void applyCycleToPitch(Pitch pitch, Cycle newCycleOrNull) {
    pitch.setCycle(newCycleOrNull);
    pitchRepository.save(pitch);

    List<Task> tasks = taskRepository.findByPitchId(pitch.getId());
    if (tasks.isEmpty()) {
      return;
    }

    for (Task task : tasks) {
      task.setCycle(newCycleOrNull);
      // Only mirror the project reference when the pitch is being assigned to a cycle. A task
      // losing its cycle (pitch un-bet) should keep its existing project reference — it hasn't
      // moved projects, it's just gone back to being un-scheduled.
      if (newCycleOrNull != null) {
        task.setProject(newCycleOrNull.getProject());
      }
    }
    taskRepository.saveAll(tasks);

    taskCycleHistoryService.recordSnapshots(tasks, TaskCycleHistory.ChangeSource.PITCH_CYCLE_CHANGE);

    log.info("Propagated cycle change for pitch {} to {} task(s) (cycle={})", pitch.getId(), tasks.size(),
        newCycleOrNull != null ? newCycleOrNull.getId() : "null");
  }
}
