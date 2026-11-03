package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.DashboardNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.notification.NotificationProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("AgentLoopDelivery")
class AgentLoopDeliveryTest {

  private NotificationProvider slack;
  private NotificationProvider teams;
  private UserProjectRepository userProjects;
  private DashboardNotificationService notifications;
  private AgentLoopDelivery delivery;
  private Project project;
  private final AgentLoopReport report = new AgentLoopReport("Standup digest — P", "✅ Finished (1)\n  • A — Sara", true, "PROJECT", 5L);

  private static AgentLoopConfig cfg(String json) {
    return AgentLoopConfig.parse(json, new ObjectMapper());
  }

  private static NotificationProvider provider(String name, boolean active) {
    NotificationProvider p = mock(NotificationProvider.class);
    when(p.getProviderName()).thenReturn(name);
    when(p.isActive()).thenReturn(active);
    return p;
  }

  @BeforeEach
  void setUp() {
    slack = provider("slack", true);
    teams = provider("teams", true);
    userProjects = mock(UserProjectRepository.class);
    notifications = mock(DashboardNotificationService.class);
    delivery = new AgentLoopDelivery(List.of(slack, teams), userProjects, notifications);
    project = new Project();
    project.setId(5L);
  }

  @Test
  @DisplayName("Slack and Teams each receive the composed message, the type, the channel override and the entity")
  void sendsToExternalChannels() {
    AgentLoopDelivery.Outcome o = delivery.deliver(project, "Daily", "AGENT_STANDUP_DIGEST", report, "Calm day.",
        cfg("{\"channels\":[\"SLACK\",\"TEAMS\"],\"channel\":\"#standup\"}"));

    ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
    verify(slack).sendNotification(eq("AGENT_STANDUP_DIGEST"), msg.capture(), eq("#standup"), eq("PROJECT"), eq(5L));
    verify(teams).sendNotification(eq("AGENT_STANDUP_DIGEST"), anyString(), eq("#standup"), eq("PROJECT"), eq(5L));
    assertThat(msg.getValue()).startsWith("*Standup digest — P*\nCalm day.\n\n✅ Finished (1)").endsWith("— ShipFlow Agent");
    assertThat(o.sent()).containsExactlyInAnyOrder("Slack", "Teams");
    assertThat(o.skipped()).isEmpty();
    verify(notifications, never()).notifyAutomationTriggered(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A requested channel that isn't configured is reported as skipped, not silently dropped")
  void unconfiguredChannelIsVisible() {
    when(teams.isActive()).thenReturn(false);

    AgentLoopDelivery.Outcome o = delivery.deliver(project, "Daily", "T", report, null, cfg("{\"channels\":[\"SLACK\",\"TEAMS\"]}"));

    assertThat(o.sent()).containsExactly("Slack");
    assertThat(o.skipped()).containsExactly("Teams (not configured)");
    verify(teams, never()).sendNotification(any(), any(), any(), any(), any());
    assertThat(o.describe()).isEqualTo("Sent to Slack; skipped: Teams (not configured)");
  }

  @Test
  @DisplayName("A provider that is absent altogether is also 'not configured'")
  void missingProvider() {
    AgentLoopDelivery d = new AgentLoopDelivery(List.of(slack), userProjects, notifications);

    AgentLoopDelivery.Outcome o = d.deliver(project, "Daily", "T", report, null, cfg("{\"channels\":[\"TEAMS\"]}"));

    assertThat(o.anySent()).isFalse();
    assertThat(o.describe()).isEqualTo("Nothing was sent; skipped: Teams (not configured)");
  }

  @Test
  @DisplayName("A provider that throws doesn't stop the other channels")
  void failureIsIsolated() {
    doThrow(new RuntimeException("webhook 500")).when(slack).sendNotification(any(), any(), any(), any(), any());

    AgentLoopDelivery.Outcome o = delivery.deliver(project, "Daily", "T", report, null, cfg("{\"channels\":[\"SLACK\",\"TEAMS\"]}"));

    assertThat(o.sent()).containsExactly("Teams");
    assertThat(o.skipped()).hasSize(1).first().asString().startsWith("Slack (failed: webhook 500");
  }

  @Test
  @DisplayName("In-app: every project member is notified with a short body")
  void inApp() {
    User a = new User();
    a.setId(1L);
    User b = new User();
    b.setId(2L);
    UserProject ua = new UserProject();
    ua.setUser(a);
    UserProject ub = new UserProject();
    ub.setUser(b);
    when(userProjects.findByProjectIdEager(5L)).thenReturn(List.of(ua, ub));
    AgentLoopReport big = new AgentLoopReport("T", "x".repeat(2000), true, "PROJECT", 5L);

    AgentLoopDelivery.Outcome o = delivery.deliver(project, "Daily", "T", big, "Short.", cfg("{\"channels\":[\"IN_APP\"]}"));

    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(notifications, times(2)).notifyAutomationTriggered(any(User.class), eq("Daily"), body.capture(), eq(null));
    assertThat(body.getValue()).hasSize(AgentLoopDelivery.IN_APP_MAX).endsWith("…").startsWith("Short.");
    assertThat(o.sent()).containsExactly("In-app (2 members)");
  }

  @Test
  @DisplayName("In-app for a project with no members is reported as skipped")
  void inAppNoMembers() {
    when(userProjects.findByProjectIdEager(5L)).thenReturn(List.of());

    AgentLoopDelivery.Outcome o = delivery.deliver(project, "Daily", "T", report, null, cfg("{\"channels\":[\"IN_APP\"]}"));

    assertThat(o.anySent()).isFalse();
    assertThat(o.skipped()).containsExactly("In-app (the project has no members)");
  }

  @Test
  @DisplayName("compose() omits the summary line cleanly when there is none")
  void composeWithoutSummary() {
    assertThat(AgentLoopDelivery.compose(report, null))
        .isEqualTo("*Standup digest — P*\n\n✅ Finished (1)\n  • A — Sara\n\n— ShipFlow Agent");
  }
}
