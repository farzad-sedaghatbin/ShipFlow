package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
     * Force re-detection of stacks even if cached results exist.
     * Set to true to ignore cache and perform fresh detection.
     */
    @Builder.Default
    private Boolean forceRedetection = false;
}
