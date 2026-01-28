package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Project Type functionality (Kanban vs Shape Up).
 * 
 * This test class verifies:
 * - Default project type is SHAPE_UP for backward compatibility
 * - KANBAN projects automatically create a "Continuous Flow" cycle
 * - SHAPE_UP projects do NOT auto-create cycles
 * - Project type is correctly persisted and returned in DTOs
 * - Cycle creation behavior differs based on project type
 */
@ExtendWith(MockitoExtension.class)
class ProjectTypeTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private ProjectService projectService;

    private User testOwner;

    @BeforeEach
    void setUp() {
        lenient().when(localizationService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(i -> i.getArgument(0));
        lenient().when(localizationService.getMessage(anyString()))
                .thenAnswer(i -> i.getArgument(0));

        testOwner = User.builder()
                .id(1L)
                .username("owner")
                .role(UserRole.MANAGER)
                .build();
    }

    @Test
    void create_WithNoProjectType_ShouldDefaultToShapeUp() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Test Project");
        request.setProjectKey("TST");
        request.setOwnerId(1L);
        // Note: projectType not set, should default to SHAPE_UP

        Project savedProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .projectKey("TST")
                .projectType(ProjectType.SHAPE_UP)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectRepository.existsByProjectKey("TST")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
        when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

        // Act
        ProjectDTO result = projectService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProjectType()).isEqualTo(ProjectType.SHAPE_UP);
        
        // Verify project was saved with SHAPE_UP type
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getProjectType()).isEqualTo(ProjectType.SHAPE_UP);
        
        // Verify NO cycle was created for Shape Up projects
        verify(cycleRepository, never()).save(any(Cycle.class));
    }

    @Test
    void create_WithShapeUpType_ShouldNotCreateDefaultCycle() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Shape Up Project");
        request.setProjectKey("SUP");
        request.setOwnerId(1L);
        request.setProjectType(ProjectType.SHAPE_UP);

        Project savedProject = Project.builder()
                .id(1L)
                .name("Shape Up Project")
                .projectKey("SUP")
                .projectType(ProjectType.SHAPE_UP)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectRepository.existsByProjectKey("SUP")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
        when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

        // Act
        ProjectDTO result = projectService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProjectType()).isEqualTo(ProjectType.SHAPE_UP);
        
        // Verify NO cycle was automatically created
        verify(cycleRepository, never()).save(any(Cycle.class));
    }

    @Test
    void create_WithKanbanType_ShouldCreateContinuousFlowCycle() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Kanban Project");
        request.setProjectKey("KBN");
        request.setOwnerId(1L);
        request.setProjectType(ProjectType.KANBAN);

        Project savedProject = Project.builder()
                .id(1L)
                .name("Kanban Project")
                .projectKey("KBN")
                .projectType(ProjectType.KANBAN)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectRepository.existsByProjectKey("KBN")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(cycleRepository.countByProjectId(1L)).thenReturn(1L);
        when(cycleRepository.countActiveByProjectId(1L)).thenReturn(1L);

        // Act
        ProjectDTO result = projectService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProjectType()).isEqualTo(ProjectType.KANBAN);
        
        // Verify a default cycle was created for Kanban projects
        ArgumentCaptor<Cycle> cycleCaptor = ArgumentCaptor.forClass(Cycle.class);
        verify(cycleRepository).save(cycleCaptor.capture());
        
        Cycle createdCycle = cycleCaptor.getValue();
        assertThat(createdCycle.getName()).isEqualTo("Continuous Flow");
        assertThat(createdCycle.getProject()).isEqualTo(savedProject);
        assertThat(createdCycle.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(createdCycle.getEndDate()).isEqualTo(LocalDate.of(2099, 12, 31));
        assertThat(createdCycle.getPhase()).isEqualTo(CyclePhase.BUILD);
        assertThat(createdCycle.getIsActive()).isTrue();
    }

    @Test
    void create_WithKanbanType_ShouldReturnProjectWithKanbanType() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Kanban Board");
        request.setProjectKey("KAN");
        request.setOwnerId(1L);
        request.setProjectType(ProjectType.KANBAN);

        Project savedProject = Project.builder()
                .id(2L)
                .name("Kanban Board")
                .projectKey("KAN")
                .projectType(ProjectType.KANBAN)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectRepository.existsByProjectKey("KAN")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(cycleRepository.countByProjectId(2L)).thenReturn(1L);
        when(cycleRepository.countActiveByProjectId(2L)).thenReturn(1L);

        // Act
        ProjectDTO result = projectService.create(request);

        // Assert
        assertThat(result.getProjectType()).isEqualTo(ProjectType.KANBAN);
        assertThat(result.getName()).isEqualTo("Kanban Board");
    }

    @Test
    void update_ShouldPreserveProjectType() {
        // Arrange - existing Kanban project
        Project existingProject = Project.builder()
                .id(1L)
                .name("Old Name")
                .projectKey("KBN")
                .projectType(ProjectType.KANBAN)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        CreateProjectRequest updateRequest = new CreateProjectRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setProjectKey("KBN");
        updateRequest.setOwnerId(1L);
        updateRequest.setProjectType(ProjectType.KANBAN);

        Project updatedProject = Project.builder()
                .id(1L)
                .name("Updated Name")
                .projectKey("KBN")
                .projectType(ProjectType.KANBAN)
                .owner(testOwner)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existingProject));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(projectRepository.save(any(Project.class))).thenReturn(updatedProject);
        when(cycleRepository.countByProjectId(1L)).thenReturn(1L);
        when(cycleRepository.countActiveByProjectId(1L)).thenReturn(1L);

        // Act
        ProjectDTO result = projectService.update(1L, updateRequest);

        // Assert
        assertThat(result.getProjectType()).isEqualTo(ProjectType.KANBAN);
        assertThat(result.getName()).isEqualTo("Updated Name");
        
        // Verify project type was preserved during update
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getProjectType()).isEqualTo(ProjectType.KANBAN);
    }
}
