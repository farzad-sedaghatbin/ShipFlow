package com.github.farzadsedaghatbin.shipflow.dto.teams;

import com.github.farzadsedaghatbin.shipflow.entity.enums.FlowType;
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
    private FlowType flowType;
    private Boolean notifyTaskAssigned;
    private Boolean notifyTaskCompleted;
    private Boolean notifyTaskBlocked;
    private Boolean notifyPitchShaped;
    private Boolean notifyCycleStarted;
    private Boolean notifyCycleCooldown;
    private Boolean notifyBettingCompleted;
    private Boolean notifySprintStarted;
}
