package com.github.farzadsedaghatbin.shipflow.dto.inbound;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInboundWebhookConfigRequest {

    @NotBlank(message = "Provider name is required")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*$",
        message = "Provider name must be lowercase alphanumeric with optional hyphens")
    private String providerName;

    private String displayName;

    private String webhookSecret;

    private String signatureHeader;

    private String hmacAlgorithm;

    private String description;

    private Boolean isEnabled;
}
