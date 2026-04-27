package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing detected technology stacks from repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectStacksResponseDTO {

    /**
     * The pitch ID that was analyzed.
     */
    private Long pitchId;

    /**
     * Pitch name for display.
     */
    private String pitchName;

    /**
     * List of detected technology stacks across all repositories.
     */
    private List<DetectedStackDTO> detectedStacks;

    /**
     * Total number of repositories scanned.
     */
    private Integer repositoriesScanned;

    /**
     * Message for the user (e.g., "Found 3 tech stacks across 2 repositories").
     */
    private String message;
}
