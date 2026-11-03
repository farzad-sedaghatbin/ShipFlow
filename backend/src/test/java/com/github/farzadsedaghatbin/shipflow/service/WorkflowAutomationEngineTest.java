package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowAutomationEngine")
class WorkflowAutomationEngineTest {

  @Mock private WorkflowAutomationRepository automationRepository;
  @Mock private WorkflowAutomationExecutionRepository executionRepository;
  @Mock private WorkflowAutomationActionDispatcher dispatcher;

  private WorkflowAutomationEngine engine;
  private WorkflowAutomation automation;

  @BeforeEach
  void setUp() {
    engine = new WorkflowAutomationEngine(automationRepository, executionRepository, dispatcher);
    Project p = new Project();
    p.setId(5L);
    automation = new WorkflowAutomation();
    automation.setId(7L);
    automation.setProject(p);
    automation.setTriggerType(TriggerType.SCHEDULE);
    when(executionRepository.save(any(WorkflowAutomationExecution.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private WorkflowAutomationExecution recorded() {
    ArgumentCaptor<WorkflowAutomationExecution> c = ArgumentCaptor.forClass(WorkflowAutomationExecution.class);
    verify(executionRepository).save(c.capture());
    return c.getValue();
  }

  @Test
  @DisplayName("The action's own description becomes the execution log entry")
  void recordsTheActionsMessage() {
    when(dispatcher.dispatch(any(), any())).thenReturn("Sent to Slack");

    engine.runScheduled(automation, Instant.parse("2026-11-06T09:00:00Z"));

    WorkflowAutomationExecution e = recorded();
    assertThat(e.getStatus()).isEqualTo(AutomationExecutionStatus.SUCCESS);
    assertThat(e.getResultMessage()).isEqualTo("Sent to Slack");
    assertThat(e.getTriggerEventType()).isEqualTo("SCHEDULE");
    assertThat(e.getTriggerEventData()).contains("\"scheduledFor\":\"2026-11-06T09:00:00Z\"");
  }

  @Test
  @DisplayName("An action with nothing to say still logs the generic success text")
  void defaultMessage() {
    when(dispatcher.dispatch(any(), any())).thenReturn(null);

    engine.runScheduled(automation, null);

    assertThat(recorded().getResultMessage()).isEqualTo("Action executed successfully.");
  }

  @Test
  @DisplayName("A failing action is logged as FAILURE with its reason and is not counted as a run")
  void failureIsRecorded() {
    when(dispatcher.dispatch(any(), any())).thenThrow(new IllegalStateException("Nothing was sent"));

    engine.runScheduled(automation, null);

    WorkflowAutomationExecution e = recorded();
    assertThat(e.getStatus()).isEqualTo(AutomationExecutionStatus.FAILURE);
    assertThat(e.getResultMessage()).contains("Nothing was sent");
    verify(automationRepository, never()).recordExecution(any(), any());
  }

  @Test
  @DisplayName("A run is counted with a targeted update, never by saving the whole automation back")
  void countsWithoutRewritingTheRow() {
    when(dispatcher.dispatch(any(), any())).thenReturn("ok");

    engine.runScheduled(automation, null);

    verify(automationRepository).recordExecution(any(), any());
    verify(automationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Run now goes through the same path and hands back the execution record")
  void runNow() {
    when(dispatcher.dispatch(any(), any())).thenReturn("Sent to Teams");

    WorkflowAutomationExecution e = engine.runNow(automation);

    assertThat(e.getResultMessage()).isEqualTo("Sent to Teams");
    assertThat(e.getTriggerEventData()).contains("\"trigger\":\"MANUAL\"");
  }
}
