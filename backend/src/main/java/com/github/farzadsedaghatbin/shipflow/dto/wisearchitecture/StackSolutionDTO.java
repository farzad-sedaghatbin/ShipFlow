package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the technical solution for a specific technology stack.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StackSolutionDTO {

    /**
     * The technology stack this solution is for.
     */
    private TechStackType stackType;

    /**
     * High-level architecture overview following best practices.
     */
    private String architectureOverview;

    /**
     * List of existing services in the codebase that can be reused.
     */
    private List<ReusableServiceDTO> reusableServices;

    /**
     * Recommended libraries and tools to use.
     */
    private List<RecommendedLibraryDTO> recommendedLibraries;

    /**
     * Step-by-step implementation plan with time estimates.
     */
    private List<ImplementationStepDTO> implementationSteps;

    /**
     * Total estimated hours for this stack's implementation.
     */
    private Integer estimatedHours;

    /**
     * Risk factors specific to this stack's implementation.
     */
    private List<String> riskFactors;

    /**
     * DTO for a reusable service found in the codebase.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReusableServiceDTO {
        private String serviceName;
        private String filePath;
        private String description;
        private String howToUse;
    }

    /**
     * DTO for a recommended library or tool.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedLibraryDTO {
        private String name;
        private String version;
        private String purpose;
        private String documentationUrl;
        private Boolean alreadyInProject;
    }

    /**
     * DTO for an implementation step.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImplementationStepDTO {
        private Integer stepNumber;
        private String title;
        private String description;
        private Integer estimatedHours;
        private List<String> filesToCreate;
        private List<String> filesToModify;
    }
}
