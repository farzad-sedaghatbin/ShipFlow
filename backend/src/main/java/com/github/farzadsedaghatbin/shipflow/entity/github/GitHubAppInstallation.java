package com.github.farzadsedaghatbin.shipflow.entity.github;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to store GitHub App installation data.
 * 
 * When a user authorizes the GitHub App for their organization,
 * this entity stores the installation details including access tokens
 * and the scope (all repositories or selected repositories).
 * 
 * This enables organization-wide repository access with a single authorization,
 * eliminating the need to add repositories one by one.
 */
@Entity
@Table(name = "github_app_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubAppInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * GitHub Installation ID - unique identifier for this installation
     */
    @Column(name = "installation_id", nullable = false, unique = true)
    private Long installationId;

    /**
     * Account type: "Organization" or "User"
     */
    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    /**
     * GitHub account login (organization or user name)
     */
    @Column(name = "account_login", nullable = false)
    private String accountLogin;

    /**
     * GitHub account ID
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * Repository selection: "all" or "selected"
     * "all" means access to all current and future repositories
     * "selected" means access only to explicitly selected repositories
     */
    @Column(name = "repository_selection", nullable = false, length = 20)
    private String repositorySelection;

    /**
     * Installation access token (short-lived, auto-refreshed)
     * Used for API calls to GitHub on behalf of the installation
     */
    @Column(name = "access_token", length = 500)
    private String accessToken;

    /**
     * Token expiration timestamp
     */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /**
     * Permissions granted to this installation (JSON format)
     * e.g., {"contents": "read", "pull_requests": "write"}
     */
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    /**
     * Events the installation is subscribed to (comma-separated)
     * e.g., "push,pull_request,create,delete"
     */
    @Column(name = "subscribed_events", length = 1000)
    private String subscribedEvents;

    /**
     * Target ID (organization or user ID)
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * Target type: "Organization" or "User"
     */
    @Column(name = "target_type", length = 20)
    private String targetType;

    /**
     * User who installed/authorized the app
     */
    @Column(name = "installed_by_login")
    private String installedByLogin;

    /**
     * Whether this installation is currently active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Whether webhooks are configured and working
     */
    @Column(name = "webhooks_configured", nullable = false)
    private Boolean webhooksConfigured;

    /**
     * Last sync timestamp - when repositories were last synced
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * Number of repositories accessible through this installation
     */
    @Column(name = "repository_count")
    private Integer repositoryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (webhooksConfigured == null) {
            webhooksConfigured = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the access token is expired or about to expire (within 5 minutes)
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(tokenExpiresAt);
    }

    /**
     * Check if this installation has access to all repositories
     */
    public boolean hasAllRepositoriesAccess() {
        return "all".equals(repositorySelection);
    }
}
