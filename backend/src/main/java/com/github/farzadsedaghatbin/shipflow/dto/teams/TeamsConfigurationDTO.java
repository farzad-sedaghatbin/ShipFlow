package com.github.farzadsedaghatbin.shipflow.dto.teams;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsConfigurationDTO {
  private Long id;
  private String tenantName;
  private String webhookUrl;
  private String defaultChannel;
  private Boolean isEnabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
