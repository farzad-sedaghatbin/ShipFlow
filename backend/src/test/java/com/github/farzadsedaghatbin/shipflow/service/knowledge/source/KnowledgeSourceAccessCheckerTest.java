package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.repository.TeamAssignmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class KnowledgeSourceAccessCheckerTest {

  private final UserRepository users = mock(UserRepository.class);
  private final TeamAssignmentRepository teamAssignments = mock(TeamAssignmentRepository.class);
  private final UserProjectRepository userProjects = mock(UserProjectRepository.class);
  private final KnowledgeSourceAccessChecker acl =
      new KnowledgeSourceAccessChecker(users, teamAssignments, userProjects);

  private static User user(Long id, UserRole role, Long personId) {
    Person person = personId == null ? null : Person.builder().id(personId).build();
    return User.builder().id(id).role(role).person(person).build();
  }

  // ---------- ORG scope ----------

  @Test
  void org_create_requires_admin() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.ORG, null, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only ADMIN");
  }

  @Test
  void org_create_denied_for_manager() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.ORG, null, null, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void org_create_succeeds_for_admin() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.ADMIN, 10L)));

    assertThatCode(() -> acl.assertCanCreate(KnowledgeSourceScope.ORG, null, null, 1L))
        .doesNotThrowAnyException();
  }

  @Test
  void create_unknown_user_denied() {
    when(users.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.ORG, null, null, 99L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Unknown user");
  }

  // ---------- TEAM scope ----------

  @Test
  void team_create_requires_teamId() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, null, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("teamId required");
  }

  @Test
  void team_create_succeeds_for_manager_member_of_team() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));
    when(teamAssignments.existsActiveAssignment(10L, 42L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, 42L, null, 1L))
        .doesNotThrowAnyException();
  }

  @Test
  void team_create_succeeds_for_admin_member_of_team() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.ADMIN, 10L)));
    when(teamAssignments.existsActiveAssignment(10L, 42L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, 42L, null, 1L))
        .doesNotThrowAnyException();
  }

  @Test
  void team_create_denied_for_non_member() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));
    // existsActiveAssignment defaults to false

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, 42L, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not in team");
  }

  @Test
  void team_create_denied_for_member_role_even_if_in_team() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));
    when(teamAssignments.existsActiveAssignment(10L, 42L)).thenReturn(true);

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, 42L, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("MANAGER or ADMIN");
  }

  @Test
  void team_create_denied_when_user_has_no_person_link() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, null)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.TEAM, 42L, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not in team");
  }

  // ---------- PROJECT scope ----------

  @Test
  void project_create_requires_projectId() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.PROJECT, null, null, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("projectId required");
  }

  @Test
  void project_create_succeeds_for_member_in_project() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));
    when(userProjects.existsByUserIdAndProjectId(1L, 7L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanCreate(KnowledgeSourceScope.PROJECT, null, 7L, 1L))
        .doesNotThrowAnyException();
  }

  @Test
  void project_create_denied_for_user_not_on_project() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));
    // existsByUserIdAndProjectId defaults to false

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.PROJECT, null, 7L, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not on this project");
  }

  @Test
  void project_create_denied_for_readonly_even_if_on_project() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.READONLY, 10L)));
    when(userProjects.existsByUserIdAndProjectId(1L, 7L)).thenReturn(true);

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.PROJECT, null, 7L, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("ADMIN, MANAGER, or MEMBER");
  }

  // ---------- list / modify ----------

  @Test
  void list_org_allows_any_known_user() {
    when(users.existsById(1L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanListOrg(1L)).doesNotThrowAnyException();
  }

  @Test
  void list_org_denies_unknown_user() {
    when(users.existsById(1L)).thenReturn(false);

    assertThatThrownBy(() -> acl.assertCanListOrg(1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void list_team_requires_membership() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.READONLY, 10L)));
    when(teamAssignments.existsActiveAssignment(10L, 42L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanListTeam(42L, 1L)).doesNotThrowAnyException();
  }

  @Test
  void list_team_denied_for_non_member() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.READONLY, 10L)));

    assertThatThrownBy(() -> acl.assertCanListTeam(42L, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Not in team");
  }

  @Test
  void list_project_requires_membership() {
    when(userProjects.existsByUserIdAndProjectId(1L, 7L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanListProject(7L, 1L)).doesNotThrowAnyException();
  }

  @Test
  void list_project_denied_for_non_member() {
    when(userProjects.existsByUserIdAndProjectId(1L, 7L)).thenReturn(false);

    assertThatThrownBy(() -> acl.assertCanListProject(7L, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void modify_allowed_for_admin_regardless_of_creator() {
    KnowledgeSource src = orgSource(99L);
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.ADMIN, 10L)));

    assertThatCode(() -> acl.assertCanModify(src, 1L)).doesNotThrowAnyException();
  }

  @Test
  void modify_allowed_for_creator() {
    KnowledgeSource src = orgSource(1L);
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MEMBER, 10L)));

    assertThatCode(() -> acl.assertCanModify(src, 1L)).doesNotThrowAnyException();
  }

  @Test
  void modify_falls_back_to_create_check_for_non_creator_non_admin() {
    KnowledgeSource src = orgSource(99L);
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));

    // MANAGER cannot create ORG-scoped sources → cannot modify either
    assertThatThrownBy(() -> acl.assertCanModify(src, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void modify_team_source_allowed_for_team_manager() {
    KnowledgeSource src =
        KnowledgeSource.builder()
            .id(5L)
            .name("Team wiki")
            .providerType(KnowledgeProviderType.URL)
            .scope(KnowledgeSourceScope.TEAM)
            .teamId(42L)
            .config("{}")
            .status(KnowledgeSourceStatus.READY)
            .createdBy(99L)
            .build();
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.MANAGER, 10L)));
    when(teamAssignments.existsActiveAssignment(10L, 42L)).thenReturn(true);

    assertThatCode(() -> acl.assertCanModify(src, 1L)).doesNotThrowAnyException();
  }

  @Test
  void access_denied_message_propagated() {
    when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserRole.READONLY, 10L)));

    assertThatThrownBy(() -> acl.assertCanCreate(KnowledgeSourceScope.PROJECT, null, 7L, 1L))
        .isInstanceOf(AccessDeniedException.class)
        .extracting(Throwable::getMessage)
        .satisfies(msg -> assertThat((String) msg).isNotBlank());
  }

  private static KnowledgeSource orgSource(Long createdBy) {
    return KnowledgeSource.builder()
        .id(5L)
        .name("Doc")
        .providerType(KnowledgeProviderType.URL)
        .scope(KnowledgeSourceScope.ORG)
        .config("{}")
        .status(KnowledgeSourceStatus.READY)
        .createdBy(createdBy)
        .build();
  }
}
