package com.github.farzadsedaghatbin.shipflow.dto.slack;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackConfigurationDTO {
  private Long id;
  private String workspaceName;
  private String webhookUrl;
  private String defaultChannel;
  private Boolean isEnabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
