package com.github.farzadsedaghatbin.shipflow.dto.workflow;

import lombok.Builder;
import lombok.Data;

/** What an agent loop would send right now — generated but not delivered anywhere. */
@Data
@Builder
public class AgentLoopPreviewDto {
  private String title;
  /** The agent's short summary; null when AI is off, unavailable, or its answer was rejected. */
  private String summary;
  /** The facts, built from the project's data. */
  private String facts;
  /** The exact message a channel would receive. */
  private String message;
  /** False when the run would be skipped as "nothing to report". */
  private boolean wouldSend;
}
