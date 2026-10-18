package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProjectRepository userProjectRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private LocalizationService localizationService;

  @InjectMocks
  private ProjectService projectService;

  private Project testProject;
  private User testOwner;
  private CreateProjectRequest testRequest;

  @BeforeEach
  void setUp() {
    // Set up security context with authenticated user
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("owner", null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    lenient().when(localizationService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("project.key.exists"))
        return "Project key already exists";
      if (key.contains("project.has.cycles"))
        return "Cannot delete project with existing cycles";
      if (key.contains("project.cannot.delete.with.cycles"))
        return "Cannot delete project with existing cycles";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("project.key.exists"))
        return "Project key already exists";
      if (key.contains("project.has.cycles"))
        return "Cannot delete project with existing cycles";
      if (key.contains("project.cannot.delete.with.cycles"))
        return "Cannot delete project with existing cycles";
      return key;
    });

    testOwner = User.builder().id(1L).username("owner").role(UserRole.MANAGER).build();

    testProject = Project.builder().id(1L).name("Test Project").projectKey("TST").description("Test Description")
        .color("#FF0000").owner(testOwner).isActive(true).createdAt(LocalDateTime.now()).build();

    testRequest = new CreateProjectRequest();
    testRequest.setName("Test Project");
    testRequest.setProjectKey("TST");
    testRequest.setDescription("Test Description");
    testRequest.setColor("#FF0000");
    testRequest.setOwnerId(1L);

    // Mock user lookup for access control
    lenient().when(userRepository.findByUsername("owner")).thenReturn(Optional.of(testOwner));
    // Mock project access check - owner has access
    lenient().when(userProjectRepository.hasProjectAccess(1L, 1L)).thenReturn(true);

    // Inject self-reference so @Cacheable proxy calls work in unit tests
    ReflectionTestUtils.setField(projectService, "self", projectService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void findAll_ShouldReturnAllProjects() {
    when(projectRepository.findAll()).thenReturn(Arrays.asList(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(2L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(1L);

    List<ProjectDTO> result = projectService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Test Project");
    assertThat(result.get(0).getCycleCount()).isEqualTo(2);
    assertThat(result.get(0).getActiveCycleCount()).isEqualTo(1);
    verify(projectRepository).findAll();
  }

  @Test
  void findAllActive_ShouldReturnActiveProjects() {
    when(projectRepository.findAllActiveOrderByName()).thenReturn(Arrays.asList(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(2L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(1L);

    List<ProjectDTO> result = projectService.findAllActive();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getIsActive()).isTrue();
    verify(projectRepository).findAllActiveOrderByName();
  }

  @Test
  void findById_WhenExists_ShouldReturnProject() {
    when(projectRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(2L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(1L);

    ProjectDTO result = projectService.findById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Project");
    assertThat(result.getOwnerName()).isEqualTo("owner");
  }

  @Test
  void findById_WhenNotExists_ShouldThrowAccessDeniedException() {
    // With access control, when project doesn't exist, the access check fails first
    when(userProjectRepository.hasProjectAccess(1L, 999L)).thenReturn(false);

    assertThatThrownBy(() -> projectService.findById(999L))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void findByKey_WhenExists_ShouldReturnProject() {
    when(projectRepository.findByProjectKey("TST")).thenReturn(Optional.of(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

    ProjectDTO result = projectService.findByKey("tst");

    assertThat(result).isNotNull();
    assertThat(result.getProjectKey()).isEqualTo("TST");
  }

  /**
   * Regression: a MANAGER who created a project could not see it. The controller
   * allows MANAGERs to create, but non-admins only see projects they own, are
   * assigned to, or reach through a team, and create() granted the creator none of
   * those. The save returned 201 and the project then never showed in their list.
   */
  @Test
  void create_ByManager_GrantsCreatorManagerAccessToTheNewProject() {
    User creator = User.builder().id(2L).username("creator").role(UserRole.MANAGER).build();
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("creator", null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

    Project saved = Project.builder().id(5L).name("Marketing").projectKey("MKT").isActive(true).build();
    when(projectRepository.existsByProjectKey("MKT")).thenReturn(false);
    when(projectRepository.save(any(Project.class))).thenReturn(saved);

    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("Marketing");
    request.setProjectKey("MKT");

    projectService.create(request);

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isSameAs(creator);
    assertThat(captor.getValue().getProject()).isSameAs(saved);
    assertThat(captor.getValue().getProjectRole()).isEqualTo(ProjectRole.MANAGER);
    assertThat(captor.getValue().getGrantedBy()).isSameAs(creator);
  }

  @Test
  void create_ByAdmin_WritesNoAssignmentBecauseAdminsSeeEveryProject() {
    User admin = User.builder().id(3L).username("boss").role(UserRole.ADMIN).build();
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("boss", null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    when(userRepository.findByUsername("boss")).thenReturn(Optional.of(admin));

    Project saved = Project.builder().id(6L).name("Ops").projectKey("OPS").isActive(true).build();
    when(projectRepository.existsByProjectKey("OPS")).thenReturn(false);
    when(projectRepository.save(any(Project.class))).thenReturn(saved);

    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("Ops");
    request.setProjectKey("OPS");

    projectService.create(request);

    verify(userProjectRepository, never()).save(any(UserProject.class));
  }

  @Test
  void create_WhenCreatorIsNamedOwner_DoesNotAddADuplicateAssignment() {
    // Ownership already makes the project visible and gives the MANAGER role, so a
    // second row would be redundant.
    when(projectRepository.existsByProjectKey("TST")).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(projectRepository.save(any(Project.class))).thenReturn(testProject);

    projectService.create(testRequest);

    verify(userProjectRepository, never()).save(any(UserProject.class));
  }

  @Test
  void create_WithoutAnAuthenticatedUser_StillCreatesAndGrantsNothing() {
    // Seeders and imports can call create outside a request.
    SecurityContextHolder.clearContext();
    Project saved = Project.builder().id(7L).name("Seeded").projectKey("SEED").isActive(true).build();
    when(projectRepository.existsByProjectKey("SEED")).thenReturn(false);
    when(projectRepository.save(any(Project.class))).thenReturn(saved);

    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("Seeded");
    request.setProjectKey("SEED");

    ProjectDTO result = projectService.create(request);

    assertThat(result.getProjectKey()).isEqualTo("SEED");
    verify(userProjectRepository, never()).save(any(UserProject.class));
  }

  @Test
  void create_WithValidData_ShouldCreateProject() {
    when(projectRepository.existsByProjectKey("TST")).thenReturn(false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(projectRepository.save(any(Project.class))).thenReturn(testProject);
    when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

    ProjectDTO result = projectService.create(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Project");
    verify(projectRepository).save(any(Project.class));
  }

  @Test
  void create_WithDuplicateKey_ShouldThrowException() {
    when(projectRepository.existsByProjectKey("TST")).thenReturn(true);

    assertThatThrownBy(() -> projectService.create(testRequest)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Project key already exists");
  }

  @Test
  void update_WhenExists_ShouldUpdateProject() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(projectRepository.save(any(Project.class))).thenReturn(testProject);
    when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

    testRequest.setName("Updated Project");
    ProjectDTO result = projectService.update(1L, testRequest);

    assertThat(result).isNotNull();
    verify(projectRepository).save(any(Project.class));
  }

  @Test
  void deactivate_ShouldDeactivateProject() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    Project deactivated = Project.builder().id(1L).name("Test Project").projectKey("TST").isActive(false)
        .createdAt(LocalDateTime.now()).build();
    when(projectRepository.save(any(Project.class))).thenReturn(deactivated);
    when(cycleRepository.countByProjectId(1L)).thenReturn(0L);
    when(cycleRepository.countActiveByProjectId(1L)).thenReturn(0L);

    ProjectDTO result = projectService.deactivate(1L);

    assertThat(result.getIsActive()).isFalse();
    verify(projectRepository).save(any(Project.class));
  }

  @Test
  void delete_WhenNoCycles_ShouldDeleteProject() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(0L);

    projectService.delete(1L);

    verify(projectRepository).delete(testProject);
  }

  @Test
  void delete_WhenHasCycles_ShouldThrowException() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(cycleRepository.countByProjectId(1L)).thenReturn(3L);

    assertThatThrownBy(() -> projectService.delete(1L)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot delete project with existing cycles");
  }
}
