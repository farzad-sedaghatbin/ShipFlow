package com.github.farzadsedaghatbin.shipflow.dto.agent;

import com.github.farzadsedaghatbin.shipflow.entity.enums.AgentActionStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/** A change the agent proposed, as shown under its comment. */
@Data
@Builder
public class AgentActionProposalDTO {
  private Long id;
  private Long commentId;
  /** The MCP tool that will run. */
  private String tool;
  /** Exactly what will run, built from the real tool and arguments (not from model text). */
  private String label;
  /** The model's own one-line description; informational only. */
  private String summary;
  /** PROPOSED turns EXPIRED here once the confirmation window has passed. */
  private AgentActionStatus status;
  private String resultMessage;
  /** True only for the person who asked, while it is still PROPOSED. */
  private Boolean canConfirm;
  private LocalDateTime createdAt;
  private LocalDateTime resolvedAt;
}
