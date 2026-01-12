package com.github.farzadsedaghatbin.shipflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricFormulaParser Tests")
class MetricFormulaParserTest {

    private MetricFormulaParser parser;

    @BeforeEach
    void setUp() {
        parser = new MetricFormulaParser();
    }

    @Nested
    @DisplayName("Formula Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate simple arithmetic formula")
        void validateSimpleArithmetic() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("2 + 2");
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate formula with parentheses")
        void validateFormulaWithParentheses() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("(5 + 3) * 2");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should validate formula with field references")
        void validateFieldReferences() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("pitch.appetiteDays + pitch.actualHours");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should validate formula with functions")
        void validateFunctions() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("SUM(pitch.actualHours) / COUNT(pitch.id)");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should reject empty formula")
        void rejectEmptyFormula() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("");
            assertFalse(result.isValid());
            assertEquals("Formula cannot be empty", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should reject null formula")
        void rejectNullFormula() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula(null);
            assertFalse(result.isValid());
            assertEquals("Formula cannot be empty", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should reject formula with unbalanced parentheses - missing closing")
        void rejectUnbalancedParenthesesMissingClosing() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("(5 + 3 * 2");
            assertFalse(result.isValid());
            assertEquals("Unbalanced parentheses in formula", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should reject formula with unbalanced parentheses - extra closing")
        void rejectUnbalancedParenthesesExtraClosing() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("5 + 3) * 2");
            assertFalse(result.isValid());
            assertEquals("Unbalanced parentheses in formula", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should reject formula with invalid field reference")
        void rejectInvalidFieldReference() {
            MetricFormulaParser.ValidationResult result = parser.validateFormula("invalid.field + 5");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Invalid field reference"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"SUM", "AVG", "COUNT", "MIN", "MAX"})
        @DisplayName("Should validate all supported functions")
        void validateAllSupportedFunctions(String function) {
            String formula = function + "(pitch.appetiteDays)";
            MetricFormulaParser.ValidationResult result = parser.validateFormula(formula);
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("Formula Evaluation Tests")
    class EvaluationTests {

        @Test
        @DisplayName("Should evaluate simple addition")
        void evaluateAddition() {
            BigDecimal result = parser.evaluateFormula("10 + 5", Map.of());
            assertEquals(new BigDecimal("15"), result);
        }

        @Test
        @DisplayName("Should evaluate simple subtraction")
        void evaluateSubtraction() {
            BigDecimal result = parser.evaluateFormula("20 - 8", Map.of());
            assertEquals(new BigDecimal("12"), result);
        }

        @Test
        @DisplayName("Should evaluate simple multiplication")
        void evaluateMultiplication() {
            BigDecimal result = parser.evaluateFormula("6 * 7", Map.of());
            assertEquals(new BigDecimal("42"), result);
        }

        @Test
        @DisplayName("Should evaluate simple division")
        void evaluateDivision() {
            BigDecimal result = parser.evaluateFormula("15 / 3", Map.of());
            assertEquals(new BigDecimal("5.0000"), result);
        }

        @Test
        @DisplayName("Should evaluate division with rounding")
        void evaluateDivisionWithRounding() {
            BigDecimal result = parser.evaluateFormula("10 / 3", Map.of());
            assertEquals(new BigDecimal("3.3333"), result);
        }

        @Test
        @DisplayName("Should return zero for division by zero")
        void returnZeroForDivisionByZero() {
            BigDecimal result = parser.evaluateFormula("10 / 0", Map.of());
            assertEquals(BigDecimal.ZERO, result);
        }

        @Test
        @DisplayName("Should respect operator precedence - multiplication before addition")
        void respectOperatorPrecedenceMultiplication() {
            BigDecimal result = parser.evaluateFormula("2 + 3 * 4", Map.of());
            assertEquals(new BigDecimal("14"), result);
        }

        @Test
        @DisplayName("Should respect operator precedence - division before subtraction")
        void respectOperatorPrecedenceDivision() {
            BigDecimal result = parser.evaluateFormula("20 - 8 / 2", Map.of());
            assertEquals(new BigDecimal("16.0000"), result);
        }

        @Test
        @DisplayName("Should evaluate parentheses correctly")
        void evaluateParentheses() {
            BigDecimal result = parser.evaluateFormula("(2 + 3) * 4", Map.of());
            assertEquals(new BigDecimal("20"), result);
        }

        @Test
        @DisplayName("Should evaluate nested parentheses")
        void evaluateNestedParentheses() {
            BigDecimal result = parser.evaluateFormula("((2 + 3) * 4 - 5) / 3", Map.of());
            assertEquals(new BigDecimal("5.0000"), result);
        }

        @Test
        @DisplayName("Should handle unary minus")
        void handleUnaryMinus() {
            BigDecimal result = parser.evaluateFormula("-5 + 10", Map.of());
            assertEquals(new BigDecimal("5"), result);
        }

        @Test
        @DisplayName("Should handle unary plus")
        void handleUnaryPlus() {
            BigDecimal result = parser.evaluateFormula("+5 + 10", Map.of());
            assertEquals(new BigDecimal("15"), result);
        }

        @Test
        @DisplayName("Should handle decimal numbers")
        void handleDecimalNumbers() {
            BigDecimal result = parser.evaluateFormula("3.5 + 2.25", Map.of());
            assertEquals(new BigDecimal("5.75"), result);
        }

        @Test
        @DisplayName("Should evaluate formula with field references")
        void evaluateWithFieldReferences() {
            Map<String, Object> context = Map.of(
                "pitch_appetiteDays", 10,
                "workLog_hours", 20
            );
            BigDecimal result = parser.evaluateFormula("pitch.appetiteDays + workLog.hours", context);
            assertEquals(new BigDecimal("30"), result);
        }

        @Test
        @DisplayName("Should use zero for missing field references")
        void useZeroForMissingFields() {
            Map<String, Object> context = Map.of("pitch_appetiteDays", 10);
            BigDecimal result = parser.evaluateFormula("pitch.appetiteDays + workLog.hours", context);
            assertEquals(new BigDecimal("10"), result);
        }

        @Test
        @DisplayName("Should evaluate formula with function calls")
        void evaluateWithFunctions() {
            Map<String, Object> context = Map.of(
                "sum_hours", 100,
                "count_items", 5
            );
            // Test with simple function names (not actual field references)
            BigDecimal result = parser.evaluateFormula("SUM(hours) / COUNT(items)", context);
            assertEquals(new BigDecimal("20.0000"), result);
        }

        @Test
        @DisplayName("Should evaluate complex formula with multiple operations")
        void evaluateComplexFormula() {
            Map<String, Object> context = Map.of(
                "avg_hours", 8,
                "count_items", 4
            );
            // 8 * 4 = 32
            String formula = "AVG(hours) * COUNT(items)";
            BigDecimal result = parser.evaluateFormula(formula, context);
            assertEquals(new BigDecimal("32"), result);
        }

        @ParameterizedTest
        @CsvSource({
            "5 + 3, 8",
            "10 - 4, 6",
            "3 * 7, 21",
            "100 / 5, 20.0000",
            "2 + 3 * 4, 14",
            "(2 + 3) * 4, 20"
        })
        @DisplayName("Should evaluate various arithmetic expressions")
        void evaluateVariousExpressions(String formula, String expected) {
            BigDecimal result = parser.evaluateFormula(formula, Map.of());
            assertEquals(new BigDecimal(expected), result);
        }
    }

    @Nested
    @DisplayName("Field Reference Extraction Tests")
    class FieldReferenceTests {

        @Test
        @DisplayName("Should extract single field reference")
        void extractSingleFieldReference() {
            List<MetricFormulaParser.FieldReference> refs = parser.extractFieldReferences("pitch.appetiteDays");
            assertEquals(1, refs.size());
            assertEquals("pitch", refs.get(0).entity());
            assertEquals("appetiteDays", refs.get(0).field());
            assertEquals("pitch.appetiteDays", refs.get(0).getFullReference());
        }

        @Test
        @DisplayName("Should extract multiple field references")
        void extractMultipleFieldReferences() {
            String formula = "pitch.appetiteDays + workLog.hours - task.estimateHours";
            List<MetricFormulaParser.FieldReference> refs = parser.extractFieldReferences(formula);
            assertEquals(3, refs.size());
            assertEquals("pitch.appetiteDays", refs.get(0).getFullReference());
            assertEquals("workLog.hours", refs.get(1).getFullReference());
            assertEquals("task.estimateHours", refs.get(2).getFullReference());
        }

        @Test
        @DisplayName("Should extract field references from complex formula")
        void extractFieldReferencesFromComplexFormula() {
            String formula = "(pitch.actualHours / pitch.appetiteDays) * 100";
            List<MetricFormulaParser.FieldReference> refs = parser.extractFieldReferences(formula);
            assertEquals(2, refs.size());
        }

        @Test
        @DisplayName("Should return empty list when no field references")
        void returnEmptyListWhenNoReferences() {
            List<MetricFormulaParser.FieldReference> refs = parser.extractFieldReferences("10 + 20");
            assertTrue(refs.isEmpty());
        }

        @Test
        @DisplayName("Should handle field references inside functions")
        void handleFieldReferencesInsideFunctions() {
            String formula = "SUM(workLog.hours) / COUNT(pitch.id)";
            List<MetricFormulaParser.FieldReference> refs = parser.extractFieldReferences(formula);
            assertEquals(2, refs.size());
            assertEquals("workLog.hours", refs.get(0).getFullReference());
            assertEquals("pitch.id", refs.get(1).getFullReference());
        }
    }

    @Nested
    @DisplayName("WHERE Clause Extraction Tests")
    class WhereClauseTests {

        @Test
        @DisplayName("Should extract WHERE clause")
        void extractWhereClause() {
            String formula = "SUM(workLog.hours) WHERE pitch.status = 'COMPLETED'";
            String whereClause = parser.extractWhereClause(formula);
            assertNotNull(whereClause);
            assertEquals("pitch.status = 'COMPLETED'", whereClause);
        }

        @Test
        @DisplayName("Should extract WHERE clause case-insensitive")
        void extractWhereClauseCaseInsensitive() {
            String formula = "SUM(workLog.hours) where pitch.status = 'ACTIVE'";
            String whereClause = parser.extractWhereClause(formula);
            assertNotNull(whereClause);
            assertEquals("pitch.status = 'ACTIVE'", whereClause);
        }

        @Test
        @DisplayName("Should return null when no WHERE clause")
        void returnNullWhenNoWhereClause() {
            String formula = "SUM(workLog.hours)";
            String whereClause = parser.extractWhereClause(formula);
            assertNull(whereClause);
        }

        @Test
        @DisplayName("Should extract complex WHERE clause")
        void extractComplexWhereClause() {
            String formula = "COUNT(pitch.id) WHERE pitch.status = 'COMPLETED' AND cycle.name = 'Cycle 1'";
            String whereClause = parser.extractWhereClause(formula);
            assertNotNull(whereClause);
            assertEquals("pitch.status = 'COMPLETED' AND cycle.name = 'Cycle 1'", whereClause);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle formula with spaces")
        void handleFormulaWithSpaces() {
            BigDecimal result = parser.evaluateFormula("  10  +   20  ", Map.of());
            assertEquals(new BigDecimal("30"), result);
        }

        @Test
        @DisplayName("Should handle very large numbers")
        void handleLargeNumbers() {
            BigDecimal result = parser.evaluateFormula("999999 + 1", Map.of());
            assertEquals(new BigDecimal("1000000"), result);
        }

        @Test
        @DisplayName("Should handle very small decimal numbers")
        void handleSmallDecimals() {
            BigDecimal result = parser.evaluateFormula("0.001 + 0.002", Map.of());
            assertEquals(new BigDecimal("0.003"), result);
        }

        @Test
        @DisplayName("Should handle formula with only numbers")
        void handleOnlyNumbers() {
            BigDecimal result = parser.evaluateFormula("42", Map.of());
            assertEquals(new BigDecimal("42"), result);
        }

        @Test
        @DisplayName("Should handle formula with only parentheses and number")
        void handleParenthesesAndNumber() {
            BigDecimal result = parser.evaluateFormula("(42)", Map.of());
            assertEquals(new BigDecimal("42"), result);
        }

        @Test
        @DisplayName("Should throw exception for invalid characters")
        void throwExceptionForInvalidCharacters() {
            assertThrows(IllegalArgumentException.class, () -> {
                parser.evaluateFormula("10 + $20", Map.of());
            });
        }

        @Test
        @DisplayName("Should handle multiple consecutive operators gracefully")
        void handleMultipleOperators() {
            BigDecimal result = parser.evaluateFormula("10 + - 5", Map.of());
            assertEquals(new BigDecimal("5"), result);
        }

        @Test
        @DisplayName("Should handle zero values in calculations")
        void handleZeroValues() {
            BigDecimal result = parser.evaluateFormula("0 + 0 * 5", Map.of());
            assertEquals(new BigDecimal("0"), result);
        }

        @Test
        @DisplayName("Should handle negative result")
        void handleNegativeResult() {
            BigDecimal result = parser.evaluateFormula("5 - 10", Map.of());
            assertEquals(new BigDecimal("-5"), result);
        }
    }

    @Nested
    @DisplayName("Valid Field Reference Tests")
    class ValidFieldReferenceTests {

        @ParameterizedTest
        @CsvSource({
            "pitch, id",
            "pitch, appetiteDays",
            "pitch, actualHours",
            "pitch, status",
            "pitch, title",
            "cycle, id",
            "cycle, name",
            "cycle, startDate",
            "cycle, endDate",
            "cycle, status",
            "task, id",
            "task, status",
            "task, estimateHours",
            "task, actualHours",
            "team, id",
            "team, name",
            "team, memberCount",
            "person, id",
            "person, name",
            "person, email"
        })
        @DisplayName("Should validate all supported field references")
        void validateSupportedFieldReferences(String entity, String field) {
            String formula = entity + "." + field;
            MetricFormulaParser.ValidationResult result = parser.validateFormula(formula);
            assertTrue(result.isValid(), "Expected " + formula + " to be valid");
        }
    }
}
