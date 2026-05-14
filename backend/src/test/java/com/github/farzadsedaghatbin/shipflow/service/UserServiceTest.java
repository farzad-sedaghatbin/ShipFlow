package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.RegisterRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

  @Test
  void createUser_MemberRole_AssignsAllActiveProjectsAsContributor() {
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
  void createUser_ReadonlyRole_AssignsAllActiveProjectsAsViewer() {
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
  void createUser_ManagerRole_AssignsAllActiveProjectsAsManager() {
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
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newadmin");
    request.setPassword("password123");
    request.setRole(UserRole.ADMIN);

    userService.createUser(request);

    verify(userProjectRepository, never()).save(any());
    verify(projectRepository, never()).findByIsActiveTrue();
  }

  @Test
  void createUser_WithSpecificProjectIds_AssignsOnlyThoseProjects() {
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
  void createUser_WithEmptyProjectIds_AssignsAllActiveProjects() {
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
