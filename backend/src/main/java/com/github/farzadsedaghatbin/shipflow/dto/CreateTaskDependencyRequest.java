package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a task dependency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskDependencyRequest {
    
    /**
     * ID of the target task (the task being depended upon or blocked).
     */
    @NotNull(message = "Target task ID is required")
    private Long targetTaskId;
    
    /**
     * Type of dependency relationship.
     * Defaults to BLOCKS if not specified.
     */
    @Builder.Default
    private DependencyType dependencyType = DependencyType.BLOCKS;
}
