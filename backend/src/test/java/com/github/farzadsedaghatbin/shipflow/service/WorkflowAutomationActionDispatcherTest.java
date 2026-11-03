package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowAutomationActionDispatcher")
class WorkflowAutomationActionDispatcherTest {

  private AgentLoopService loops;
  private WorkflowAutomationActionDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    loops = mock(AgentLoopService.class);
    dispatcher = new WorkflowAutomationActionDispatcher(
        mock(TaskRepository.class), mock(UserProjectRepository.class), mock(DashboardNotificationService.class), loops);
  }

  private static WorkflowAutomation automation(ActionType type) {
    WorkflowAutomation a = new WorkflowAutomation();
    a.setId(1L);
    a.setActionType(type);
    a.setActionConfig("{}");
    return a;
  }

  @Test
  @DisplayName("Each agent loop action is handed to the agent loop service and its message is returned")
  void agentActionsReturnTheirMessage() {
    for (ActionType type : new ActionType[] {
        ActionType.AGENT_STANDUP_DIGEST, ActionType.AGENT_CYCLE_HEALTH_CHECK, ActionType.AGENT_BETTING_PREP}) {
      WorkflowAutomation a = automation(type);
      when(loops.run(a, Map.of("x", 1))).thenReturn("Sent to Slack");

      assertThat(dispatcher.dispatch(a, Map.of("x", 1))).isEqualTo("Sent to Slack");
    }
  }

  @Test
  @DisplayName("A loop that fails to deliver propagates, so the engine records a FAILURE")
  void failuresPropagate() {
    WorkflowAutomation a = automation(ActionType.AGENT_STANDUP_DIGEST);
    when(loops.run(a, Map.of())).thenThrow(new IllegalStateException("Nothing was sent"));

    assertThatThrownBy(() -> dispatcher.dispatch(a, Map.of())).hasMessage("Nothing was sent");
  }

  @Test
  @DisplayName("Existing actions are unchanged: they have nothing more to say (null) and never touch the loops")
  void legacyActionsReturnNull() {
    assertThat(dispatcher.dispatch(automation(ActionType.SEND_WEBHOOK), Map.of())).isNull();
    assertThat(dispatcher.dispatch(automation(ActionType.ADD_COMMENT), Map.of())).isNull();
    verify(loops, never()).run(any(), any());
  }
}
