package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserProject;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProjectRepository userProjectRepository;

  @Mock
  private ProjectRepository projectRepository;

  @InjectMocks
  private ProjectPermissionService service;

  private User adminUser;
  private User memberUser;
  private User viewerUser;
  private Project project;

  @BeforeEach
  void setUp() {
    adminUser = User.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
    memberUser = User.builder().id(2L).username("member").role(UserRole.MEMBER).build();
    viewerUser = User.builder().id(3L).username("viewer").role(UserRole.MEMBER).build();
    project = Project.builder().id(10L).name("Test Project").projectKey("TP").build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuth(String username) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, "pw", List.of()));
  }

  @Nested
  class AdminAlwaysPasses {
    @Test
    void admin_hasProjectRole_returnsTrue() {
      setAuth("admin");
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      assertThat(service.hasProjectRole(10L, "VIEWER", "CONTRIBUTOR", "MANAGER")).isTrue();
    }

    @Test
    void admin_getMyRole_returnsManager() {
      setAuth("admin");
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      assertThat(service.getMyRole(10L)).contains(ProjectRole.MANAGER);
    }
  }

  @Nested
  class OwnerIsManager {
    @Test
    void owner_hasProjectRole_MANAGER_returnsTrue() {
      setAuth("member");
      project.setOwner(memberUser);
      when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

      assertThat(service.hasProjectRole(10L, "MANAGER")).isTrue();
    }

    @Test
    void owner_hasProjectRole_VIEWER_returnsFalse() {
      setAuth("member");
      project.setOwner(memberUser);
      when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

      // MANAGER text not in roles list → false
      assertThat(service.hasProjectRole(10L, "VIEWER")).isFalse();
    }
  }

  @Nested
  class MemberRoleChecks {
    @Test
    void contributor_hasRole_CONTRIBUTOR_returnsTrue() {
      setAuth("member");
      when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(2L, 10L))
          .thenReturn(Optional.of(ProjectRole.CONTRIBUTOR));

      assertThat(service.hasProjectRole(10L, "CONTRIBUTOR", "MANAGER")).isTrue();
    }

    @Test
    void viewer_hasRole_CONTRIBUTOR_returnsFalse() {
      setAuth("viewer");
      when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(3L, 10L))
          .thenReturn(Optional.of(ProjectRole.VIEWER));

      assertThat(service.hasProjectRole(10L, "CONTRIBUTOR", "MANAGER")).isFalse();
    }

    @Test
    void noMembership_returnsFalse() {
      setAuth("viewer");
      when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(3L, 10L))
          .thenReturn(Optional.empty());

      assertThat(service.hasProjectRole(10L, "VIEWER")).isFalse();
    }
  }

  @Nested
  class AccessGuards {
    @Test
    void requireProjectAccess_throws_whenNoMembership() {
      setAuth("viewer");
      when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(3L, 10L))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.requireProjectAccess(10L))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireContributor_throws_forViewer() {
      setAuth("viewer");
      when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewerUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(3L, 10L))
          .thenReturn(Optional.of(ProjectRole.VIEWER));

      assertThatThrownBy(() -> service.requireContributor(10L))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireContributor_passes_forContributor() {
      setAuth("member");
      when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));
      when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
      when(userProjectRepository.findProjectRoleByUserIdAndProjectId(2L, 10L))
          .thenReturn(Optional.of(ProjectRole.CONTRIBUTOR));

      assertThatCode(() -> service.requireContributor(10L)).doesNotThrowAnyException();
    }
  }
}
