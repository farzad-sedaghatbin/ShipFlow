package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.workflow.CreateWorkflowAutomationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationDto;
import com.github.farzadsedaghatbin.shipflow.dto.workflow.WorkflowAutomationTemplateDto;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomation;
import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationTemplate;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationExecutionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkflowAutomationTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowAutomationServiceTest {

  @Mock private WorkflowAutomationRepository automationRepository;
  @Mock private WorkflowAutomationTemplateRepository templateRepository;
  @Mock private WorkflowAutomationExecutionRepository executionRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks
  private WorkflowAutomationService service;

  private Project testProject;
  private WorkflowAutomation testAutomation;
  private WorkflowAutomationTemplate testTemplate;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("testuser", "password"));
    testProject = new Project();
    testProject.setId(1L);

    testTemplate = WorkflowAutomationTemplate.builder()
        .id(1L)
        .name("Notify on task completed")
        .category("Tasks")
        .triggerType(TriggerType.TASK_COMPLETED)
        .actionType(ActionType.NOTIFY_PROJECT_MEMBERS)
        .defaultTriggerConfig("{}")
        .defaultActionConfig("{\"message\":\"Task done\"}")
        .sortOrder(1)
        .build();

    testAutomation = WorkflowAutomation.builder()
        .id(1L)
        .name("My Automation")
        .project(testProject)
        .triggerType(TriggerType.TASK_COMPLETED)
        .actionType(ActionType.NOTIFY_PROJECT_MEMBERS)
        .enabled(true)
        .executionCount(0L)
        .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getByProject_ShouldReturnAutomations() {
    when(automationRepository.findByProjectIdNotDeleted(1L))
        .thenReturn(Arrays.asList(testAutomation));

    List<WorkflowAutomationDto> result = service.getByProject(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("My Automation");
    assertThat(result.get(0).getTriggerType()).isEqualTo(TriggerType.TASK_COMPLETED);
  }

  @Test
  void getById_WhenExists_ShouldReturnDto() {
    when(automationRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testAutomation));

    WorkflowAutomationDto result = service.getById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.isEnabled()).isTrue();
  }

  @Test
  void getById_WhenNotExists_ShouldThrow() {
    when(automationRepository.findByIdNotDeleted(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void create_ShouldPersistAndReturnDto() {
    CreateWorkflowAutomationRequest request = new CreateWorkflowAutomationRequest();
    request.setName("Alert on scope creep");
    request.setProjectId(1L);
    request.setTriggerType(TriggerType.SCOPE_CREEP_DETECTED);
    request.setActionType(ActionType.NOTIFY_PROJECT_MEMBERS);
    request.setEnabled(true);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(automationRepository.save(any())).thenReturn(testAutomation);

    WorkflowAutomationDto result = service.create(request);

    assertThat(result).isNotNull();
    verify(automationRepository, times(1)).save(any());
  }

  @Test
  void create_WhenProjectNotFound_ShouldThrow() {
    CreateWorkflowAutomationRequest request = new CreateWorkflowAutomationRequest();
    request.setProjectId(99L);
    request.setTriggerType(TriggerType.TASK_COMPLETED);
    request.setActionType(ActionType.NOTIFY_PROJECT_MEMBERS);

    when(projectRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void createFromTemplate_ShouldUseTemplateDefaults() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(automationRepository.save(any())).thenAnswer(inv -> {
      WorkflowAutomation a = inv.getArgument(0);
      a.setId(2L);
      return a;
    });

    WorkflowAutomationDto result = service.createFromTemplate(1L, 1L, null);

    assertThat(result.getName()).isEqualTo("Notify on task completed");
    assertThat(result.getTriggerType()).isEqualTo(TriggerType.TASK_COMPLETED);
    assertThat(result.getActionType()).isEqualTo(ActionType.NOTIFY_PROJECT_MEMBERS);
  }

  @Test
  void toggleEnabled_ShouldFlipEnabledState() {
    when(automationRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testAutomation));
    when(automationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    WorkflowAutomationDto result = service.toggleEnabled(1L);

    assertThat(result.isEnabled()).isFalse();
  }

  @Test
  void getTemplates_ShouldReturnAllTemplates() {
    when(templateRepository.findAllByOrderBySortOrderAsc())
        .thenReturn(Arrays.asList(testTemplate));

    List<WorkflowAutomationTemplateDto> result = service.getTemplates();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Notify on task completed");
    assertThat(result.get(0).getCategory()).isEqualTo("Tasks");
  }

  @Test
  void delete_ShouldSoftDeleteAutomation() {
    when(automationRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testAutomation));
    when(automationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.delete(1L);

    assertThat(testAutomation.getDeletedAt()).isNotNull();
    verify(automationRepository).save(testAutomation);
  }
}
