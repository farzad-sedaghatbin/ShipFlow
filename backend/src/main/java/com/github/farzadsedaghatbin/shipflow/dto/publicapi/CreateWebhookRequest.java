package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWebhookRequest {

  @NotBlank(message = "Webhook name is required")
  private String name;

  @NotBlank(message = "Target URL is required")
  private String targetUrl;

  /**
   * Comma-separated event types. Use "*" for all.
   * Examples: "task.status_changed", "pitch.status_changed", "cycle.status_changed",
   * "task.completed", "release.published"
   */
  @NotBlank(message = "Event types are required")
  private String eventTypes;
}
