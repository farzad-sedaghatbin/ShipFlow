package com.github.farzadsedaghatbin.shipflow.service.automation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.service.WorkflowAutomationEngine;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowScheduler")
class WorkflowSchedulerTest {

  private static final Instant NOW = Instant.parse("2026-11-06T09:00:30Z"); // a Friday
  private static final Instant SLOT = Instant.parse("2026-11-06T09:00:00Z");
  private static final Instant NEXT = Instant.parse("2026-11-09T09:00:00Z"); // next weekday slot

  @Mock private WorkflowAutomationRepository repository;
  @Mock private WorkflowAutomationEngine engine;

  private WorkflowScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new WorkflowScheduler(repository, engine, new ObjectMapper());
    when(repository.findScheduledWithoutNextRun(TriggerType.SCHEDULE)).thenReturn(List.of());
  }

  private WorkflowAutomation due(Instant slot, String cron) {
    WorkflowAutomation a = new WorkflowAutomation();
    a.setId(7L);
    a.setTriggerType(TriggerType.SCHEDULE);
    a.setTriggerConfig("{\"cron\":\"" + cron + "\"}");
    a.setNextRunAt(slot);
    when(repository.findDueScheduled(TriggerType.SCHEDULE, NOW)).thenReturn(List.of(a));
    return a;
  }

  @Test
  @DisplayName("A due automation is claimed for its NEXT slot and then run")
  void claimsThenRuns() {
    WorkflowAutomation a = due(SLOT, "0 0 9 * * MON-FRI");
    when(repository.claimSlot(7L, SLOT, NEXT)).thenReturn(1);

    scheduler.tick(NOW);

    verify(repository).claimSlot(7L, SLOT, NEXT);
    verify(engine).runScheduled(a, SLOT);
  }

  @Test
  @DisplayName("Losing the claim to another replica means NOT running it (no double posts)")
  void lostClaimDoesNotRun() {
    due(SLOT, "0 0 9 * * MON-FRI");
    when(repository.claimSlot(7L, SLOT, NEXT)).thenReturn(0);

    scheduler.tick(NOW);

    verify(engine, never()).runScheduled(any(), any());
  }

  @Test
  @DisplayName("A slot missed by more than the grace period is skipped, not replayed")
  void staleSlotSkipped() {
    Instant stale = NOW.minusSeconds(3 * 3600);
    due(stale, "0 0 9 * * MON-FRI");
    when(repository.claimSlot(eq(7L), eq(stale), any())).thenReturn(1);

    scheduler.tick(NOW);

    verify(repository).claimSlot(eq(7L), eq(stale), eq(NEXT)); // still moved on to the next slot
    verify(engine, never()).runScheduled(any(), any());
  }

  @Test
  @DisplayName("A slot just inside the grace period still runs")
  void slightlyLateStillRuns() {
    Instant late = NOW.minusSeconds(100 * 60);
    WorkflowAutomation a = due(late, "0 0 9 * * MON-FRI");
    when(repository.claimSlot(eq(7L), eq(late), any())).thenReturn(1);

    scheduler.tick(NOW);

    verify(engine).runScheduled(a, late);
  }

  @Test
  @DisplayName("An automation with a broken schedule is parked, not retried every minute")
  void invalidSchedulePark() {
    due(SLOT, "nonsense");

    scheduler.tick(NOW);

    verify(repository).claimSlot(7L, SLOT, null);
    verify(engine, never()).runScheduled(any(), any());
  }

  @Test
  @DisplayName("One automation failing to start does not stop the others")
  void oneFailureIsIsolated() {
    WorkflowAutomation bad = due(SLOT, "0 0 9 * * MON-FRI");
    WorkflowAutomation good = new WorkflowAutomation();
    good.setId(8L);
    good.setTriggerType(TriggerType.SCHEDULE);
    good.setTriggerConfig("{\"cron\":\"0 0 9 * * MON-FRI\"}");
    good.setNextRunAt(SLOT);
    when(repository.findDueScheduled(TriggerType.SCHEDULE, NOW)).thenReturn(List.of(bad, good));
    when(repository.claimSlot(7L, SLOT, NEXT)).thenThrow(new RuntimeException("db down"));
    when(repository.claimSlot(8L, SLOT, NEXT)).thenReturn(1);

    scheduler.tick(NOW);

    verify(engine).runScheduled(good, SLOT);
  }

  @Test
  @DisplayName("Enabled schedules with no slot yet are given their first one")
  void initialisesUnscheduled() {
    WorkflowAutomation fresh = new WorkflowAutomation();
    fresh.setId(9L);
    fresh.setTriggerType(TriggerType.SCHEDULE);
    fresh.setTriggerConfig("{\"cron\":\"0 0 9 * * MON-FRI\"}");
    when(repository.findScheduledWithoutNextRun(TriggerType.SCHEDULE)).thenReturn(List.of(fresh));
    when(repository.findDueScheduled(TriggerType.SCHEDULE, NOW)).thenReturn(List.of());

    scheduler.tick(NOW);

    verify(repository).initSlot(9L, NEXT);
    verify(engine, never()).runScheduled(any(), any());
  }

  @Test
  @DisplayName("Nothing due, nothing happens")
  void idle() {
    when(repository.findDueScheduled(TriggerType.SCHEDULE, NOW)).thenReturn(List.of());

    scheduler.tick(NOW);

    verify(repository, never()).claimSlot(anyLong(), any(), any());
  }
}
