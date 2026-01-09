package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseValidationResult;
import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesResponse.TestCaseSuggestion;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validator for test case quality and completeness.
 */
@Component
@Slf4j
public class TestCaseValidator {
    
    private static final List<String> VAGUE_WORDS = List.of("check", "verify", "ensure", "make sure", "validate");
    private static final Pattern SPECIFIC_ACTION_PATTERN = Pattern.compile("(click|type|select|navigate|enter|submit|upload|download|delete|create|update)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Validate a single test case suggestion.
     */
    public TestCaseValidationResult validate(TestCaseSuggestion testCase, TestType testType) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        // 1. Check required fields
        boolean hasRequiredFields = validateRequiredFields(testCase, issues);
        
        // 2. Validate step quality
        boolean hasActionableSteps = validateStepQuality(testCase, issues, suggestions);
        
        // 3. Check for duplicates or vague descriptions
        validateDescriptionQuality(testCase, issues, suggestions);
        
        // 4. Validate test type requirements
        validateTestTypeRequirements(testCase, testType, issues, suggestions);
        
        // 5. Calculate completeness score
        int completenessScore = calculateCompletenessScore(testCase);
        
        return TestCaseValidationResult.builder()
                .isValid(issues.isEmpty())
                .issues(issues)
                .completenessScore(completenessScore)
                .hasActionableSteps(hasActionableSteps)
                .hasRequiredFields(hasRequiredFields)
                .suggestions(suggestions)
                .build();
    }
    
    /**
     * Validate multiple test cases as a suite.
     */
    public TestCaseValidationResult validateSuite(List<TestCaseSuggestion> testCases, TestType testType) {
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        // Check for duplicate scenarios
        Set<String> scenarios = new HashSet<>();
        for (TestCaseSuggestion tc : testCases) {
            String scenario = normalizeScenario(tc.getTitle());
            if (!scenarios.add(scenario)) {
                issues.add("Duplicate scenario detected: " + tc.getTitle());
            }
        }
        
        // Check suite size based on test type
        boolean meetsTypeRequirements = validateSuiteSize(testCases, testType, issues);
        
        // Check coverage diversity
        validateCoverageDiversity(testCases, suggestions);
        
        int avgCompleteness = (int) testCases.stream()
                .mapToInt(tc -> calculateCompletenessScore(tc))
                .average()
                .orElse(0);
        
        return TestCaseValidationResult.builder()
                .isValid(issues.isEmpty())
                .issues(issues)
                .completenessScore(avgCompleteness)
                .hasActionableSteps(meetsTypeRequirements)
                .hasRequiredFields(true)
                .suggestions(suggestions)
                .build();
    }
    
    private boolean validateRequiredFields(TestCaseSuggestion testCase, List<String> issues) {
        boolean valid = true;
        
        if (testCase.getTitle() == null || testCase.getTitle().trim().isEmpty()) {
            issues.add("Test case missing title");
            valid = false;
        }
        
        if (testCase.getSteps() == null || testCase.getSteps().trim().isEmpty()) {
            issues.add("Test case missing steps");
            valid = false;
        }
        
        if (testCase.getExpectedResult() == null || testCase.getExpectedResult().trim().isEmpty()) {
            issues.add("Test case missing expected result");
            valid = false;
        }
        
        return valid;
    }
    
    private boolean validateStepQuality(TestCaseSuggestion testCase, List<String> issues, List<String> suggestions) {
        if (testCase.getSteps() == null) return false;
        
        String steps = testCase.getSteps().toLowerCase();
        
        // Check for vague language
        for (String vague : VAGUE_WORDS) {
            if (steps.startsWith(vague) || steps.contains("\n" + vague)) {
                // Check if it has specific details
                if (!SPECIFIC_ACTION_PATTERN.matcher(steps).find()) {
                    issues.add("Steps contain vague language without specific actions: '" + vague + "'");
                    suggestions.add("Make steps more specific with clear actions (click, type, navigate, etc.)");
                    return false;
                }
            }
        }
        
        // Check for numbered steps
        if (!steps.matches(".*[\\d]+\\..*")) {
            suggestions.add("Consider numbering test steps for clarity");
        }
        
        // Check step count
        long stepCount = steps.lines().filter(line -> !line.trim().isEmpty()).count();
        if (stepCount < 2) {
            issues.add("Test case has very few steps (" + stepCount + "), consider breaking down the actions");
            return false;
        }
        
        if (stepCount > 20) {
            suggestions.add("Test case has many steps (" + stepCount + "), consider splitting into multiple tests");
        }
        
        return true;
    }
    
    private void validateDescriptionQuality(TestCaseSuggestion testCase, List<String> issues, List<String> suggestions) {
        if (testCase.getDescription() == null || testCase.getDescription().length() < 10) {
            suggestions.add("Add a more detailed description explaining what is being tested");
        }
        
        // Check if title and description are too similar
        if (testCase.getTitle() != null && testCase.getDescription() != null) {
            String normalizedTitle = testCase.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "");
            String normalizedDesc = testCase.getDescription().toLowerCase().replaceAll("[^a-z0-9]", "");
            
            if (normalizedTitle.equals(normalizedDesc)) {
                suggestions.add("Description should provide additional context beyond the title");
            }
        }
    }
    
    private void validateTestTypeRequirements(TestCaseSuggestion testCase, TestType testType, 
                                               List<String> issues, List<String> suggestions) {
        if (testType == null) return;
        
        switch (testType) {
            case SMOKE:
                // Smoke tests should be quick
                if (testCase.getSteps() != null) {
                    long stepCount = testCase.getSteps().lines().count();
                    if (stepCount > 10) {
                        issues.add("SMOKE test should be quick - this test has too many steps (" + stepCount + ")");
                    }
                }
                break;
                
            case FUNCTIONAL:
                // Functional tests should have preconditions
                if (testCase.getPreconditions() == null || testCase.getPreconditions().trim().isEmpty()) {
                    suggestions.add("FUNCTIONAL tests benefit from explicit preconditions");
                }
                break;
                
            case REGRESSION:
                // Regression tests should reference previous functionality
                if (testCase.getDescription() != null && 
                    !testCase.getDescription().toLowerCase().contains("previous") &&
                    !testCase.getDescription().toLowerCase().contains("existing")) {
                    suggestions.add("REGRESSION tests should reference previously working functionality");
                }
                break;
                
            case E2E:
                // E2E tests should have multiple steps representing user journey
                if (testCase.getSteps() != null) {
                    long stepCount = testCase.getSteps().lines().count();
                    if (stepCount < 3) {
                        issues.add("E2E test should represent a complete user journey - add more steps");
                    }
                }
                break;
                
            default:
                // Other test types don't have specific requirements yet
                break;
        }
    }
    
    private boolean validateSuiteSize(List<TestCaseSuggestion> testCases, TestType testType, List<String> issues) {
        if (testType == null) return true;
        
        int size = testCases.size();
        
        switch (testType) {
            case SMOKE:
                if (size < 5 || size > 10) {
                    issues.add("SMOKE test suite should have 5-10 tests, got " + size);
                    return false;
                }
                break;
                
            case FUNCTIONAL:
                if (size < 3) {
                    issues.add("FUNCTIONAL test suite should have at least 3 tests (happy path + edge cases + errors)");
                    return false;
                }
                break;
                
            default:
                // Other test types don't have specific suite size requirements
                break;
        }
        
        return true;
    }
    
    private void validateCoverageDiversity(List<TestCaseSuggestion> testCases, List<String> suggestions) {
        // Check if all tests are the same type
        Set<String> types = new HashSet<>();
        for (TestCaseSuggestion tc : testCases) {
            if (tc.getSuggestedType() != null) {
                types.add(tc.getSuggestedType());
            }
        }
        
        if (types.size() == 1) {
            suggestions.add("Consider adding different types of test cases for better coverage");
        }
        
        // Check if all tests are the same priority
        Set<String> priorities = new HashSet<>();
        for (TestCaseSuggestion tc : testCases) {
            if (tc.getSuggestedPriority() != null) {
                priorities.add(tc.getSuggestedPriority());
            }
        }
        
        if (priorities.size() == 1 && testCases.size() > 3) {
            suggestions.add("Consider varying test priorities based on risk and importance");
        }
    }
    
    private int calculateCompletenessScore(TestCaseSuggestion testCase) {
        int score = 0;
        
        // Title (20 points)
        if (testCase.getTitle() != null && !testCase.getTitle().trim().isEmpty()) {
            score += 20;
        }
        
        // Description (15 points)
        if (testCase.getDescription() != null && testCase.getDescription().length() >= 10) {
            score += 15;
        }
        
        // Preconditions (10 points)
        if (testCase.getPreconditions() != null && !testCase.getPreconditions().trim().isEmpty()) {
            score += 10;
        }
        
        // Steps (25 points)
        if (testCase.getSteps() != null && testCase.getSteps().lines().count() >= 2) {
            score += 25;
        }
        
        // Expected result (20 points)
        if (testCase.getExpectedResult() != null && !testCase.getExpectedResult().trim().isEmpty()) {
            score += 20;
        }
        
        // Type and Priority (5 points each)
        if (testCase.getSuggestedType() != null) score += 5;
        if (testCase.getSuggestedPriority() != null) score += 5;
        
        return score;
    }
    
    private String normalizeScenario(String scenario) {
        if (scenario == null) return "";
        return scenario.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
