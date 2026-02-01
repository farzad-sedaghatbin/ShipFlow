package com.github.farzadsedaghatbin.shipflow.dto.teams;

import com.github.farzadsedaghatbin.shipflow.entity.enums.FlowType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsChannelConfigDTO {
    private Long id;
    private Long teamsConfigId;
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
    private FlowType flowType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
