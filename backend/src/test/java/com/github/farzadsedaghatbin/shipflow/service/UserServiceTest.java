package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.RegisterRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateUserProjectsRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UserProjectAssignmentDTO;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.repository.NotificationUserMappingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PasswordResetTokenRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserProjectRepository userProjectRepository;

  @Mock
  private NotificationUserMappingRepository notificationUserMappingRepository;

  @Mock
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private UserService userService;

  private Project projectA;
  private Project projectB;

  @BeforeEach
  void setUp() {
    projectA = Project.builder().id(1L).name("Project A").projectKey("PA").isActive(true).build();
    projectB = Project.builder().id(2L).name("Project B").projectKey("PB").isActive(true).build();

    lenient().when(passwordEncoder.encode(any())).thenReturn("encoded-password");
    lenient().when(userRepository.existsByUsername(any())).thenReturn(false);
    lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      if (u.getId() == null) {
        u.setId(100L);
      }
      return u;
    });
    lenient().when(userProjectRepository.save(any(UserProject.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsAdmin() {
    User admin = User.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
    lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, List.of()));
  }

  @Test
  void createUser_MemberRole_WhenCallerIsAdmin_AssignsAllActiveProjectsAsContributor() {
    authenticateAsAdmin();
    when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(projectA, projectB));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newmember");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);

    UserDTO result = userService.createUser(request);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("newmember");

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository, times(2)).save(captor.capture());

    List<UserProject> saved = captor.getAllValues();
    assertThat(saved).hasSize(2);
    assertThat(saved).allSatisfy(up -> {
      assertThat(up.getProjectRole()).isEqualTo(ProjectRole.CONTRIBUTOR);
      assertThat(up.getUser().getUsername()).isEqualTo("newmember");
    });
    assertThat(saved).extracting(up -> up.getProject().getId()).containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void createUser_ReadonlyRole_WhenCallerIsAdmin_AssignsAllActiveProjectsAsViewer() {
    authenticateAsAdmin();
    when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(projectA));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newviewer");
    request.setPassword("password123");
    request.setRole(UserRole.READONLY);

    userService.createUser(request);

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository, times(1)).save(captor.capture());

    UserProject saved = captor.getValue();
    assertThat(saved.getProjectRole()).isEqualTo(ProjectRole.VIEWER);
    assertThat(saved.getProject().getId()).isEqualTo(1L);
  }

  @Test
  void createUser_ManagerRole_WhenCallerIsAdmin_AssignsAllActiveProjectsAsManager() {
    authenticateAsAdmin();
    when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(projectA, projectB));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newmanager");
    request.setPassword("password123");
    request.setRole(UserRole.MANAGER);

    userService.createUser(request);

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository, times(2)).save(captor.capture());

    assertThat(captor.getAllValues()).allSatisfy(up -> assertThat(up.getProjectRole()).isEqualTo(ProjectRole.MANAGER));
  }

  @Test
  void createUser_AdminRole_SkipsProjectAssignment() {
    authenticateAsAdmin();

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newadmin");
    request.setPassword("password123");
    request.setRole(UserRole.ADMIN);

    userService.createUser(request);

    verify(userProjectRepository, never()).save(any());
    verify(projectRepository, never()).findByIsActiveTrue();
  }

  @Test
  void createUser_WithSpecificProjectIds_WhenCallerIsAdmin_AssignsOnlyThoseProjects() {
    authenticateAsAdmin();
    when(projectRepository.findAllById(List.of(2L))).thenReturn(List.of(projectB));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("specificmember");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);
    request.setProjectIds(List.of(2L));

    userService.createUser(request);

    verify(projectRepository, never()).findByIsActiveTrue();
    verify(projectRepository).findAllById(List.of(2L));

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository, times(1)).save(captor.capture());

    UserProject saved = captor.getValue();
    assertThat(saved.getProject().getId()).isEqualTo(2L);
    assertThat(saved.getProjectRole()).isEqualTo(ProjectRole.CONTRIBUTOR);
  }

  @Test
  void createUser_WithEmptyProjectIds_WhenCallerIsAdmin_AssignsAllActiveProjects() {
    authenticateAsAdmin();
    when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(projectA));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("emptymember");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);
    request.setProjectIds(List.of());

    userService.createUser(request);

    verify(projectRepository).findByIsActiveTrue();
    verify(userProjectRepository, times(1)).save(any());
  }

  @Test
  void createUser_WithNoActiveProjects_CreatesUserWithNoAssignments() {
    authenticateAsAdmin();
    when(projectRepository.findByIsActiveTrue()).thenReturn(List.of());

    RegisterRequest request = new RegisterRequest();
    request.setUsername("noprojects");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);

    UserDTO result = userService.createUser(request);

    assertThat(result).isNotNull();
    verify(userProjectRepository, never()).save(any());
  }

  @Test
  void createUser_UnauthenticatedCaller_SkipsProjectAssignment() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("selfregister");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);

    UserDTO result = userService.createUser(request);

    assertThat(result).isNotNull();
    verify(userProjectRepository, never()).save(any());
    verify(projectRepository, never()).findByIsActiveTrue();
  }

  @Test
  void createUser_NonAdminCaller_SkipsProjectAssignment() {
    User member = User.builder().id(2L).username("member").role(UserRole.MEMBER).build();
    when(userRepository.findByUsername("member")).thenReturn(Optional.of(member));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("member", null, List.of()));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);

    UserDTO result = userService.createUser(request);

    assertThat(result).isNotNull();
    verify(userProjectRepository, never()).save(any());
    verify(projectRepository, never()).findByIsActiveTrue();
  }

  @Test
  void createUser_DuplicateUsername_ThrowsException() {
    when(userRepository.existsByUsername("existing")).thenReturn(true);
    when(messageService.getMessage("error.user.username.exists", "existing")).thenReturn("Username already exists");

    RegisterRequest request = new RegisterRequest();
    request.setUsername("existing");
    request.setPassword("password123");
    request.setRole(UserRole.MEMBER);

    assertThatThrownBy(() -> userService.createUser(request)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Username already exists");

    verify(userRepository, never()).save(any());
    verify(userProjectRepository, never()).save(any());
  }

  // --- getUserProjectAssignments tests ---

  @Test
  void getUserProjectAssignments_ReturnsAssignmentsForUser() {
    when(userRepository.existsById(100L)).thenReturn(true);
    UserProject up = UserProject.builder().user(User.builder().id(100L).build()).project(projectA)
        .projectRole(ProjectRole.CONTRIBUTOR).build();
    when(userProjectRepository.findByUserId(100L)).thenReturn(List.of(up));

    List<UserProjectAssignmentDTO> result = userService.getUserProjectAssignments(100L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    assertThat(result.get(0).getProjectName()).isEqualTo("Project A");
    assertThat(result.get(0).getProjectRole()).isEqualTo(ProjectRole.CONTRIBUTOR);
  }

  @Test
  void getUserProjectAssignments_UserNotFound_Throws() {
    when(userRepository.existsById(999L)).thenReturn(false);

    assertThatThrownBy(() -> userService.getUserProjectAssignments(999L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // --- updateUserProjectAssignments tests ---

  @Test
  void updateUserProjectAssignments_AddsNewAssignment() {
    authenticateAsAdmin();
    User targetUser = User.builder().id(50L).username("target").role(UserRole.MEMBER).build();
    when(userRepository.findById(50L)).thenReturn(Optional.of(targetUser));
    when(userProjectRepository.findByUserId(50L)).thenReturn(List.of());
    when(projectRepository.findById(1L)).thenReturn(Optional.of(projectA));

    UpdateUserProjectsRequest request = new UpdateUserProjectsRequest();
    request.setAssignments(
        List.of(new UpdateUserProjectsRequest.ProjectAssignment(1L, ProjectRole.CONTRIBUTOR)));

    userService.updateUserProjectAssignments(50L, request);

    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository).save(captor.capture());
    assertThat(captor.getValue().getProject().getId()).isEqualTo(1L);
    assertThat(captor.getValue().getProjectRole()).isEqualTo(ProjectRole.CONTRIBUTOR);
    assertThat(captor.getValue().getGrantedBy().getUsername()).isEqualTo("admin");
  }

  @Test
  void updateUserProjectAssignments_RemovesMissingAssignment() {
    authenticateAsAdmin();
    User targetUser = User.builder().id(50L).username("target").role(UserRole.MEMBER).build();
    when(userRepository.findById(50L)).thenReturn(Optional.of(targetUser));

    UserProject existingUp = UserProject.builder().id(10L).user(targetUser).project(projectA)
        .projectRole(ProjectRole.VIEWER).build();
    when(userProjectRepository.findByUserId(50L)).thenReturn(List.of(existingUp));

    UpdateUserProjectsRequest request = new UpdateUserProjectsRequest();
    request.setAssignments(List.of());

    userService.updateUserProjectAssignments(50L, request);

    verify(userProjectRepository).deleteByUserIdAndProjectId(50L, 1L);
    verify(userProjectRepository, never()).save(any());
  }

  @Test
  void updateUserProjectAssignments_UpdatesChangedRole() {
    authenticateAsAdmin();
    User targetUser = User.builder().id(50L).username("target").role(UserRole.MEMBER).build();
    when(userRepository.findById(50L)).thenReturn(Optional.of(targetUser));

    UserProject existingUp = UserProject.builder().id(10L).user(targetUser).project(projectA)
        .projectRole(ProjectRole.VIEWER).build();
    when(userProjectRepository.findByUserId(50L)).thenReturn(List.of(existingUp));

    UpdateUserProjectsRequest request = new UpdateUserProjectsRequest();
    request.setAssignments(
        List.of(new UpdateUserProjectsRequest.ProjectAssignment(1L, ProjectRole.MANAGER)));

    userService.updateUserProjectAssignments(50L, request);

    verify(userProjectRepository, never()).deleteByUserIdAndProjectId(any(), any());
    ArgumentCaptor<UserProject> captor = ArgumentCaptor.forClass(UserProject.class);
    verify(userProjectRepository).save(captor.capture());
    assertThat(captor.getValue().getProjectRole()).isEqualTo(ProjectRole.MANAGER);
  }

  @Test
  void updateUserProjectAssignments_SkipsSaveWhenRoleUnchanged() {
    authenticateAsAdmin();
    User targetUser = User.builder().id(50L).username("target").role(UserRole.MEMBER).build();
    when(userRepository.findById(50L)).thenReturn(Optional.of(targetUser));

    UserProject existingUp = UserProject.builder().id(10L).user(targetUser).project(projectA)
        .projectRole(ProjectRole.CONTRIBUTOR).build();
    when(userProjectRepository.findByUserId(50L)).thenReturn(List.of(existingUp));

    UpdateUserProjectsRequest request = new UpdateUserProjectsRequest();
    request.setAssignments(
        List.of(new UpdateUserProjectsRequest.ProjectAssignment(1L, ProjectRole.CONTRIBUTOR)));

    userService.updateUserProjectAssignments(50L, request);

    verify(userProjectRepository, never()).save(any());
    verify(userProjectRepository, never()).deleteByUserIdAndProjectId(any(), any());
  }

  @Test
  void updateUserProjectAssignments_UserNotFound_Throws() {
    authenticateAsAdmin();
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    UpdateUserProjectsRequest request = new UpdateUserProjectsRequest();
    request.setAssignments(List.of());

    assertThatThrownBy(() -> userService.updateUserProjectAssignments(999L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // --- mapUserRoleToProjectRole tests ---

  @Test
  void mapUserRoleToProjectRole_MemberReturnsContributor() {
    assertThat(userService.mapUserRoleToProjectRole(UserRole.MEMBER)).isEqualTo(ProjectRole.CONTRIBUTOR);
  }

  @Test
  void mapUserRoleToProjectRole_ReadonlyReturnsViewer() {
    assertThat(userService.mapUserRoleToProjectRole(UserRole.READONLY)).isEqualTo(ProjectRole.VIEWER);
  }

  @Test
  void mapUserRoleToProjectRole_ManagerReturnsManager() {
    assertThat(userService.mapUserRoleToProjectRole(UserRole.MANAGER)).isEqualTo(ProjectRole.MANAGER);
  }

  @Test
  void mapUserRoleToProjectRole_AdminReturnsNull() {
    assertThat(userService.mapUserRoleToProjectRole(UserRole.ADMIN)).isNull();
  }
}
