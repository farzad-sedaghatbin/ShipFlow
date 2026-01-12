package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing organization-wide configuration settings.
 * Only one record should exist in this table.
 */
@Entity
@Table(name = "organization_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String organizationName;

    // Cycle Configuration
    @Column(nullable = false)
    private Integer defaultCycleLengthWeeks = 6;

    @Column(nullable = false)
    private Integer defaultCooldownWeeks = 2;

    // Risk Thresholds (stored as JSON in a single column for flexibility)
    @Column(columnDefinition = "TEXT")
    private String riskThresholdsJson;

    // Categories (stored as JSON arrays)
    @Column(columnDefinition = "TEXT")
    private String taskCategoriesJson;

    @Column(columnDefinition = "TEXT")
    private String pitchCategoriesJson;

    // Colors (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String colorsJson;

    // Bug Configuration (stored as JSON arrays)
    @Column(columnDefinition = "TEXT")
    private String bugStatusesJson;

    @Column(columnDefinition = "TEXT")
    private String severityLevelsJson;

    // Other Settings
    @Column(nullable = false, name = "time_zone")
    private String timeZone = "UTC";

    @Column(nullable = false, name = "date_format")
    private String dateFormat = "MM/DD/YYYY";

    @Column(nullable = false, name = "enable_notifications")
    private Boolean enableNotifications = true;

    @Column(nullable = false, name = "enable_ai_features")
    private Boolean enableAIFeatures = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
