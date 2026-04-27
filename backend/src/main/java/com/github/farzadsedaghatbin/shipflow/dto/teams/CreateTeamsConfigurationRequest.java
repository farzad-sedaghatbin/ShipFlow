package com.github.farzadsedaghatbin.shipflow.dto.teams;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamsConfigurationRequest {
  @NotBlank(message = "Tenant name is required")
  private String tenantName;

  @NotBlank(message = "Webhook URL is required")
  private String webhookUrl;

  private String defaultChannel;

  private Boolean isEnabled;
}
