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
 * Request DTO for generating a technical solution document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseArchitectureRequestDTO {

    /**
     * The pitch ID to generate a solution for.
     */
    @NotNull(message = "Pitch ID is required")
    private Long pitchId;

    /**
     * List of repository IDs to analyze.
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
     * Selected technology stacks to generate solutions for.
     */
    @NotEmpty(message = "At least one stack must be selected")
    private List<TechStackType> selectedStacks;
}
