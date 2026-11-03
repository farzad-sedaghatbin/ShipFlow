package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.agent.AgentActionProposalDTO;
import com.github.farzadsedaghatbin.shipflow.entity.AgentActionProposal;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AgentActionStatus;
import com.github.farzadsedaghatbin.shipflow.repository.AgentActionProposalRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the agent's proposals — what {@code CommentService} attaches to the agent's
 * comments. Kept apart from {@link AgentActionService} (which runs MCP tools) because that service
 * reaches {@code CommentService} through the tool layer; this one only reads, so there is no cycle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentActionProposalReader {

  private final AgentActionProposalRepository repository;
  private final AgentActionCatalog catalog;
  private final AgentActionProperties properties;
  private final ObjectMapper objectMapper;

  /** Proposals for the given agent comments, grouped by comment id, as seen by {@code viewerId}. */
  @Transactional(readOnly = true)
  public Map<Long, List<AgentActionProposalDTO>> forComments(Collection<Long> commentIds, Long viewerId) {
    if (commentIds == null || commentIds.isEmpty()) {
      return Map.of();
    }
    return repository.findByCommentIdInOrderByIdAsc(commentIds).stream()
        .map(p -> toDTO(p, viewerId))
        .collect(Collectors.groupingBy(AgentActionProposalDTO::getCommentId, HashMap::new, Collectors.toList()));
  }

  public AgentActionProposalDTO toDTO(AgentActionProposal p, Long viewerId) {
    AgentActionStatus status = effectiveStatus(p);
    return AgentActionProposalDTO.builder()
        .id(p.getId())
        .commentId(p.getCommentId())
        .tool(p.getTool())
        .label(catalog.describe(p.getTool(), readArguments(p.getArgumentsJson())))
        .summary(p.getSummary())
        .status(status)
        .resultMessage(p.getResultMessage())
        .canConfirm(status == AgentActionStatus.PROPOSED && p.getRequesterUserId().equals(viewerId))
        .createdAt(p.getCreatedAt())
        .resolvedAt(p.getResolvedAt())
        .build();
  }

  /** A proposal nobody confirmed within the window reads as EXPIRED without needing a write. */
  public AgentActionStatus effectiveStatus(AgentActionProposal p) {
    if (p.getStatus() == AgentActionStatus.PROPOSED && isExpired(p)) {
      return AgentActionStatus.EXPIRED;
    }
    return p.getStatus();
  }

  public boolean isExpired(AgentActionProposal p) {
    return p.getCreatedAt() != null
        && p.getCreatedAt().plusHours(properties.getTtlHours()).isBefore(LocalDateTime.now());
  }

  public Map<String, Object> readArguments(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.warn("Unreadable arguments on an agent proposal: {}", e.getMessage());
      return Map.of();
    }
  }
}
