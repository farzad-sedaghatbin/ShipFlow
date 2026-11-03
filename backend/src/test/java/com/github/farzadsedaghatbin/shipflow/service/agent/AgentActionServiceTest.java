package com.github.farzadsedaghatbin.shipflow.service.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.agent.AgentActionProposalDTO;
import com.github.farzadsedaghatbin.shipflow.entity.AgentActionProposal;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AgentActionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.AgentActionProposalRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.AgentActionParser.ProposedAction;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpProjectScopeGuard;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentActionService")
class AgentActionServiceTest {

  private static final Long REQUESTER = 1L;
  private static final Long PROJECT = 5L;

  @Mock private AgentActionProposalRepository repository;
  @Mock private AgentIdentityService identity;
  @Mock private UserRepository userRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private UserProjectRepository userProjectRepository;
  @Mock private McpToolDispatcher dispatcher;
  @Mock private McpProjectScopeGuard scopeGuard;
  @Mock private Authentication auth;

  private AgentActionProperties properties;
  private AgentActionService service;
  private User requester;
  private AgentActionProposal proposal;

  @BeforeEach
  void setUp() {
    properties = new AgentActionProperties(true, 24);
    ObjectMapper mapper = new ObjectMapper();
    AgentActionCatalog catalog = new AgentActionCatalog();
    AgentActionProposalReader reader = new AgentActionProposalReader(repository, catalog, properties, mapper);
    service =
        new AgentActionService(
            repository, reader, catalog, properties, identity, userRepository, projectRepository,
            userProjectRepository, dispatcher, scopeGuard, mapper);

    requester = User.builder().id(REQUESTER).username("sara").role(UserRole.MEMBER).isActive(true).build();
    proposal =
        AgentActionProposal.builder()
            .id(10L).commentId(20L).entityType(CommentEntityType.TASK).entityId(3L)
            .requesterUserId(REQUESTER).projectId(PROJECT).tool("update_task_status")
            .argumentsJson("{\"taskId\":3,\"status\":\"DONE\"}").summary("Mark done")
            .status(AgentActionStatus.PROPOSED).createdAt(LocalDateTime.now()).build();

    lenient().when(identity.isEnabled()).thenReturn(true);
    lenient().when(repository.findById(10L)).thenReturn(Optional.of(proposal));
    lenient().when(repository.save(any(AgentActionProposal.class))).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(userRepository.findById(REQUESTER)).thenReturn(Optional.of(requester));
    lenient().when(projectRepository.findById(PROJECT)).thenReturn(Optional.of(new Project()));
    lenient()
        .when(userProjectRepository.findProjectRoleByUserIdAndProjectId(REQUESTER, PROJECT))
        .thenReturn(Optional.of(ProjectRole.CONTRIBUTOR));
    lenient()
        .when(repository.transition(eq(10L), eq(AgentActionStatus.PROPOSED), any(), any()))
        .thenReturn(1);
  }

  private static ProposedAction action(String tool) {
    return new ProposedAction(tool, Map.of("taskId", 3, "status", "DONE"), "s");
  }

  // ── who may be offered changes ──────────────────────────────────────────────

  @Test
  @DisplayName("Contributors, managers, the owner and admins may be offered changes")
  void mayProposeForWriters() {
    assertThat(service.mayPropose(requester, PROJECT)).isTrue();

    when(userProjectRepository.findProjectRoleByUserIdAndProjectId(REQUESTER, PROJECT))
        .thenReturn(Optional.of(ProjectRole.MANAGER));
    assertThat(service.mayPropose(requester, PROJECT)).isTrue();

    requester.setRole(UserRole.ADMIN);
    assertThat(service.mayPropose(requester, PROJECT)).isTrue();
  }

  @Test
  @DisplayName("The project owner may be offered changes without a membership row")
  void owner() {
    Project p = new Project();
    p.setOwner(requester);
    when(projectRepository.findById(PROJECT)).thenReturn(Optional.of(p));

    assertThat(service.mayPropose(requester, PROJECT)).isTrue();
    verify(userProjectRepository, never()).findProjectRoleByUserIdAndProjectId(anyLong(), anyLong());
  }

  @Test
  @DisplayName("READONLY accounts, viewers, outsiders and project-less threads are never offered changes")
  void mayNotProposeForReaders() {
    requester.setRole(UserRole.READONLY);
    assertThat(service.mayPropose(requester, PROJECT)).isFalse();

    requester.setRole(UserRole.MEMBER);
    when(userProjectRepository.findProjectRoleByUserIdAndProjectId(REQUESTER, PROJECT))
        .thenReturn(Optional.of(ProjectRole.VIEWER));
    assertThat(service.mayPropose(requester, PROJECT)).isFalse();

    when(userProjectRepository.findProjectRoleByUserIdAndProjectId(REQUESTER, PROJECT)).thenReturn(Optional.empty());
    assertThat(service.mayPropose(requester, PROJECT)).isFalse();
    assertThat(service.mayPropose(requester, null)).isFalse();
    assertThat(service.mayPropose(null, PROJECT)).isFalse();
  }

  @Test
  @DisplayName("Nothing is offered when agent actions or the agent itself are switched off")
  void switchedOff() {
    AgentActionProperties off = new AgentActionProperties(false, 24);
    AgentActionService s =
        new AgentActionService(
            repository, new AgentActionProposalReader(repository, new AgentActionCatalog(), off, new ObjectMapper()),
            new AgentActionCatalog(), off, identity, userRepository, projectRepository, userProjectRepository,
            dispatcher, scopeGuard, new ObjectMapper());
    assertThat(s.mayPropose(requester, PROJECT)).isFalse();

    when(identity.isEnabled()).thenReturn(false);
    assertThat(service.mayPropose(requester, PROJECT)).isFalse();
  }

  // ── screening what the model emitted ────────────────────────────────────────

  @Test
  @DisplayName("screen keeps valid allow-listed proposals, scope-checked against the thread's project")
  void screenKeepsValid() {
    List<ProposedAction> kept = service.screen(List.of(action("update_task_status")), PROJECT);

    assertThat(kept).hasSize(1);
    ArgumentCaptor<ApiKey> key = ArgumentCaptor.forClass(ApiKey.class);
    verify(scopeGuard).authorize(eq("update_task_status"), any(), key.capture());
    assertThat(key.getValue().getRestrictedToProjectId()).isEqualTo(PROJECT);
  }

  @Test
  @DisplayName("screen drops tools outside the allow-list before they reach the scope guard")
  void screenDropsForbiddenTools() {
    List<ProposedAction> kept =
        service.screen(List.of(action("create_epic"), action("wise_architecture_analyze"), action("delete_everything")), PROJECT);

    assertThat(kept).isEmpty();
    verify(scopeGuard, never()).authorize(any(), any(), any());
  }

  @Test
  @DisplayName("screen drops a proposal that reaches outside the project")
  void screenDropsForeignProject() {
    when(scopeGuard.authorize(eq("update_task_status"), any(), any()))
        .thenThrow(new RuntimeException("Resource not found or not accessible with this API key"));

    assertThat(service.screen(List.of(action("update_task_status")), PROJECT)).isEmpty();
  }

  @Test
  @DisplayName("screen caps the count and drops oversized arguments")
  void screenCapsAndSizes() {
    List<ProposedAction> many =
        List.of(action("update_task"), action("update_task"), action("update_task"), action("update_task"), action("update_task"));
    assertThat(service.screen(many, PROJECT)).hasSize(AgentActionProperties.MAX_PER_REPLY);

    ProposedAction huge = new ProposedAction("update_task", Map.of("description", "x".repeat(5000)), "");
    assertThat(service.screen(List.of(huge), PROJECT)).isEmpty();
  }

  @Test
  @DisplayName("save stores each proposal as PROPOSED against the reply comment, as the requester")
  void saveStores() {
    service.save(20L, CommentEntityType.TASK, 3L, REQUESTER, PROJECT, List.of(action("update_task_status")));

    ArgumentCaptor<AgentActionProposal> saved = ArgumentCaptor.forClass(AgentActionProposal.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getCommentId()).isEqualTo(20L);
    assertThat(saved.getValue().getRequesterUserId()).isEqualTo(REQUESTER);
    assertThat(saved.getValue().getProjectId()).isEqualTo(PROJECT);
    assertThat(saved.getValue().getStatus()).isEqualTo(AgentActionStatus.PROPOSED);
    assertThat(saved.getValue().getArgumentsJson()).contains("\"taskId\":3");
  }

  // ── confirming ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Confirm runs the tool as the requester inside the project and records EXECUTED")
  void confirmRuns() {
    when(dispatcher.executeWriteToolAs(eq("update_task_status"), any(), eq(auth), eq(PROJECT)))
        .thenReturn(Map.of("id", 3));

    AgentActionProposalDTO dto = service.confirm(10L, auth, REQUESTER);

    assertThat(dto.getStatus()).isEqualTo(AgentActionStatus.EXECUTED);
    assertThat(dto.getResultMessage()).isEqualTo("Done (id 3)");
    ArgumentCaptor<Map<String, Object>> args = ArgumentCaptor.forClass(Map.class);
    verify(dispatcher).executeWriteToolAs(eq("update_task_status"), args.capture(), eq(auth), eq(PROJECT));
    assertThat(args.getValue()).containsEntry("status", "DONE");
    verify(repository).transition(eq(10L), eq(AgentActionStatus.PROPOSED), eq(AgentActionStatus.CONFIRMED), any());
  }

  @Test
  @DisplayName("Only the requester can confirm — anyone else is refused and nothing runs")
  void confirmByOtherUser() {
    assertThatThrownBy(() -> service.confirm(10L, auth, 99L)).isInstanceOf(AccessDeniedException.class);

    verify(repository, never()).transition(anyLong(), any(), any(), any());
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A second confirm (double click, second tab) loses the claim and runs nothing")
  void doubleConfirm() {
    when(repository.transition(eq(10L), eq(AgentActionStatus.PROPOSED), eq(AgentActionStatus.CONFIRMED), any()))
        .thenReturn(0);

    assertThatThrownBy(() -> service.confirm(10L, auth, REQUESTER)).isInstanceOf(BadRequestException.class);
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("An expired proposal cannot be confirmed")
  void expired() {
    proposal.setCreatedAt(LocalDateTime.now().minusHours(25));

    assertThatThrownBy(() -> service.confirm(10L, auth, REQUESTER))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("expired");
    verify(repository).transition(eq(10L), eq(AgentActionStatus.PROPOSED), eq(AgentActionStatus.EXPIRED), any());
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Confirm is refused while agent actions are switched off")
  void confirmWhileOff() {
    AgentActionProperties off = new AgentActionProperties(false, 24);
    AgentActionService s =
        new AgentActionService(
            repository, new AgentActionProposalReader(repository, new AgentActionCatalog(), off, new ObjectMapper()),
            new AgentActionCatalog(), off, identity, userRepository, projectRepository, userProjectRepository,
            dispatcher, scopeGuard, new ObjectMapper());

    assertThatThrownBy(() -> s.confirm(10L, auth, REQUESTER)).isInstanceOf(BadRequestException.class);
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Permission is re-checked at confirm time: a demoted requester gets FAILED, not a change")
  void permissionRevoked() {
    when(userProjectRepository.findProjectRoleByUserIdAndProjectId(REQUESTER, PROJECT))
        .thenReturn(Optional.of(ProjectRole.VIEWER));

    AgentActionProposalDTO dto = service.confirm(10L, auth, REQUESTER);

    assertThat(dto.getStatus()).isEqualTo(AgentActionStatus.FAILED);
    assertThat(dto.getResultMessage()).contains("no longer have permission");
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A deactivated requester cannot have a change applied")
  void inactiveRequester() {
    requester.setIsActive(false);

    assertThat(service.confirm(10L, auth, REQUESTER).getStatus()).isEqualTo(AgentActionStatus.FAILED);
    verify(dispatcher, never()).executeWriteToolAs(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A tool failure is recorded on the proposal and not thrown")
  void toolFailure() {
    when(dispatcher.executeWriteToolAs(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Invalid status 'DONEZO'"));

    AgentActionProposalDTO dto = service.confirm(10L, auth, REQUESTER);

    assertThat(dto.getStatus()).isEqualTo(AgentActionStatus.FAILED);
    assertThat(dto.getResultMessage()).contains("Invalid status");
  }

  // ── cancelling ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("The requester can cancel; anyone else is refused; a handled proposal cannot be cancelled")
  void cancel() {
    service.cancel(10L, REQUESTER);
    verify(repository).transition(eq(10L), eq(AgentActionStatus.PROPOSED), eq(AgentActionStatus.CANCELLED), any());

    assertThatThrownBy(() -> service.cancel(10L, 99L)).isInstanceOf(AccessDeniedException.class);

    when(repository.transition(eq(10L), eq(AgentActionStatus.PROPOSED), eq(AgentActionStatus.CANCELLED), any()))
        .thenReturn(0);
    assertThatThrownBy(() -> service.cancel(10L, REQUESTER)).isInstanceOf(BadRequestException.class);
  }
}
