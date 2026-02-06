package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the complete technical solution document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseArchitectureResponseDTO {

    /**
     * Unique session ID for follow-up questions.
     */
    private String sessionId;

    /**
     * Pitch ID this solution is for.
     */
    private Long pitchId;

    /**
     * Pitch name for display.
     */
    private String pitchName;

    /**
     * Solutions organized by technology stack.
     */
    private Map<TechStackType, StackSolutionDTO> solutions;

    /**
     * Appetite check result.
     */
    private AppetiteCheckDTO appetiteCheck;

    /**
     * Total estimated hours across all stacks.
     */
    private Integer totalEstimatedHours;

    /**
     * Reduced scope alternative if appetite is exceeded.
     * Null if appetite is sufficient.
     */
    private ReducedScopeDTO reducedScope;

    /**
     * Timestamp when this solution was generated.
     */
    private LocalDateTime generatedAt;

    /**
     * Information about which context sources were available for this analysis.
     * Used to display warnings when context is missing.
     */
    private ContextSourcesDTO contextSources;

    /**
     * DTO for appetite verification result.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppetiteCheckDTO {
        /**
         * Whether the solution fits within the pitch appetite.
         */
        private Boolean passed;

        /**
         * Available hours based on pitch appetite (appetiteDays * 8).
         */
        private Integer availableHours;

        /**
         * Total estimated hours for the solution.
         */
        private Integer estimatedHours;

        /**
         * Breakdown by stack type.
         */
        private Map<TechStackType, Integer> hoursByStack;

        /**
         * Human-readable message about the appetite check.
         */
        private String message;
    }

    /**
     * DTO for reduced scope alternative when appetite is exceeded.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReducedScopeDTO {
        /**
         * Explanation of what was reduced.
         */
        private String explanation;

        /**
         * Reduced implementation steps that fit within appetite.
         */
        private List<StackSolutionDTO.ImplementationStepDTO> reducedSteps;

        /**
         * Features or components that were deferred.
         */
        private List<String> deferredItems;

        /**
         * Total hours for the reduced scope.
         */
        private Integer reducedTotalHours;
    }

    /**
     * DTO for tracking which context sources were available during analysis.
     * Missing context sources result in less accurate recommendations.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextSourcesDTO {
        /**
         * Whether code context from GitHub MCP was available.
         */
        private Boolean hasCodeContext;

        /**
         * Whether team skills information was available.
         */
        private Boolean hasTeamSkills;

        /**
         * Whether Figma design context was available.
         */
        private Boolean hasFigmaContext;

        /**
         * List of warning messages about missing context sources.
         */
        private List<String> warnings;

        /**
         * Create a DTO with warnings based on missing sources.
         */
        public static ContextSourcesDTO create(boolean hasCode, boolean hasTeamSkills, boolean hasFigma) {
            List<String> warnings = new java.util.ArrayList<>();
            
            if (!hasCode) {
                warnings.add("Code repository not accessible - architecture recommendations based on pitch description only");
            }
            if (!hasTeamSkills) {
                warnings.add("Team skills not available - recommendations may not match team expertise");
            }
            if (!hasFigma) {
                warnings.add("Figma design not accessible - UI/UX recommendations based on pitch description only");
            }
            
            return ContextSourcesDTO.builder()
                .hasCodeContext(hasCode)
                .hasTeamSkills(hasTeamSkills)
                .hasFigmaContext(hasFigma)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
        }
    }
}
