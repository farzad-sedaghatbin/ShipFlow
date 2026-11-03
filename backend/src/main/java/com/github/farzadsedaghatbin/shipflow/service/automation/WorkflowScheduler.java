package com.github.farzadsedaghatbin.shipflow.service.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.service.WorkflowAutomationEngine;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs SCHEDULE automations when their slot arrives.
 *
 * <p><b>Several replicas can run this at once</b> (the Helm chart scales the app horizontally and
 * the other {@code @Scheduled} jobs here are idempotent). A digest posted to Slack is not, so every
 * run is first <em>claimed</em>: the replica whose conditional {@code UPDATE ... WHERE next_run_at =
 * :seen} moves the automation to its next slot is the only one that runs it; the others see 0 rows
 * changed and move on. No external lock service is needed.
 *
 * <p><b>Missed slots are skipped, not replayed.</b> After downtime a Monday-09:00 digest is not
 * posted on Tuesday afternoon; a slot more than {@link #MISSED_GRACE} old is advanced past without
 * running.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowScheduler {

  /** How late a slot may be and still run. */
  static final Duration MISSED_GRACE = Duration.ofHours(2);

  private final WorkflowAutomationRepository repository;
  private final WorkflowAutomationEngine engine;
  private final ObjectMapper objectMapper;

  @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
  public void tick() {
    tick(Instant.now());
  }

  /** One pass at a given moment — separate from {@link #tick()} so it can be tested with a clock. */
  void tick(Instant now) {
    initialiseUnscheduled(now);
    List<WorkflowAutomation> due = repository.findDueScheduled(TriggerType.SCHEDULE, now);
    for (WorkflowAutomation automation : due) {
      try {
        runIfWon(automation, now);
      } catch (Exception e) {
        log.error("Scheduled automation {} failed to start: {}", automation.getId(), e.getMessage(), e);
      }
    }
  }

  private void runIfWon(WorkflowAutomation automation, Instant now) {
    Instant slot = automation.getNextRunAt();
    WorkflowSchedule schedule;
    try {
      schedule = WorkflowSchedule.parse(automation.getTriggerConfig(), objectMapper);
    } catch (IllegalArgumentException e) {
      // A broken schedule must not be retried every minute: park it until someone edits it.
      log.warn("Automation {} has an invalid schedule and was parked: {}", automation.getId(), e.getMessage());
      repository.claimSlot(automation.getId(), slot, null);
      return;
    }
    Instant next = schedule.next(now).orElse(null);
    if (repository.claimSlot(automation.getId(), slot, next) == 0) {
      log.debug("Automation {} was already claimed by another instance", automation.getId());
      return;
    }
    if (Duration.between(slot, now).compareTo(MISSED_GRACE) > 0) {
      log.info("Automation {} skipped its {} slot (more than {} late); next run {}",
          automation.getId(), slot, MISSED_GRACE, next);
      return;
    }
    engine.runScheduled(automation, slot);
  }

  /** Enabled SCHEDULE automations with no slot (older rows, or enabled outside the service) get one. */
  private void initialiseUnscheduled(Instant now) {
    for (WorkflowAutomation a : repository.findScheduledWithoutNextRun(TriggerType.SCHEDULE)) {
      try {
        Instant next = WorkflowSchedule.parse(a.getTriggerConfig(), objectMapper).next(now).orElse(null);
        if (next != null) {
          repository.initSlot(a.getId(), next);
        }
      } catch (IllegalArgumentException e) {
        log.debug("Automation {} has no usable schedule yet: {}", a.getId(), e.getMessage());
      }
    }
  }
}
