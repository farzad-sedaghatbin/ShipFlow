package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.AgentActionProposalRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.agent.AgentActionParser.ProposedAction;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpProjectScopeGuard;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * The propose→confirm half of the ShipFlow Agent (S70).
 *
 * <p>The agent never changes anything itself. It <em>proposes</em> a change; the person who asked
 * <em>confirms</em> it; only then does it run — as that person, through the same MCP write tools
 * and project scoping external tools use ({@link McpToolDispatcher#executeWriteToolAs}).
 *
 * <p>Safety rules enforced here, each covered by a test:
 *
 * <ul>
 *   <li>only an allow-listed tool can be proposed ({@link AgentActionCatalog});
 *   <li>a proposal must already pass the project scope guard for the thread's project, and is
 *       checked again when confirmed;
 *   <li>only the requester can confirm or cancel, and only while it is still {@code PROPOSED} and
 *       not expired; a conditional status update makes a double click run it once;
 *   <li>proposing and confirming both need the person to be able to write in that project
 *       (not a READONLY account, and contributor-or-better on the project);
 *   <li>a failure is recorded on the proposal rather than thrown away.
 * </ul>
 *
 * Deliberately not {@code @Transactional}: the claim, the change itself and the final status each
 * commit on their own, so a failed change can never roll back the record that it failed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentActionService {

  private static final int MAX_RESULT_CHARS = 480;

  private final AgentActionProposalRepository repository;
  private final AgentActionProposalReader reader;
  private final AgentActionCatalog catalog;
  private final AgentActionProperties properties;
  private final AgentIdentityService identity;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final UserProjectRepository userProjectRepository;
  private final McpToolDispatcher dispatcher;
  private final McpProjectScopeGuard scopeGuard;
  private final ObjectMapper objectMapper;

  // ── Proposing ────────────────────────────────────────────────────────────────

  /** Whether the agent may offer changes to this person in this project. */
  public boolean mayPropose(User requester, Long projectId) {
    return properties.isEnabled() && identity.isEnabled() && canWrite(requester, projectId);
  }

  /**
   * Keeps only the proposals that are safe to show: allow-listed tool, sane arguments, within the
   * thread's project, and at most {@link AgentActionProperties#MAX_PER_REPLY}. Anything else is
   * dropped (and logged) rather than shown to a person as if it were valid.
   */
  public List<ProposedAction> screen(List<ProposedAction> raw, Long projectId) {
    List<ProposedAction> accepted = new ArrayList<>();
    ApiKey scope = projectScope(projectId);
    for (ProposedAction action : raw) {
      if (accepted.size() >= AgentActionProperties.MAX_PER_REPLY) {
        break;
      }
      if (!catalog.isAllowed(action.tool())) {
        log.warn("Agent proposed a tool outside the allow-list: {}", action.tool());
        continue;
      }
      String json = toJson(action.arguments());
      if (json == null || json.length() > AgentActionProperties.MAX_ARGUMENTS_CHARS) {
        log.warn("Agent proposal for {} dropped: arguments unreadable or too large", action.tool());
        continue;
      }
      try {
        scopeGuard.authorize(action.tool(), action.arguments(), scope);
      } catch (Exception e) {
        log.warn("Agent proposal for {} dropped by the project scope guard: {}", action.tool(), e.getMessage());
        continue;
      }
      accepted.add(action);
    }
    return accepted;
  }

  /** Stores screened proposals against the agent's reply comment. */
  public List<AgentActionProposal> save(
      Long commentId,
      CommentEntityType entityType,
      Long entityId,
      Long requesterUserId,
      Long projectId,
      List<ProposedAction> actions) {
    List<AgentActionProposal> saved = new ArrayList<>();
    for (ProposedAction a : actions) {
      saved.add(
          repository.save(
              AgentActionProposal.builder()
                  .commentId(commentId)
                  .entityType(entityType)
                  .entityId(entityId)
                  .requesterUserId(requesterUserId)
                  .projectId(projectId)
                  .tool(a.tool())
                  .argumentsJson(toJson(a.arguments()))
                  .summary(a.summary())
                  .status(AgentActionStatus.PROPOSED)
                  .build()));
    }
    return saved;
  }

  // ── Confirming / cancelling ──────────────────────────────────────────────────

  /**
   * Applies a proposal as the signed-in requester.
   *
   * @param auth the requester's authentication (the caller's own request context)
   * @return the proposal as it now stands — {@code EXECUTED} or {@code FAILED}
   */
  public AgentActionProposalDTO confirm(Long proposalId, Authentication auth, Long userId) {
    AgentActionProposal p = load(proposalId);
    requireRequester(p, userId);
    if (!properties.isEnabled() || !identity.isEnabled()) {
      throw new BadRequestException("Agent actions are turned off on this ShipFlow instance");
    }
    if (reader.effectiveStatus(p) == AgentActionStatus.EXPIRED) {
      repository.transition(p.getId(), AgentActionStatus.PROPOSED, AgentActionStatus.EXPIRED, LocalDateTime.now());
      throw new BadRequestException("This proposal expired; ask the agent again");
    }
    // Whoever flips PROPOSED→CONFIRMED first owns the run; a double click or a second tab gets 0.
    if (repository.transition(p.getId(), AgentActionStatus.PROPOSED, AgentActionStatus.CONFIRMED, LocalDateTime.now()) == 0) {
      throw new BadRequestException("This proposal has already been handled");
    }

    User requester = userRepository.findById(userId).orElse(null);
    if (requester == null
        || !Boolean.TRUE.equals(requester.getIsActive())
        || !canWrite(requester, p.getProjectId())) {
      return finish(p, AgentActionStatus.FAILED, "You no longer have permission to change this project", userId);
    }
    try {
      Object result =
          dispatcher.executeWriteToolAs(
              p.getTool(), reader.readArguments(p.getArgumentsJson()), auth, p.getProjectId());
      log.info("Agent proposal {} ({}) applied for user {}", p.getId(), p.getTool(), userId);
      return finish(p, AgentActionStatus.EXECUTED, describeResult(result), userId);
    } catch (Exception e) {
      log.warn("Agent proposal {} ({}) failed: {}", p.getId(), p.getTool(), e.getMessage());
      return finish(p, AgentActionStatus.FAILED, clip(String.valueOf(e.getMessage())), userId);
    }
  }

  /** Declines a proposal. */
  public AgentActionProposalDTO cancel(Long proposalId, Long userId) {
    AgentActionProposal p = load(proposalId);
    requireRequester(p, userId);
    if (repository.transition(p.getId(), AgentActionStatus.PROPOSED, AgentActionStatus.CANCELLED, LocalDateTime.now()) == 0) {
      throw new BadRequestException("This proposal has already been handled");
    }
    return reader.toDTO(load(proposalId), userId);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  /** Can this person change things in this project? Never true for a READONLY account. */
  boolean canWrite(User user, Long projectId) {
    if (user == null || projectId == null || user.getRole() == UserRole.READONLY) {
      return false;
    }
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    Project project = projectRepository.findById(projectId).orElse(null);
    if (project != null && project.getOwner() != null && project.getOwner().getId().equals(user.getId())) {
      return true;
    }
    return userProjectRepository
        .findProjectRoleByUserIdAndProjectId(user.getId(), projectId)
        .map(r -> r == ProjectRole.MANAGER || r == ProjectRole.CONTRIBUTOR)
        .orElse(false);
  }

  private AgentActionProposal load(Long id) {
    return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proposal " + id + " not found"));
  }

  private static void requireRequester(AgentActionProposal p, Long userId) {
    if (!p.getRequesterUserId().equals(userId)) {
      throw new AccessDeniedException("Only the person who asked the agent can confirm or cancel its proposal");
    }
  }

  private AgentActionProposalDTO finish(AgentActionProposal p, AgentActionStatus status, String message, Long viewerId) {
    AgentActionProposal fresh = load(p.getId());
    fresh.setStatus(status);
    fresh.setResultMessage(clip(message));
    fresh.setResolvedAt(LocalDateTime.now());
    return reader.toDTO(repository.save(fresh), viewerId);
  }

  private static ApiKey projectScope(Long projectId) {
    ApiKey key = new ApiKey();
    key.setRestrictedToProjectId(projectId);
    return key;
  }

  private String toJson(Map<String, Object> arguments) {
    try {
      return objectMapper.writeValueAsString(arguments);
    } catch (Exception e) {
      return null;
    }
  }

  /** "Done" plus the id of what was created or changed, when the tool reports one. */
  private String describeResult(Object result) {
    try {
      JsonNode node = objectMapper.valueToTree(result);
      for (String field : new String[] {"bugKey", "id", "taskId"}) {
        JsonNode v = node.get(field);
        if (v != null && !v.isNull()) {
          return "Done (" + field + " " + v.asText() + ")";
        }
      }
    } catch (Exception ignored) {
      // fall through to the plain message
    }
    return "Done";
  }

  private static String clip(String s) {
    return s.length() <= MAX_RESULT_CHARS ? s : s.substring(0, MAX_RESULT_CHARS) + "…";
  }
}
