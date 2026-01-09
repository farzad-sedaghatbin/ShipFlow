package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

import java.util.List;

/**
 * Response DTO for AI test case generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestCasesResponse {
    
    /**
     * Generated test case suggestions.
     */
    private List<TestCaseSuggestion> suggestions;
    
    /**
     * Context used for generation.
     */
    private String contextUsed;
    
    /**
     * Whether AI generation was available.
     */
    private Boolean aiEnabled;
    
    /**
     * Processing time in milliseconds.
     */
    private Long processingTimeMs;
    
    /**
     * Error message if generation failed.
     */
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseSuggestion {
        private String title;
        private String description;
        private String preconditions;
        private String steps;
        private String expectedResult;
        private String suggestedType;
        private String suggestedPriority;
        private List<String> suggestedTags;
        private Double confidenceScore;
    }
}
