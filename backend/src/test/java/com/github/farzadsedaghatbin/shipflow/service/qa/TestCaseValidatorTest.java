package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesResponse.TestCaseSuggestion;
import com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseValidationResult;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TestCaseValidator.
 */
class TestCaseValidatorTest {

    private TestCaseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TestCaseValidator();
    }

    @Test
    void testValidateCompleteTestCase() {
        TestCaseSuggestion testCase = TestCaseSuggestion.builder()
                .title("User can successfully login")
                .description("Verify that a valid user can login with correct credentials")
                .preconditions("User account exists, User is on login page")
                .steps("1. Enter username\n2. Enter password\n3. Click login button")
                .expectedResult("User is redirected to dashboard")
                .suggestedType("FUNCTIONAL")
                .suggestedPriority("HIGH")
                .suggestedTags(Arrays.asList("login", "authentication"))
                .build();

        TestCaseValidationResult result = validator.validate(testCase, TestType.FUNCTIONAL);

        assertThat(result.getIsValid()).isTrue();
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.getCompletenessScore()).isGreaterThan(80);
        assertThat(result.getHasActionableSteps()).isTrue();
        assertThat(result.getHasRequiredFields()).isTrue();
    }

    @Test
    void testValidateIncompleteTestCase() {
        TestCaseSuggestion testCase = TestCaseSuggestion.builder()
                .title("Login test")
                .build();

        TestCaseValidationResult result = validator.validate(testCase, TestType.FUNCTIONAL);

        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getIssues()).isNotEmpty();
        assertThat(result.getIssues()).contains("Test case missing steps");
        assertThat(result.getIssues()).contains("Test case missing expected result");
        assertThat(result.getCompletenessScore()).isLessThan(50);
    }

    @Test
    void testValidateVagueSteps() {
        TestCaseSuggestion testCase = TestCaseSuggestion.builder()
                .title("Check login functionality")
                .description("Verify login works")
                .steps("Check that login works properly")
                .expectedResult("Login should work")
                .build();

        TestCaseValidationResult result = validator.validate(testCase, TestType.FUNCTIONAL);

        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getHasActionableSteps()).isFalse();
        assertThat(result.getSuggestions()).isNotEmpty();
    }

    @Test
    void testValidateSmokeTestSuite() {
        List<TestCaseSuggestion> suite = Arrays.asList(
                createTestCase("Smoke 1", 3),
                createTestCase("Smoke 2", 4),
                createTestCase("Smoke 3", 5),
                createTestCase("Smoke 4", 3),
                createTestCase("Smoke 5", 4)
        );

        TestCaseValidationResult result = validator.validateSuite(suite, TestType.SMOKE);

        assertThat(result.getIsValid()).isTrue();
        assertThat(result.getIssues()).isEmpty();
    }

    @Test
    void testValidateSmokeTestTooFewTests() {
        List<TestCaseSuggestion> suite = Arrays.asList(
                createTestCase("Smoke 1", 3),
                createTestCase("Smoke 2", 4)
        );

        TestCaseValidationResult result = validator.validateSuite(suite, TestType.SMOKE);

        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getIssues()).anyMatch(issue -> issue.contains("should have 5-10 tests"));
    }

    @Test
    void testValidateDuplicateScenarios() {
        List<TestCaseSuggestion> suite = Arrays.asList(
                createTestCase("User Login Test", 3),
                createTestCase("User login test", 3), // Duplicate (case-insensitive)
                createTestCase("Dashboard Load", 3)
        );

        TestCaseValidationResult result = validator.validateSuite(suite, TestType.FUNCTIONAL);

        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getIssues()).anyMatch(issue -> issue.contains("Duplicate scenario"));
    }

    @Test
    void testValidateE2ETestTooFewSteps() {
        TestCaseSuggestion testCase = TestCaseSuggestion.builder()
                .title("E2E: User completes purchase")
                .description("Complete purchase workflow")
                .steps("1. Login\n2. Buy item")
                .expectedResult("Order confirmed")
                .suggestedType("E2E")
                .build();

        TestCaseValidationResult result = validator.validate(testCase, TestType.E2E);

        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getIssues()).anyMatch(issue -> issue.contains("complete user journey"));
    }

    private TestCaseSuggestion createTestCase(String title, int stepCount) {
        StringBuilder steps = new StringBuilder();
        for (int i = 1; i <= stepCount; i++) {
            steps.append(i).append(". Navigate to page ").append(i).append("\n");
        }

        return TestCaseSuggestion.builder()
                .title(title)
                .description("Test description")
                .steps(steps.toString())
                .expectedResult("Expected result")
                .suggestedType("SMOKE")
                .suggestedPriority("HIGH")
                .build();
    }
}
