package com.github.farzadsedaghatbin.shipflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for parsing and evaluating custom metric formulas. Supports: - Mathematical operations:
 * +, -, *, /, (, ) - Functions: SUM, AVG, COUNT, MIN, MAX - Field references: pitch.appetiteDays,
 * workLog.hours, etc. - Simple WHERE clauses
 */
@Service
@Slf4j
public class MetricFormulaParser {

  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile("(SUM|AVG|COUNT|MIN|MAX)\\(([^)]++)\\)");
  private static final Pattern WHERE_PATTERN =
      Pattern.compile("WHERE\\s+([^)]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern FIELD_PATTERN =
      Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]++)\\.([a-zA-Z_][a-zA-Z0-9_]++)");

  /** Validates a metric formula for syntax errors */
  public ValidationResult validateFormula(String formula) {
    if (formula == null || formula.trim().isEmpty()) {
      return ValidationResult.error("Formula cannot be empty");
    }

    try {
      // Check for balanced parentheses
      if (!hasBalancedParentheses(formula)) {
        return ValidationResult.error("Unbalanced parentheses in formula");
      }

      // Check for valid functions
      Matcher functionMatcher = FUNCTION_PATTERN.matcher(formula);
      while (functionMatcher.find()) {
        String function = functionMatcher.group(1);
        if (!isValidFunction(function)) {
          return ValidationResult.error("Invalid function: " + function);
        }
      }

      // Check for valid field references
      Matcher fieldMatcher = FIELD_PATTERN.matcher(formula);
      while (fieldMatcher.find()) {
        String entity = fieldMatcher.group(1);
        String field = fieldMatcher.group(2);
        if (!isValidFieldReference(entity, field)) {
          return ValidationResult.error("Invalid field reference: " + entity + "." + field);
        }
      }

      return ValidationResult.success();
    } catch (Exception e) {
      log.error("Formula validation failed", e);
      return ValidationResult.error("Invalid formula: " + e.getMessage());
    }
  }

  /**
   * Evaluates a metric formula with provided data context
   *
   * @param formula The formula to evaluate
   * @param dataContext Map containing aggregated data (e.g., {"totalPitches": 10,
   *     "completedPitches": 8})
   * @return Calculated value
   */
  public BigDecimal evaluateFormula(String formula, Map<String, Object> dataContext) {
    try {
      // Replace field references with actual values from context
      String processedFormula = replaceFieldReferences(formula, dataContext);

      // Replace function calls with computed values
      processedFormula = replaceFunctionCalls(processedFormula, dataContext);

      // Evaluate the resulting mathematical expression
      return evaluateMathExpression(processedFormula);
    } catch (Exception e) {
      log.error("Formula evaluation failed: {}", formula, e);
      throw new IllegalArgumentException("Failed to evaluate formula: " + e.getMessage());
    }
  }

  /** Extracts field references from a formula */
  public List<FieldReference> extractFieldReferences(String formula) {
    List<FieldReference> references = new ArrayList<>();
    Matcher matcher = FIELD_PATTERN.matcher(formula);

    while (matcher.find()) {
      String entity = matcher.group(1);
      String field = matcher.group(2);
      references.add(new FieldReference(entity, field));
    }

    return references;
  }

  /** Extracts WHERE clause conditions from a formula */
  public String extractWhereClause(String formula) {
    Matcher matcher = WHERE_PATTERN.matcher(formula);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private boolean hasBalancedParentheses(String formula) {
    int count = 0;
    for (char c : formula.toCharArray()) {
      if (c == '(') count++;
      if (c == ')') count--;
      if (count < 0) return false;
    }
    return count == 0;
  }

  private boolean isValidFunction(String function) {
    return function.matches("(SUM|AVG|COUNT|MIN|MAX)");
  }

  private boolean isValidFieldReference(String entity, String field) {
    // Define valid entity.field combinations
    Map<String, List<String>> validFields =
        Map.of(
            "pitch", List.of("id", "appetiteDays", "actualHours", "status", "title"),
            "cycle", List.of("id", "name", "startDate", "endDate", "status"),
            "workLog", List.of("id", "hours", "hoursSpent", "date"),
            "task", List.of("id", "status", "estimateHours", "actualHours"),
            "team", List.of("id", "name", "memberCount"),
            "person", List.of("id", "name", "email"));

    return validFields.containsKey(entity.toLowerCase())
        && validFields.get(entity.toLowerCase()).contains(field);
  }

  private String replaceFieldReferences(String formula, Map<String, Object> dataContext) {
    Matcher matcher = FIELD_PATTERN.matcher(formula);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String fullReference = matcher.group(0);
      String contextKey = fullReference.replace(".", "_");
      Object value = dataContext.get(contextKey);

      if (value != null) {
        matcher.appendReplacement(result, value.toString());
      } else {
        matcher.appendReplacement(result, "0");
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String replaceFunctionCalls(String formula, Map<String, Object> dataContext) {
    Matcher matcher = FUNCTION_PATTERN.matcher(formula);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String function = matcher.group(1);
      String argument = matcher.group(2);
      String contextKey =
          function.toLowerCase() + "_" + argument.replace(".", "_").replace(" ", "_");

      Object value = dataContext.get(contextKey);
      if (value != null) {
        matcher.appendReplacement(result, value.toString());
      } else {
        matcher.appendReplacement(result, "0");
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private BigDecimal evaluateMathExpression(String expression) {
    // Remove spaces
    expression = expression.replaceAll("\\s+", "");

    // Simple evaluation using recursive descent parser
    return new ExpressionEvaluator(expression).evaluate();
  }

  /** Simple recursive descent parser for mathematical expressions */
  private static class ExpressionEvaluator {
    private final String expression;
    private int pos = -1;
    private int ch;

    ExpressionEvaluator(String expression) {
      this.expression = expression;
      nextChar();
    }

    void nextChar() {
      ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    boolean eat(int charToEat) {
      while (ch == ' ') nextChar();
      if (ch == charToEat) {
        nextChar();
        return true;
      }
      return false;
    }

    BigDecimal evaluate() {
      BigDecimal x = parseExpression();
      if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
      return x;
    }

    BigDecimal parseExpression() {
      BigDecimal x = parseTerm();
      for (; ; ) {
        if (eat('+')) x = x.add(parseTerm());
        else if (eat('-')) x = x.subtract(parseTerm());
        else return x;
      }
    }

    BigDecimal parseTerm() {
      BigDecimal x = parseFactor();
      for (; ; ) {
        if (eat('*')) x = x.multiply(parseFactor());
        else if (eat('/')) {
          BigDecimal divisor = parseFactor();
          if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            // Return 0 instead of throwing exception when dividing by zero
            return BigDecimal.ZERO;
          }
          x = x.divide(divisor, 4, RoundingMode.HALF_UP);
        } else return x;
      }
    }

    BigDecimal parseFactor() {
      if (eat('+')) return parseFactor();
      if (eat('-')) return parseFactor().negate();

      BigDecimal x;
      int startPos = this.pos;
      if (eat('(')) {
        x = parseExpression();
        eat(')');
      } else if ((ch >= '0' && ch <= '9') || ch == '.') {
        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
        x = new BigDecimal(expression.substring(startPos, this.pos));
      } else {
        throw new RuntimeException("Unexpected: " + (char) ch);
      }

      return x;
    }
  }

  public static class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
      this.valid = valid;
      this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult error(String message) {
      return new ValidationResult(false, message);
    }

    public boolean isValid() {
      return valid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  public record FieldReference(String entity, String field) {
    public String getFullReference() {
      return entity + "." + field;
    }
  }
}
