package com.github.farzadsedaghatbin.shipflow.dto.teams;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsNotificationHistoryDTO {
  private Long id;
  private Long teamsConfigId;
  private String channelName;
  private String notificationType;
  private String messageText;
  private String entityType;
  private Long entityId;
  private LocalDateTime sentAt;
  private Boolean success;
  private String errorMessage;
}
