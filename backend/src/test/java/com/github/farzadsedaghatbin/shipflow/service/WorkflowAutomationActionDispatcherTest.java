package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowAutomationActionDispatcher")
class WorkflowAutomationActionDispatcherTest {

  private AgentLoopService loops;
  private TaskRepository tasks;
  private UserProjectRepository members;
  private IEmailNotificationService email;
  private com.github.farzadsedaghatbin.shipflow.service.email.EmailRecipientService recipients;
  private WorkflowAutomationActionDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    loops = mock(AgentLoopService.class);
    tasks = mock(TaskRepository.class);
    members = mock(UserProjectRepository.class);
    email = mock(IEmailNotificationService.class);
    recipients = mock(com.github.farzadsedaghatbin.shipflow.service.email.EmailRecipientService.class);
    when(email.isEnabled()).thenReturn(true);
    dispatcher = new WorkflowAutomationActionDispatcher(
        tasks, members, mock(DashboardNotificationService.class), loops, email, recipients);
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

  // ── SEND_EMAIL ─────────────────────────────────────────────────────────────────────────────

  private User member(long id, String username, String personEmail) {
    Person person = Person.builder().id(id).name(username + " Name").email(personEmail).build();
    User user = User.builder().id(id).username(username).person(person).build();
    person.setUser(user);
    String usable = com.github.farzadsedaghatbin.shipflow.service.email.EmailAddressResolver.of(user);
    when(recipients.forUserId(id)).thenReturn(usable == null ? Optional.empty()
        : Optional.of(new com.github.farzadsedaghatbin.shipflow.service.email.EmailRecipientService.Recipient(
            usable, username + " Name")));
    return user;
  }

  private static UserProject membership(User user, ProjectRole role) {
    return UserProject.builder().user(user).projectRole(role).build();
  }

  private WorkflowAutomation emailRule(String config) {
    WorkflowAutomation a = automation(ActionType.SEND_EMAIL);
    a.setName("Cycle wrap-up");
    a.setActionConfig(config);
    Project project = new Project();
    project.setId(5L);
    a.setProject(project);
    return a;
  }

  @Test
  @DisplayName("SEND_EMAIL with no 'to' emails every project member, filling {{placeholders}}")
  void sendEmailDefaultsToProjectMembers() {
    User hooman = member(1L, "hooman", "hooman@goldorigins.com");
    User nikoo = member(2L, "nikoo", "a.nikoo@goldorigins.com");
    when(members.findByProjectIdEager(5L)).thenReturn(
        List.of(membership(hooman, ProjectRole.CONTRIBUTOR), membership(nikoo, ProjectRole.MANAGER)));
    WorkflowAutomation a = emailRule("{\"subject\":\"Cycle {{cycleName}} done\",\"body\":\"Check the retro.\"}");

    String detail = dispatcher.dispatch(a, Map.of("cycleName", "Cycle \"7\"", "cycleId", 9L));

    assertThat(detail).isEqualTo("Emailed 2 recipients");
    verify(email).sendAutomationEmail(eq("hooman@goldorigins.com"), eq("hooman Name"),
        eq("Cycle Cycle \"7\" done"), eq("Check the retro."), eq("/cycles/9"));
    verify(email).sendAutomationEmail(eq("a.nikoo@goldorigins.com"), anyString(), anyString(), anyString(), eq("/cycles/9"));
  }

  @Test
  @DisplayName("'project_managers' emails only members with the MANAGER role")
  void sendEmailToManagersOnly() {
    User hooman = member(1L, "hooman", "hooman@goldorigins.com");
    User nikoo = member(2L, "nikoo", "a.nikoo@goldorigins.com");
    when(members.findByProjectIdEager(5L)).thenReturn(
        List.of(membership(hooman, ProjectRole.CONTRIBUTOR), membership(nikoo, ProjectRole.MANAGER)));

    String detail = dispatcher.dispatch(emailRule("{\"subject\":\"s\",\"to\":\"project_managers\"}"), Map.of());

    assertThat(detail).isEqualTo("Emailed 1 recipient");
    verify(email, never()).sendAutomationEmail(eq("hooman@goldorigins.com"), anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("'assignee' emails the triggering task's assignee; explicit addresses are accepted and de-duplicated")
  void sendEmailToAssigneeAndExplicitAddresses() {
    User assignee = member(3L, "dodangeh", "h.dodangeh@goldorigins.com");
    Task task = Task.builder().id(77L).title("T").assignee(assignee.getPerson()).build();
    when(tasks.findById(77L)).thenReturn(Optional.of(task));

    String detail = dispatcher.dispatch(
        emailRule("{\"subject\":\"s\",\"to\":[\"assignee\",\"H.Dodangeh@goldorigins.com\",\"boss@goldorigins.com\",\"nonsense\"]}"),
        Map.of("taskId", 77L));

    assertThat(detail).isEqualTo("Emailed 2 recipients");
    verify(email, times(1)).sendAutomationEmail(eq("h.dodangeh@goldorigins.com"), anyString(), anyString(), anyString(), eq("/backlog/77"));
    verify(email).sendAutomationEmail(eq("boss@goldorigins.com"), eq("boss@goldorigins.com"), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("SEND_EMAIL fails honestly when SMTP is not configured")
  void sendEmailFailsWhenSmtpMissing() {
    when(email.isEnabled()).thenReturn(false);

    assertThatThrownBy(() -> dispatcher.dispatch(emailRule("{\"subject\":\"s\"}"), Map.of()))
        .hasMessageContaining("not configured");
    verify(email, never()).sendAutomationEmail(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("SEND_EMAIL fails when nobody has a usable address (placeholder domains do not count)")
  void sendEmailFailsWithNoRecipients() {
    User admin = member(1L, "admin", "admin@shipflow.local");
    when(members.findByProjectIdEager(5L)).thenReturn(List.of(membership(admin, ProjectRole.MANAGER)));

    assertThatThrownBy(() -> dispatcher.dispatch(emailRule("{\"subject\":\"s\"}"), Map.of()))
        .hasMessageContaining("Nobody to email");
  }
}
