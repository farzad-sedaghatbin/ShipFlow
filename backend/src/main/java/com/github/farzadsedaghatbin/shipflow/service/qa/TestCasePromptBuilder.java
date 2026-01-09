package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesResponse;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for test type-specific prompts.
 */
@Component
@Slf4j
public class TestCasePromptBuilder {
    
    /**
     * Build a prompt based on the test type.
     */
    public String buildPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request,
            List<EmbeddingMatch<TextSegment>> historicalTests,
            TestType testType) {
        
        if (testType == null) {
            return buildGenericPrompt(pitch, context, request);
        }
        
        return switch (testType) {
            case SMOKE -> buildSmokeTestPrompt(pitch, context, request, historicalTests);
            case FUNCTIONAL -> buildFunctionalTestPrompt(pitch, context, request, historicalTests);
            case REGRESSION -> buildRegressionTestPrompt(pitch, context, request, historicalTests);
            case INTEGRATION -> buildIntegrationTestPrompt(pitch, context, request);
            case E2E -> buildE2ETestPrompt(pitch, context, request);
            default -> buildGenericPrompt(pitch, context, request);
        };
    }
    
    private String buildSmokeTestPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request,
            List<EmbeddingMatch<TextSegment>> historicalTests) {
        
        String historicalExamples = formatHistoricalExamples(historicalTests);
        String cycleId = pitch.getCycle() != null ? pitch.getCycle().getId().toString() : "unknown";
        
        return String.format("""
            You are a QA expert for Shape Up methodology projects.
            
            **Context:**
            %s
            
            **Task:** Generate SMOKE test cases for "%s"
            
            **Smoke Test Requirements:**
            - Focus on critical path validation
            - Maximum 5-10 test cases
            - Each test should complete in < 2 minutes
            - Cover: deployment verification, core workflows, critical integrations
            - High-level checks only - detailed validation comes later
            
            **Historical Examples from similar features:**
            %s
            
            **Output Format (JSON-like structure):**
            For each test case provide:
            ---TEST CASE---
            TITLE: Clear, concise title (e.g., "Verify application starts successfully")
            DESCRIPTION: What critical functionality is being verified
            PRECONDITIONS: ["Clean deployment", "Database accessible"]
            STEPS:
            1. [Specific action with data if needed]
            2. [Next action]
            ...
            EXPECTED: Clear expected outcome with measurable criteria
            TYPE: SMOKE
            PRIORITY: P0 (all smoke tests should be P0 or HIGH)
            TAGS: critical, deployment, [feature-specific-tag]
            ---END---
            
            Generate 5-8 smoke tests covering the most critical paths.
            Focus on "Can the user perform the core action?" not "Does every detail work?"
            
            """, context, pitch.getTitle(), historicalExamples);
    }
    
    private String buildFunctionalTestPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request,
            List<EmbeddingMatch<TextSegment>> historicalTests) {
        
        String historicalExamples = formatHistoricalExamples(historicalTests);
        
        return String.format("""
            **Context:**
            %s
            
            **Task:** Generate FUNCTIONAL test cases for "%s"
            
            **Functional Test Requirements:**
            - Cover all acceptance criteria from the pitch
            - Include positive and negative scenarios
            - Test boundary conditions
            - Validate business rules
            - Consider edge cases mentioned in scopes/requirements
            
            **Coverage Guidelines:**
            - Happy path: 40%% (expected user journey)
            - Edge cases: 30%% (unusual but valid inputs)
            - Error handling: 20%% (invalid inputs, system errors)
            - Boundary conditions: 10%% (limits, thresholds)
            
            **Similar Feature Tests for consistency:**
            %s
            
            **Output Format:**
            ---TEST CASE---
            TITLE: Descriptive title indicating scenario
            DESCRIPTION: What functionality and scenario is being tested
            PRECONDITIONS: Setup required before test execution
            STEPS:
            1. [Action with specific data]
            2. [Action with expected state]
            ...
            EXPECTED: Detailed expected result with success criteria
            TYPE: FUNCTIONAL
            PRIORITY: HIGH|MEDIUM|LOW (based on user impact)
            TAGS: happy-path|edge-case|error-handling, [feature], [component]
            ---END---
            
            Generate comprehensive functional test cases.
            Ensure you cover at least one test for each acceptance criterion.
            Include at least one error handling test.
            
            """, context, pitch.getTitle(), historicalExamples);
    }
    
    private String buildRegressionTestPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request,
            List<EmbeddingMatch<TextSegment>> historicalTests) {
        
        String historicalExamples = formatHistoricalExamples(historicalTests);
        String pastFailures = extractPastFailures(request);
        
        return String.format("""
            **Task:** Generate REGRESSION test cases for "%s"
            
            **Context:**
            %s
            
            **Regression Focus:**
            - Re-validate previously working features that might be affected by this change
            - Check for side effects from new changes
            - Verify integrations still work correctly
            - Include test cases from previous cycles that touched this area
            - Pay special attention to areas that have failed before
            
            **Historical Test Results from related features:**
            %s
            
            **Previously Failed Areas (prioritize these):**
            %s
            
            **Output Format:**
            ---TEST CASE---
            TITLE: "Regression: [Feature] - [Scenario]"
            DESCRIPTION: What previously working functionality is being re-validated and why
            PRECONDITIONS: [Previous state that worked]
            STEPS:
            1. [Action that previously worked]
            2. [Verification step]
            ...
            EXPECTED: Same result as before the change
            TYPE: REGRESSION
            PRIORITY: Based on risk of regression
            TAGS: regression, [affected-area], [related-feature]
            ---END---
            
            Generate regression test suite focusing on risk areas.
            Include tests for all integrations points that could be affected.
            
            """, pitch.getTitle(), context, historicalExamples, pastFailures);
    }
    
    private String buildIntegrationTestPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request) {
        
        return String.format("""
            **Task:** Generate INTEGRATION test cases for "%s"
            
            **Context:**
            %s
            
            **Integration Test Focus:**
            - Test interactions between components/modules
            - Verify data flow across boundaries
            - Validate API contracts and responses
            - Check database interactions
            - Test external service integrations
            
            **Output Format:**
            ---TEST CASE---
            TITLE: "Integration: [Component A] -> [Component B]"
            DESCRIPTION: What integration point is being tested
            PRECONDITIONS: Both components available and configured
            STEPS:
            1. [Setup integration]
            2. [Trigger interaction]
            3. [Verify data flow]
            ...
            EXPECTED: Correct data passed between components
            TYPE: INTEGRATION
            PRIORITY: HIGH (integration failures are critical)
            TAGS: integration, [component-a], [component-b], [interface]
            ---END---
            
            Focus on boundaries between systems/modules.
            Include positive and negative integration scenarios.
            
            """, pitch.getTitle(), context);
    }
    
    private String buildE2ETestPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request) {
        
        return String.format("""
            **Task:** Generate END-TO-END test cases for "%s"
            
            **Context:**
            %s
            
            **E2E Test Focus:**
            - Complete user journeys from start to finish
            - Cross multiple components/pages/features
            - Represent real user workflows
            - Include setup and cleanup
            - Test from user perspective
            
            **Output Format:**
            ---TEST CASE---
            TITLE: "E2E: [User Role] - [Goal]"
            DESCRIPTION: Complete user journey and goal
            PRECONDITIONS: User logged in, initial data state
            STEPS:
            1. [Navigate to starting point]
            2. [First interaction]
            3. [Navigate to next step]
            4. [Interaction]
            ...
            EXPECTED: User successfully completes journey with expected outcome
            TYPE: E2E
            PRIORITY: HIGH (represents real user value)
            TAGS: e2e, [user-role], [workflow], [business-goal]
            ---END---
            
            Generate realistic user journeys.
            Each test should represent a complete business workflow.
            Include at least 5-10 steps per test.
            
            """, pitch.getTitle(), context);
    }
    
    private String buildGenericPrompt(
            Pitch pitch, 
            String context, 
            GenerateTestCasesRequest request) {
        
        int maxTestCases = request.getMaxTestCases() != null ? request.getMaxTestCases() : 5;
        String testTypes = request.getTestTypes() != null && !request.getTestTypes().isEmpty() ?
                String.join(", ", request.getTestTypes()) : "FUNCTIONAL, INTEGRATION, E2E";
        
        return String.format("""
            You are a QA engineer generating test cases for a software feature.
            
            Based on the following context, generate %d test cases for the pitch "%s".
            
            Context:
            %s
            
            Test Types to focus on: %s
            
            For each test case, provide:
            ---TEST CASE---
            TITLE: <title>
            DESCRIPTION: <description>
            PRECONDITIONS: <preconditions>
            STEPS:
            1. <step>
            2. <step>
            ...
            EXPECTED: <expected result>
            TYPE: <FUNCTIONAL|INTEGRATION|UNIT|E2E|REGRESSION|SMOKE>
            PRIORITY: <LOW|MEDIUM|HIGH|CRITICAL>
            TAGS: <tag1, tag2, ...>
            ---END---
            
            Generate comprehensive test cases covering:
            - Happy path scenarios
            - Edge cases
            - Error handling
            - Integration points
            """, maxTestCases, pitch.getTitle(), context, testTypes);
    }
    
    private String formatHistoricalExamples(List<EmbeddingMatch<TextSegment>> historicalTests) {
        if (historicalTests == null || historicalTests.isEmpty()) {
            return "No historical examples available.";
        }
        
        return historicalTests.stream()
                .limit(3) // Show only top 3 examples
                .map(match -> {
                    String text = match.embedded().text();
                    return "Example: " + (text.length() > 200 ? text.substring(0, 200) + "..." : text);
                })
                .collect(Collectors.joining("\n\n"));
    }
    
    private String extractPastFailures(GenerateTestCasesRequest request) {
        // This could be enhanced to actually fetch past failures from the database
        if (request.getAdditionalContext() != null && 
            request.getAdditionalContext().toLowerCase().contains("fail")) {
            return request.getAdditionalContext();
        }
        return "No specific past failures documented.";
    }
}
