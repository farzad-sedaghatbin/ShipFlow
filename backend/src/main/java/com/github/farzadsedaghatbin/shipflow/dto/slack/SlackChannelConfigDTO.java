package com.github.farzadsedaghatbin.shipflow.dto.slack;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackChannelConfigDTO {
  private Long id;
  private Long slackConfigId;
  private String channelName;
  private String channelWebhookUrl;
  private Boolean notifyTaskAssigned;
  private Boolean notifyTaskCompleted;
  private Boolean notifyTaskBlocked;
  private Boolean notifyPitchShaped;
  private Boolean notifyCycleStarted;
  private Boolean notifyCycleCooldown;
  private Boolean notifyBettingCompleted;
  private Boolean notifySprintStarted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
