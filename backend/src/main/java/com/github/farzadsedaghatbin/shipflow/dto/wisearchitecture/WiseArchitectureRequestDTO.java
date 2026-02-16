package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
     * Selected technology stacks to generate solutions for.
     */
    @NotEmpty(message = "At least one stack must be selected")
    private List<TechStackType> selectedStacks;
}
