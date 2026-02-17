package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for detecting technology stacks in repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectStacksRequestDTO {

    /**
     * The pitch ID to analyze solutions for.
     */
    @NotNull(message = "Pitch ID is required")
    private Long pitchId;

    /**
     * List of repository IDs to scan for technology stacks.
     */
    @NotEmpty(message = "At least one repository is required")
    private List<Long> repositoryIds;

    /**
     * Optional map of repository ID to branch name.
     * If not specified for a repo, the default branch will be used.
     */
    @Builder.Default
    private Map<Long, String> repositoryBranches = new HashMap<>();

    /**
     * Force re-detection of stacks even if cached results exist.
     * Set to true to ignore cache and perform fresh detection.
     */
    @Builder.Default
    private Boolean forceRedetection = false;
}
