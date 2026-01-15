package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Type of dependency relationship between tasks.
 * Used for lightweight dependency tracking without full Kanban complexity.
 */
public enum DependencyType {
    /**
     * Source task blocks the target task from being started or completed.
     * The most common type - indicates a blocker relationship.
     */
    BLOCKS,
    
    /**
     * Source task depends on the target task being completed first.
     * Inverse of BLOCKS - useful for expressing the relationship from the other direction.
     */
    DEPENDS_ON,
    
    /**
     * Source task is related to the target task but not strictly blocking.
     * Useful for tracking related work items without enforcing strict ordering.
     */
    RELATED_TO
}
