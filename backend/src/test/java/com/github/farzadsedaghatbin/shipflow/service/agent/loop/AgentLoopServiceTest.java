package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentLoopService")
class AgentLoopServiceTest {

  private ProjectRepository projects;
  private CycleRepository cycles;
  private StandupDigestBuilder standup;
  private CycleHealthBuilder health;
  private BettingPrepBuilder betting;
  private AgentLoopNarrator narrator;
  private AgentLoopDelivery delivery;
  private AgentLoopService service;
  private Project project;

  private static final AgentLoopReport BUSY = new AgentLoopReport("Standup digest — P", "facts", true, "PROJECT", 5L);
  private static final AgentLoopReport QUIET = new AgentLoopReport("Standup digest — P", "nothing", false, "PROJECT", 5L);

  @BeforeEach
  void setUp() {
    projects = mock(ProjectRepository.class);
    cycles = mock(CycleRepository.class);
    standup = mock(StandupDigestBuilder.class);
    health = mock(CycleHealthBuilder.class);
    betting = mock(BettingPrepBuilder.class);
    narrator = mock(AgentLoopNarrator.class);
    delivery = mock(AgentLoopDelivery.class);
    service = new AgentLoopService(new ObjectMapper(), projects, cycles, standup, health, betting, narrator, delivery);
    project = new Project();
    project.setId(5L);
    project.setProjectType(ProjectType.SHAPE_UP);
    when(projects.findById(5L)).thenReturn(Optional.of(project));
  }

  private WorkflowAutomation automation(ActionType type, String config) {
    WorkflowAutomation a = new WorkflowAutomation();
    a.setId(7L);
    a.setName("Daily");
    a.setProject(project);
    a.setActionType(type);
    a.setActionConfig(config);
    return a;
  }

  private static AgentLoopDelivery.Outcome sent(String... channels) {
    return new AgentLoopDelivery.Outcome(List.of(channels), List.of());
  }

  @Test
  @DisplayName("Builds the report, asks for a summary, delivers, and returns what happened")
  void runsAndReports() {
    when(standup.build(eq(project), any(), any())).thenReturn(BUSY);
    when(narrator.summarize(BUSY)).thenReturn("Calm day.");
    when(delivery.deliver(eq(project), eq("Daily"), eq("AGENT_STANDUP_DIGEST"), eq(BUSY), eq("Calm day."), any()))
        .thenReturn(sent("Slack"));

    String result = service.run(automation(ActionType.AGENT_STANDUP_DIGEST, "{\"channels\":[\"SLACK\"]}"), Map.of());

    assertThat(result).isEqualTo("Sent to Slack (with an AI summary)");
  }

  @Test
  @DisplayName("useAi=false never calls the model")
  void aiOff() {
    when(standup.build(eq(project), any(), any())).thenReturn(BUSY);
    when(delivery.deliver(any(), any(), any(), any(), eq(null), any())).thenReturn(sent("In-app (3 members)"));

    String result = service.run(automation(ActionType.AGENT_STANDUP_DIGEST, "{\"useAi\":false}"), Map.of());

    assertThat(result).isEqualTo("Sent to In-app (3 members)");
    verify(narrator, never()).summarize(any());
  }

  @Test
  @DisplayName("A quiet standup is skipped without posting anything")
  void quietStandup() {
    when(standup.build(eq(project), any(), any())).thenReturn(QUIET);

    String result = service.run(automation(ActionType.AGENT_STANDUP_DIGEST, "{}"), Map.of());

    assertThat(result).isEqualTo("Skipped: nothing to report");
    verify(delivery, never()).deliver(any(), any(), any(), any(), any(), any());
    verify(narrator, never()).summarize(any());
  }

  @Test
  @DisplayName("Cycle health stays quiet when healthy (default) but posts when onlyWhenAtRisk is off")
  void healthQuietUnlessAsked() {
    when(health.build(eq(project), any(), any())).thenReturn(QUIET);

    assertThat(service.run(automation(ActionType.AGENT_CYCLE_HEALTH_CHECK, "{}"), Map.of()))
        .isEqualTo("Skipped: no risks found");

    when(delivery.deliver(any(), any(), any(), any(), any(), any())).thenReturn(sent("Teams"));
    assertThat(service.run(automation(ActionType.AGENT_CYCLE_HEALTH_CHECK, "{\"onlyWhenAtRisk\":false,\"channels\":[\"TEAMS\"]}"), Map.of()))
        .startsWith("Sent to Teams");
  }

  @Test
  @DisplayName("If nothing reached any channel the run FAILS, so the execution log never claims success")
  void nothingSentIsAFailure() {
    when(standup.build(eq(project), any(), any())).thenReturn(BUSY);
    when(delivery.deliver(any(), any(), any(), any(), any(), any()))
        .thenReturn(new AgentLoopDelivery.Outcome(List.of(), List.of("Slack (not configured)")));

    assertThatThrownBy(() -> service.run(automation(ActionType.AGENT_STANDUP_DIGEST, "{\"channels\":[\"SLACK\"]}"), Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Nothing was sent; skipped: Slack (not configured)");
  }

  @Test
  @DisplayName("Betting prep reacting to another project's cycle-ended event is skipped")
  void foreignCycleEvent() {
    Project other = new Project();
    other.setId(99L);
    Cycle foreign = new Cycle();
    foreign.setId(1L);
    foreign.setProject(other);
    when(cycles.findById(1L)).thenReturn(Optional.of(foreign));

    String result = service.run(automation(ActionType.AGENT_BETTING_PREP, "{}"), Map.of("cycleId", 1L));

    assertThat(result).isEqualTo("Skipped: the event belongs to another project");
    verify(betting, never()).build(any(), any());
  }

  @Test
  @DisplayName("Betting prep for its own project's cycle runs normally")
  void ownCycleEvent() {
    Cycle own = new Cycle();
    own.setId(2L);
    own.setProject(project);
    when(cycles.findById(2L)).thenReturn(Optional.of(own));
    when(betting.build(eq(project), any())).thenReturn(BUSY);
    when(delivery.deliver(any(), any(), eq("AGENT_BETTING_PREP"), any(), any(), any())).thenReturn(sent("Slack"));

    assertThat(service.run(automation(ActionType.AGENT_BETTING_PREP, "{\"channels\":[\"SLACK\"],\"useAi\":false}"), Map.of("cycleId", 2L)))
        .isEqualTo("Sent to Slack");
  }

  @Test
  @DisplayName("preview builds the same message without delivering it")
  void previewDoesNotDeliver() {
    when(standup.build(eq(project), any(), any())).thenReturn(BUSY);
    when(narrator.summarize(BUSY)).thenReturn("Calm day.");

    AgentLoopService.Preview p = service.preview(automation(ActionType.AGENT_STANDUP_DIGEST, "{}"));

    assertThat(p.title()).isEqualTo("Standup digest — P");
    assertThat(p.summary()).isEqualTo("Calm day.");
    assertThat(p.facts()).isEqualTo("facts");
    assertThat(p.message()).contains("Calm day.").contains("facts");
    assertThat(p.wouldSend()).isTrue();
    verify(delivery, never()).deliver(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("preview says when a real run would be skipped")
  void previewReportsSkip() {
    when(standup.build(eq(project), any(), any())).thenReturn(QUIET);

    assertThat(service.preview(automation(ActionType.AGENT_STANDUP_DIGEST, "{}")).wouldSend()).isFalse();
  }

  @Test
  @DisplayName("Unreadable delivery settings and a missing project are explained")
  void badInputs() {
    assertThatThrownBy(() -> service.run(automation(ActionType.AGENT_STANDUP_DIGEST, "{\"channels\":[\"EMAIL\"]}"), Map.of()))
        .isInstanceOf(IllegalArgumentException.class);

    WorkflowAutomation orphan = automation(ActionType.AGENT_STANDUP_DIGEST, "{}");
    orphan.setProject(null);
    assertThatThrownBy(() -> service.run(orphan, Map.of())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("isLoopAction recognises exactly the three agent loop actions")
  void isLoopAction() {
    assertThat(AgentLoopService.isLoopAction(ActionType.AGENT_STANDUP_DIGEST)).isTrue();
    assertThat(AgentLoopService.isLoopAction(ActionType.AGENT_CYCLE_HEALTH_CHECK)).isTrue();
    assertThat(AgentLoopService.isLoopAction(ActionType.AGENT_BETTING_PREP)).isTrue();
    assertThat(AgentLoopService.isLoopAction(ActionType.NOTIFY_ASSIGNEE)).isFalse();
    assertThat(AgentLoopService.isLoopAction(null)).isFalse();
  }
}
