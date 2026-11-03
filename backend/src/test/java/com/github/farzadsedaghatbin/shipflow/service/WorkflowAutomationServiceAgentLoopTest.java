package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.AgentLoopPreviewDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.CreateWorkflowAutomationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationExecutionDto;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationTemplate;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AutomationExecutionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.license.LicenseLimits;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationTemplateRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

/** The S71 rules in {@link WorkflowAutomationService}: schedule + agent-loop validation and next-run upkeep. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowAutomationService — agent loops")
class WorkflowAutomationServiceAgentLoopTest {

  private static final String CRON = "{\"cron\":\"0 0 9 * * MON-FRI\",\"timezone\":\"UTC\"}";
  private static final String CHANNELS = "{\"channels\":[\"SLACK\"]}";

  @Mock private WorkflowAutomationRepository automationRepository;
  @Mock private WorkflowAutomationTemplateRepository templateRepository;
  @Mock private WorkflowAutomationExecutionRepository executionRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private UserRepository userRepository;
  @Mock private LicenseLimits licenseLimits;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private ProjectPermissionService projectPermissionService;
  @Mock private AgentLoopService agentLoopService;
  @Mock private WorkflowAutomationEngine engine;

  @InjectMocks private WorkflowAutomationService service;

  private Project project;

  @BeforeEach
  void setUp() {
    when(licenseLimits.automationsUnlimited()).thenReturn(true);
    project = new Project();
    project.setId(1L);
    project.setProjectType(ProjectType.SHAPE_UP);
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(automationRepository.save(any(WorkflowAutomation.class))).thenAnswer(inv -> {
      WorkflowAutomation a = inv.getArgument(0);
      if (a.getId() == null) {
        a.setId(10L);
      }
      return a;
    });
  }

  private CreateWorkflowAutomationRequest request(TriggerType trigger, String triggerConfig, ActionType action, String actionConfig, boolean enabled) {
    CreateWorkflowAutomationRequest r = new CreateWorkflowAutomationRequest();
    r.setName("Daily");
    r.setProjectId(1L);
    r.setTriggerType(trigger);
    r.setTriggerConfig(triggerConfig);
    r.setActionType(action);
    r.setActionConfig(actionConfig);
    r.setEnabled(enabled);
    return r;
  }

  private WorkflowAutomation existing(TriggerType trigger, ActionType action, boolean enabled) {
    WorkflowAutomation a = new WorkflowAutomation();
    a.setId(10L);
    a.setName("Daily");
    a.setProject(project);
    a.setTriggerType(trigger);
    a.setTriggerConfig(CRON);
    a.setActionType(action);
    a.setActionConfig(CHANNELS);
    a.setEnabled(enabled);
    when(automationRepository.findByIdNotDeleted(10L)).thenReturn(Optional.of(a));
    return a;
  }

  // ── create ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("An enabled SCHEDULE loop gets its first slot, in the future, and project access is required")
  void createSchedulesFirstSlot() {
    WorkflowAutomationDto dto = service.create(
        request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_STANDUP_DIGEST, CHANNELS, true));

    assertThat(dto.getNextRunAt()).isAfter(Instant.now());
    verify(projectPermissionService).requireProjectAccess(1L);
  }

  @Test
  @DisplayName("A disabled loop has no next run")
  void disabledHasNoSlot() {
    WorkflowAutomationDto dto = service.create(
        request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_STANDUP_DIGEST, CHANNELS, false));

    assertThat(dto.getNextRunAt()).isNull();
  }

  @Test
  @DisplayName("A bad schedule is refused before anything is saved")
  void badSchedule() {
    assertThatThrownBy(() -> service.create(
        request(TriggerType.SCHEDULE, "{\"cron\":\"* * * * * *\"}", ActionType.AGENT_STANDUP_DIGEST, CHANNELS, true)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once an hour");
    verify(automationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Standup digest and cycle health only run on a schedule")
  void loopsNeedSchedule() {
    for (ActionType action : new ActionType[] {ActionType.AGENT_STANDUP_DIGEST, ActionType.AGENT_CYCLE_HEALTH_CHECK}) {
      assertThatThrownBy(() -> service.create(request(TriggerType.TASK_CREATED, "{}", action, CHANNELS, true)))
          .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Schedule");
    }
    verify(automationRepository, never()).save(any());
  }

  @Test
  @DisplayName("Betting prep may also run when a cycle ends, but not on arbitrary events")
  void bettingPrepTriggers() {
    service.create(request(TriggerType.CYCLE_ENDED, "{}", ActionType.AGENT_BETTING_PREP, CHANNELS, true));

    assertThatThrownBy(() -> service.create(request(TriggerType.TASK_CREATED, "{}", ActionType.AGENT_BETTING_PREP, CHANNELS, true)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("schedule or when a cycle ends");
  }

  @Test
  @DisplayName("Betting prep needs a Shape Up project")
  void bettingPrepShapeUpOnly() {
    project.setProjectType(ProjectType.SCRUM);

    assertThatThrownBy(() -> service.create(request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_BETTING_PREP, CHANNELS, true)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Shape Up");
  }

  @Test
  @DisplayName("Unreadable delivery settings are refused")
  void badActionConfig() {
    assertThatThrownBy(() -> service.create(
        request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_STANDUP_DIGEST, "{\"channels\":[\"EMAIL\"]}", true)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SLACK, TEAMS or IN_APP");
  }

  @Test
  @DisplayName("Someone without access to the project cannot point a loop at it")
  void projectAccessRequired() {
    doThrow(new AccessDeniedException("no")).when(projectPermissionService).requireProjectAccess(1L);

    assertThatThrownBy(() -> service.create(
        request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_STANDUP_DIGEST, CHANNELS, true)))
        .isInstanceOf(AccessDeniedException.class);
    verify(automationRepository, never()).save(any());
  }

  @Test
  @DisplayName("A non-agent action on a SCHEDULE is allowed, schedule-validated, and needs no extra project check")
  void scheduleWithOrdinaryAction() {
    WorkflowAutomationDto dto = service.create(
        request(TriggerType.SCHEDULE, CRON, ActionType.NOTIFY_PROJECT_MEMBERS, "{\"message\":\"Retro today\"}", true));

    assertThat(dto.getNextRunAt()).isNotNull();
    verify(projectPermissionService, never()).requireProjectAccess(any());
  }

  @Test
  @DisplayName("Event-triggered automations are not scheduled")
  void eventTriggerHasNoSlot() {
    assertThat(service.create(request(TriggerType.TASK_COMPLETED, "{}", ActionType.NOTIFY_PROJECT_MEMBERS, "{}", true))
        .getNextRunAt()).isNull();
  }

  // ── update / toggle / template ──────────────────────────────────────────────

  @Test
  @DisplayName("Disabling or re-timing a loop updates its next slot")
  void updateRecomputes() {
    existing(TriggerType.SCHEDULE, ActionType.AGENT_STANDUP_DIGEST, true);

    WorkflowAutomationDto off = service.update(10L,
        request(TriggerType.SCHEDULE, CRON, ActionType.AGENT_STANDUP_DIGEST, CHANNELS, false));
    assertThat(off.getNextRunAt()).isNull();

    WorkflowAutomationDto on = service.update(10L,
        request(TriggerType.SCHEDULE, "{\"cron\":\"0 0 17 * * FRI\"}", ActionType.AGENT_STANDUP_DIGEST, CHANNELS, true));
    assertThat(on.getNextRunAt()).isAfter(Instant.now());
  }

  @Test
  @DisplayName("Switching a loop's trigger away from SCHEDULE clears the slot")
  void updateAwayFromSchedule() {
    WorkflowAutomation a = existing(TriggerType.SCHEDULE, ActionType.AGENT_BETTING_PREP, true);
    a.setNextRunAt(Instant.now().plusSeconds(3600));

    WorkflowAutomationDto dto = service.update(10L,
        request(TriggerType.CYCLE_ENDED, "{}", ActionType.AGENT_BETTING_PREP, CHANNELS, true));

    assertThat(dto.getNextRunAt()).isNull();
  }

  @Test
  @DisplayName("Toggling a schedule on gives it a slot; toggling off removes it")
  void toggle() {
    existing(TriggerType.SCHEDULE, ActionType.AGENT_STANDUP_DIGEST, false);

    assertThat(service.toggleEnabled(10L).getNextRunAt()).isAfter(Instant.now());
    assertThat(service.toggleEnabled(10L).getNextRunAt()).isNull();
  }

  @Test
  @DisplayName("Creating from a built-in agent template schedules it")
  void fromTemplate() {
    WorkflowAutomationTemplate t = WorkflowAutomationTemplate.builder()
        .id(3L).name("Daily standup digest").category("Agent").triggerType(TriggerType.SCHEDULE)
        .defaultTriggerConfig(CRON).actionType(ActionType.AGENT_STANDUP_DIGEST)
        .defaultActionConfig("{\"channels\":[\"SLACK\",\"TEAMS\",\"IN_APP\"],\"useAi\":true}").build();
    when(templateRepository.findById(3L)).thenReturn(Optional.of(t));

    WorkflowAutomationDto dto = service.createFromTemplate(3L, 1L, null);

    assertThat(dto.getNextRunAt()).isAfter(Instant.now());
    assertThat(dto.getActionType()).isEqualTo(ActionType.AGENT_STANDUP_DIGEST);
  }

  // ── preview / run now ───────────────────────────────────────────────────────

  @Test
  @DisplayName("Preview returns what the loop would send and sends nothing")
  void preview() {
    WorkflowAutomation a = existing(TriggerType.SCHEDULE, ActionType.AGENT_STANDUP_DIGEST, true);
    when(agentLoopService.preview(a)).thenReturn(new AgentLoopService.Preview("T", "S", "F", "M", true));

    AgentLoopPreviewDto dto = service.preview(10L);

    assertThat(dto.getTitle()).isEqualTo("T");
    assertThat(dto.getSummary()).isEqualTo("S");
    assertThat(dto.getMessage()).isEqualTo("M");
    assertThat(dto.isWouldSend()).isTrue();
    verify(projectPermissionService).requireProjectAccess(1L);
    verify(engine, never()).runNow(any());
  }

  @Test
  @DisplayName("Run now goes through the engine and returns the execution record")
  void runNow() {
    WorkflowAutomation a = existing(TriggerType.SCHEDULE, ActionType.AGENT_CYCLE_HEALTH_CHECK, true);
    WorkflowAutomationExecution e = WorkflowAutomationExecution.builder()
        .automation(a).triggerEventType("SCHEDULE").status(AutomationExecutionStatus.SUCCESS)
        .resultMessage("Sent to Slack").build();
    when(engine.runNow(a)).thenReturn(e);

    WorkflowAutomationExecutionDto dto = service.runNow(10L);

    assertThat(dto.getResultMessage()).isEqualTo("Sent to Slack");
    assertThat(dto.getStatus()).isEqualTo(AutomationExecutionStatus.SUCCESS);
  }

  @Test
  @DisplayName("Only agent loops can be previewed or run on demand, and only by someone on the project")
  void onlyLoopsAndOnlyMembers() {
    existing(TriggerType.TASK_CREATED, ActionType.NOTIFY_PROJECT_MEMBERS, true);
    assertThatThrownBy(() -> service.preview(10L)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.runNow(10L)).isInstanceOf(IllegalArgumentException.class);
    verify(engine, never()).runNow(any());

    existing(TriggerType.SCHEDULE, ActionType.AGENT_STANDUP_DIGEST, true);
    doThrow(new AccessDeniedException("no")).when(projectPermissionService).requireProjectAccess(1L);
    assertThatThrownBy(() -> service.runNow(10L)).isInstanceOf(AccessDeniedException.class);
  }
}
