package com.github.farzadsedaghatbin.shipflow.dto.slack;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSlackConfigurationRequest {
    @NotBlank(message = "Workspace name is required")
    private String workspaceName;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private String defaultChannel;

    private Boolean isEnabled;
}
