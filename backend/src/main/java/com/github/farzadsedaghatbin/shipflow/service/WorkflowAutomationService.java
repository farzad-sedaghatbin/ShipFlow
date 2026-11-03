package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.workflow.CreateWorkflowAutomationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationExecutionDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationTemplateDto;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationTemplate;
import com.github.farzadsedaghatbin.shipflow.license.AutomationLimitExceededException;
import com.github.farzadsedaghatbin.shipflow.license.LicenseLimits;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.AgentLoopPreviewDto;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopConfig;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopService;
import com.github.farzadsedaghatbin.shipflow.service.automation.WorkflowSchedule;
import java.time.Instant;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkflowAutomationService {

  private final WorkflowAutomationRepository automationRepository;
  private final WorkflowAutomationTemplateRepository templateRepository;
  private final WorkflowAutomationExecutionRepository executionRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final LicenseLimits licenseLimits;
  private final ObjectMapper objectMapper;
  private final ProjectPermissionService projectPermissionService;
  private final AgentLoopService agentLoopService;
  private final WorkflowAutomationEngine engine;

  @Transactional(readOnly = true)
  public List<WorkflowAutomationDto> getByProject(Long projectId) {
    return automationRepository.findByProjectIdNotDeleted(projectId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public WorkflowAutomationDto getById(Long id) {
    return toDto(findOrThrow(id));
  }

  public WorkflowAutomationDto create(CreateWorkflowAutomationRequest request) {
    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + request.getProjectId()));

    validate(project, request.getTriggerType(), request.getTriggerConfig(), request.getActionType(),
        request.getActionConfig());
    if (request.isEnabled()) {
      assertAutomationCanBeEnabled();
    }

    WorkflowAutomation.WorkflowAutomationBuilder builder = WorkflowAutomation.builder()
        .name(request.getName())
        .description(request.getDescription())
        .project(project)
        .triggerType(request.getTriggerType())
        .triggerConfig(request.getTriggerConfig())
        .actionType(request.getActionType())
        .actionConfig(request.getActionConfig())
        .enabled(request.isEnabled());

    if (request.getTemplateId() != null) {
      templateRepository.findById(request.getTemplateId())
          .ifPresent(builder::template);
    }

    WorkflowAutomation toSave = builder.build();
    refreshSchedule(toSave);
    WorkflowAutomation saved = automationRepository.save(toSave);
    log.info("Created workflow automation '{}' (id={}) for project {}", saved.getName(), saved.getId(), project.getId());
    return toDto(saved);
  }

  public WorkflowAutomationDto createFromTemplate(Long templateId, Long projectId, String name) {
    WorkflowAutomationTemplate template = templateRepository.findById(templateId)
        .orElseThrow(() -> new EntityNotFoundException("Template not found: " + templateId));
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

    // Always created enabled (see .enabled(true) below).
    assertAutomationCanBeEnabled();

    WorkflowAutomation automation = WorkflowAutomation.builder()
        .name(name != null ? name : template.getName())
        .description(template.getDescription())
        .project(project)
        .triggerType(template.getTriggerType())
        .triggerConfig(template.getDefaultTriggerConfig())
        .actionType(template.getActionType())
        .actionConfig(template.getDefaultActionConfig())
        .template(template)
        .enabled(true)
        .build();

    validate(project, automation.getTriggerType(), automation.getTriggerConfig(), automation.getActionType(),
        automation.getActionConfig());
    refreshSchedule(automation);
    WorkflowAutomation saved = automationRepository.save(automation);
    log.info("Created automation '{}' from template '{}' for project {}", saved.getName(), template.getName(), projectId);
    return toDto(saved);
  }

  public WorkflowAutomationDto update(Long id, CreateWorkflowAutomationRequest request) {
    WorkflowAutomation automation = findOrThrow(id);
    validate(automation.getProject(), request.getTriggerType(), request.getTriggerConfig(),
        request.getActionType(), request.getActionConfig());
    if (request.isEnabled() && !automation.isEnabled()) {
      assertAutomationCanBeEnabled();
    }
    automation.setName(request.getName());
    automation.setDescription(request.getDescription());
    automation.setTriggerType(request.getTriggerType());
    automation.setTriggerConfig(request.getTriggerConfig());
    automation.setActionType(request.getActionType());
    automation.setActionConfig(request.getActionConfig());
    automation.setEnabled(request.isEnabled());
    refreshSchedule(automation);
    return toDto(automationRepository.save(automation));
  }

  public WorkflowAutomationDto toggleEnabled(Long id) {
    WorkflowAutomation automation = findOrThrow(id);
    if (!automation.isEnabled()) {
      assertAutomationCanBeEnabled();
    }
    automation.setEnabled(!automation.isEnabled());
    refreshSchedule(automation);
    return toDto(automationRepository.save(automation));
  }

  /**
   * Throws {@link AutomationLimitExceededException} when enabling one more automation would
   * exceed {@link LicenseLimits#enabledAutomationCap()} under a Community Edition (MISSING/EXPIRED)
   * licence. No-op (unlimited) under a VALID/GRACE licence. Never disables an already-enabled
   * automation — callers must check this BEFORE flipping one to enabled.
   */
  private void assertAutomationCanBeEnabled() {
    if (licenseLimits.automationsUnlimited()) {
      return;
    }
    int cap = licenseLimits.enabledAutomationCap();
    if (automationRepository.countEnabledNotDeleted() >= cap) {
      throw new AutomationLimitExceededException(cap);
    }
  }

  /** Current enabled, non-deleted automation count — used by the licence status endpoint. */
  @Transactional(readOnly = true)
  public long countEnabledAutomations() {
    return automationRepository.countEnabledNotDeleted();
  }

  public void delete(Long id) {
    WorkflowAutomation automation = findOrThrow(id);
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    userRepository.findByUsername(username).ifPresent(automation::setDeletedBy);
    automation.setDeletedAt(LocalDateTime.now());
    automationRepository.save(automation);
    log.info("Soft-deleted automation id={}", id);
  }

  @Transactional(readOnly = true)
  public List<WorkflowAutomationTemplateDto> getTemplates() {
    return templateRepository.findAllByOrderBySortOrderAsc().stream()
        .map(this::toTemplateDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WorkflowAutomationTemplateDto> getTemplatesByCategory(String category) {
    return templateRepository.findByCategoryOrderBySortOrderAsc(category).stream()
        .map(this::toTemplateDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WorkflowAutomationExecutionDto> getExecutionLog(Long automationId, int limit) {
    return executionRepository
        .findRecentByAutomationId(automationId, PageRequest.of(0, limit))
        .stream()
        .map(this::toExecutionDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<WorkflowAutomationExecutionDto> getProjectExecutionLog(Long projectId, int limit) {
    return executionRepository
        .findRecentByProjectId(projectId, PageRequest.of(0, limit))
        .stream()
        .map(this::toExecutionDto)
        .collect(Collectors.toList());
  }

  // ── Agent loops (S71): validation, preview, run now ─────────────────────────

  /**
   * Rejects configurations that cannot work, with a message fit to show the person who made them:
   * a SCHEDULE needs a valid, not-too-frequent cron; an agent loop needs a trigger that makes sense
   * for it, readable delivery settings, a project the caller can access, and — for betting prep — a
   * Shape Up project.
   */
  void validate(Project project, TriggerType trigger, String triggerConfig, ActionType action, String actionConfig) {
    if (trigger == TriggerType.SCHEDULE) {
      WorkflowSchedule.parse(triggerConfig, objectMapper);
    }
    if (!AgentLoopService.isLoopAction(action)) {
      return;
    }
    boolean ok = trigger == TriggerType.SCHEDULE
        || (action == ActionType.AGENT_BETTING_PREP && trigger == TriggerType.CYCLE_ENDED);
    if (!ok) {
      throw new IllegalArgumentException(action == ActionType.AGENT_BETTING_PREP
          ? "Betting prep runs on a schedule or when a cycle ends"
          : "This agent loop runs on a schedule — pick the Schedule trigger");
    }
    AgentLoopConfig.parse(actionConfig, objectMapper);
    if (action == ActionType.AGENT_BETTING_PREP && project.getProjectType() != ProjectType.SHAPE_UP) {
      throw new IllegalArgumentException("Betting prep only applies to Shape Up projects");
    }
    // These loops read a project's data and post it to chat channels: the person must be on it.
    projectPermissionService.requireProjectAccess(project.getId());
  }

  /** SCHEDULE + enabled → the next slot from now; anything else → none. */
  private void refreshSchedule(WorkflowAutomation automation) {
    if (automation.getTriggerType() == TriggerType.SCHEDULE && automation.isEnabled()) {
      automation.setNextRunAt(
          WorkflowSchedule.parse(automation.getTriggerConfig(), objectMapper).next(Instant.now()).orElse(null));
    } else {
      automation.setNextRunAt(null);
    }
  }

  /** Generates what the loop would send right now, without sending anything. */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public AgentLoopPreviewDto preview(Long id) {
    WorkflowAutomation automation = requireLoop(id);
    AgentLoopService.Preview p = agentLoopService.preview(automation);
    return AgentLoopPreviewDto.builder()
        .title(p.title()).summary(p.summary()).facts(p.facts()).message(p.message()).wouldSend(p.wouldSend())
        .build();
  }

  /** Runs the loop now, as if its slot had arrived, and returns the execution record. */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public WorkflowAutomationExecutionDto runNow(Long id) {
    return toExecutionDto(engine.runNow(requireLoop(id)));
  }

  private WorkflowAutomation requireLoop(Long id) {
    WorkflowAutomation automation = findOrThrow(id);
    if (!AgentLoopService.isLoopAction(automation.getActionType())) {
      throw new IllegalArgumentException("Only agent loop automations can be previewed or run on demand");
    }
    projectPermissionService.requireProjectAccess(automation.getProject().getId());
    return automation;
  }

  private WorkflowAutomation findOrThrow(Long id) {
    return automationRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new EntityNotFoundException("WorkflowAutomation not found: " + id));
  }

  WorkflowAutomationDto toDto(WorkflowAutomation a) {
    return WorkflowAutomationDto.builder()
        .id(a.getId())
        .name(a.getName())
        .description(a.getDescription())
        .projectId(a.getProject() != null ? a.getProject().getId() : null)
        .triggerType(a.getTriggerType())
        .triggerConfig(a.getTriggerConfig())
        .actionType(a.getActionType())
        .actionConfig(a.getActionConfig())
        .enabled(a.isEnabled())
        .templateId(a.getTemplate() != null ? a.getTemplate().getId() : null)
        .templateName(a.getTemplate() != null ? a.getTemplate().getName() : null)
        .executionCount(a.getExecutionCount())
        .lastTriggeredAt(a.getLastTriggeredAt())
        .nextRunAt(a.getNextRunAt())
        .createdAt(a.getCreatedAt())
        .updatedAt(a.getUpdatedAt())
        .build();
  }

  private WorkflowAutomationTemplateDto toTemplateDto(WorkflowAutomationTemplate t) {
    return WorkflowAutomationTemplateDto.builder()
        .id(t.getId())
        .name(t.getName())
        .description(t.getDescription())
        .category(t.getCategory())
        .triggerType(t.getTriggerType())
        .defaultTriggerConfig(t.getDefaultTriggerConfig())
        .actionType(t.getActionType())
        .defaultActionConfig(t.getDefaultActionConfig())
        .iconName(t.getIconName())
        .builtIn(t.isBuiltIn())
        .sortOrder(t.getSortOrder())
        .build();
  }

  private WorkflowAutomationExecutionDto toExecutionDto(
      com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationExecution e) {
    return WorkflowAutomationExecutionDto.builder()
        .id(e.getId())
        .automationId(e.getAutomation() != null ? e.getAutomation().getId() : null)
        .automationName(e.getAutomation() != null ? e.getAutomation().getName() : null)
        .triggerEventType(e.getTriggerEventType())
        .triggerEventData(e.getTriggerEventData())
        .status(e.getStatus())
        .resultMessage(e.getResultMessage())
        .executedAt(e.getExecutedAt())
        .build();
  }
}
