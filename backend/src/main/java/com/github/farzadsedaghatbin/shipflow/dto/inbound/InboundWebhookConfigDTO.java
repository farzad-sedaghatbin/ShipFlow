package com.github.farzadsedaghatbin.shipflow.dto.inbound;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundWebhookConfigDTO {

    private Long id;
    private String providerName;
    private String displayName;
    private String webhookSecret;
    private String signatureHeader;
    private String hmacAlgorithm;
    private String description;
    private Boolean isEnabled;
    private String webhookUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
