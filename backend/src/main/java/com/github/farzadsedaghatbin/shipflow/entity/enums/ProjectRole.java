package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Role assigned to a user within a specific project.
 * This is separate from the global UserRole and applies
 * only within the context of a single project.
 */
public enum ProjectRole {
    /**
     * Read-only access to project data.
     * Can view cycles, pitches, tasks, bugs, reports.
     */
    VIEWER,
    
    /**
     * Contributor access within the project.
     * Can create/edit pitches, tasks, bugs.
     * Can participate in betting and retrospectives.
     */
    CONTRIBUTOR,
    
    /**
     * Manager access for the project.
     * Can manage teams, cycles, and project settings.
     * Can grant/revoke project access to other users.
     */
    MANAGER
}
