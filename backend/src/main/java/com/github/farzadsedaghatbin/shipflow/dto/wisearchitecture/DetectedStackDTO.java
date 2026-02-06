package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a detected technology stack in a repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedStackDTO {

    /**
     * The type of technology stack detected.
     */
    private TechStackType stackType;

    /**
     * Confidence level of the detection (0-100).
     */
    private Integer confidence;

    /**
     * Key files that were used to identify this stack.
     */
    private List<String> keyFilesFound;

    /**
     * Primary programming language for this stack.
     */
    private String primaryLanguage;

    /**
     * Framework or library detected (e.g., "Spring Boot", "Express", "React 18").
     */
    private String framework;

    /**
     * Repository ID this stack was detected in.
     */
    private Long repositoryId;

    /**
     * Repository name for display purposes.
     */
    private String repositoryName;
}
