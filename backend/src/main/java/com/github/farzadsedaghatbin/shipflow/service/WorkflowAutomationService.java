package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.workflow.CreateWorkflowAutomationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationExecutionDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationTemplateDto;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationTemplate;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationTemplateRepository;
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

    WorkflowAutomation saved = automationRepository.save(builder.build());
    log.info("Created workflow automation '{}' (id={}) for project {}", saved.getName(), saved.getId(), project.getId());
    return toDto(saved);
  }

  public WorkflowAutomationDto createFromTemplate(Long templateId, Long projectId, String name) {
    WorkflowAutomationTemplate template = templateRepository.findById(templateId)
        .orElseThrow(() -> new EntityNotFoundException("Template not found: " + templateId));
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

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

    WorkflowAutomation saved = automationRepository.save(automation);
    log.info("Created automation '{}' from template '{}' for project {}", saved.getName(), template.getName(), projectId);
    return toDto(saved);
  }

  public WorkflowAutomationDto update(Long id, CreateWorkflowAutomationRequest request) {
    WorkflowAutomation automation = findOrThrow(id);
    automation.setName(request.getName());
    automation.setDescription(request.getDescription());
    automation.setTriggerType(request.getTriggerType());
    automation.setTriggerConfig(request.getTriggerConfig());
    automation.setActionType(request.getActionType());
    automation.setActionConfig(request.getActionConfig());
    automation.setEnabled(request.isEnabled());
    return toDto(automationRepository.save(automation));
  }

  public WorkflowAutomationDto toggleEnabled(Long id) {
    WorkflowAutomation automation = findOrThrow(id);
    automation.setEnabled(!automation.isEnabled());
    return toDto(automationRepository.save(automation));
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
