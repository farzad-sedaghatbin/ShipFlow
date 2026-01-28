package com.github.farzadsedaghatbin.shipflow.dto.slack;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSlackChannelConfigRequest {
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
