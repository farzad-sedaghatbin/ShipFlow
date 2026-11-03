package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.AgentActionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * One change the ShipFlow Agent proposed in a comment reply (S70). Nothing is applied until the
 * person who asked confirms it; confirming runs {@code tool} with {@code argumentsJson} as that
 * person, through the same MCP write-tool layer and project scoping external tools use.
 */
@Entity
@Table(
    name = "agent_action_proposals",
    indexes = {
      @Index(name = "idx_agent_action_comment", columnList = "comment_id"),
      @Index(name = "idx_agent_action_requester", columnList = "requester_user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentActionProposal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The agent's reply comment that carries this proposal. */
  @Column(name = "comment_id", nullable = false)
  private Long commentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false, length = 20)
  private CommentEntityType entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  /** The person who mentioned the agent — the only one who may confirm, and who the change runs as. */
  @Column(name = "requester_user_id", nullable = false)
  private Long requesterUserId;

  /** The project the thread belongs to; the change may not touch anything outside it. */
  @Column(name = "project_id", nullable = false)
  private Long projectId;

  /** MCP write-tool name, always one of {@code AgentActionCatalog}'s allow-list. */
  @Column(nullable = false, length = 64)
  private String tool;

  @Column(name = "arguments_json", nullable = false, columnDefinition = "TEXT")
  private String argumentsJson;

  /** The model's one-line description. Shown next to — never instead of — the real tool and arguments. */
  @Column(length = 500)
  private String summary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private AgentActionStatus status = AgentActionStatus.PROPOSED;

  /** Outcome: what was created/changed, or why it failed. */
  @Column(name = "result_message", length = 500)
  private String resultMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
