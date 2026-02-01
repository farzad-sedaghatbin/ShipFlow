package com.github.farzadsedaghatbin.shipflow.entity.teams;

import com.github.farzadsedaghatbin.shipflow.entity.enums.FlowType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams_channel_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teams_config_id", nullable = false)
    private TeamsConfiguration teamsConfiguration;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "channel_webhook_url", length = 1000)
    private String channelWebhookUrl;

    @Column(name = "notify_task_assigned", nullable = false)
    private Boolean notifyTaskAssigned;

    @Column(name = "notify_task_completed", nullable = false)
    private Boolean notifyTaskCompleted;

    @Column(name = "notify_task_blocked", nullable = false)
    private Boolean notifyTaskBlocked;

    @Column(name = "notify_pitch_shaped", nullable = false)
    private Boolean notifyPitchShaped;

    @Column(name = "notify_cycle_started", nullable = false)
    private Boolean notifyCycleStarted;

    @Column(name = "notify_cycle_cooldown", nullable = false)
    private Boolean notifyCycleCooldown;

    @Column(name = "notify_betting_completed", nullable = false)
    private Boolean notifyBettingCompleted;

    @Column(name = "notify_sprint_started", nullable = false)
    private Boolean notifySprintStarted;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false)
    private FlowType flowType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (notifyTaskAssigned == null) notifyTaskAssigned = true;
        if (notifyTaskCompleted == null) notifyTaskCompleted = true;
        if (notifyTaskBlocked == null) notifyTaskBlocked = false;
        if (notifyPitchShaped == null) notifyPitchShaped = true;
        if (notifyCycleStarted == null) notifyCycleStarted = true;
        if (notifyCycleCooldown == null) notifyCycleCooldown = true;
        if (notifyBettingCompleted == null) notifyBettingCompleted = false;
        if (notifySprintStarted == null) notifySprintStarted = false;
        if (flowType == null) flowType = FlowType.WEBHOOK;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
