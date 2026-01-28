package com.github.farzadsedaghatbin.shipflow.entity.teams;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams_notification_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsNotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teams_config_id", nullable = false)
    private TeamsConfiguration teamsConfiguration;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Lob
    @Column(name = "message_text")
    private String messageText;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
    }
}
