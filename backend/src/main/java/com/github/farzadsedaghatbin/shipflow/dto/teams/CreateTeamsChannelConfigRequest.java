package com.github.farzadsedaghatbin.shipflow.dto.teams;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamsChannelConfigRequest {
    @NotBlank(message = "Channel name is required")
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
}
