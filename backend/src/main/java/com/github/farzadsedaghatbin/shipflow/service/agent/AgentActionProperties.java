package com.github.farzadsedaghatbin.shipflow.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Settings for the agent's propose→confirm actions (S70). */
@Component
public class AgentActionProperties {

  /** Most changes the agent may propose in a single reply. */
  public static final int MAX_PER_REPLY = 3;

  /** Largest accepted size of one proposal's arguments JSON, in characters. */
  public static final int MAX_ARGUMENTS_CHARS = 4000;

  private final boolean enabled;
  private final long ttlHours;

  public AgentActionProperties(
      @Value("${app.agent.actions.enabled:true}") boolean enabled,
      @Value("${app.agent.actions.ttl-hours:24}") long ttlHours) {
    this.enabled = enabled;
    this.ttlHours = Math.max(1, ttlHours);
  }

  /** False turns off proposing AND confirming; the agent keeps answering questions. */
  public boolean isEnabled() {
    return enabled;
  }

  /** How long a proposal stays confirmable. */
  public long getTtlHours() {
    return ttlHours;
  }
}
