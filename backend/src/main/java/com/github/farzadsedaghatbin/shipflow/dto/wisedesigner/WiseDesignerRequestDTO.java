package com.github.farzadsedaghatbin.shipflow.dto.wisedesigner;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to generate a UI prototype for a pitch.
 *
 * <p>At minimum only {@code pitchId} is required. Optionally provide:
 * <ul>
 *   <li>{@code wiseArchSessionId} — pass the session from a completed Wise Architecture run so the
 *   LLM can align the prototype with the architecture's component list and API contracts.</li>
 *   <li>{@code additionalContext} — free-text hints from the user ("focus on mobile", "dark theme",
 *   "follow our design system: blue primary, rounded cards").</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseDesignerRequestDTO {

  @NotNull(message = "pitchId is required")
  private Long pitchId;

  /**
   * Optional: Wise Architecture session ID to use as design context.
   * When provided, the architecture's component list and API contracts are injected into the
   * design prompt so the prototype reflects the planned architecture.
   */
  private String wiseArchSessionId;

  /**
   * Optional: free-text hints from the user. Examples:
   * "mobile-first layout", "focus on the onboarding flow", "use our brand colour #3B82F6".
   */
  private String additionalContext;
}
