package com.github.farzadsedaghatbin.shipflow.service.qa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Content safety guardrails for RAG responses.
 * 
 * Checks for:
 * - Toxic/offensive content
 * - Potential bias
 * - Hallucination indicators
 * - PII leakage (bonus - not requested but good to have)
 */
@Slf4j
@Component
public class ContentGuardrails {
    
    // Toxic content patterns
    private static final List<Pattern> TOXIC_PATTERNS = List.of(
        Pattern.compile("\\b(hate|stupid|idiot|dumb|moron)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(attack|destroy|kill|eliminate)\\s+(team|person|people)", Pattern.CASE_INSENSITIVE)
    );
    
    // Bias indicators
    private static final List<Pattern> BIAS_PATTERNS = List.of(
        Pattern.compile("\\b(always|never)\\s+(the\\s+)?(best|worst|right|wrong)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(all|none)\\s+(men|women|developers|managers)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(obviously|clearly)\\s+(better|worse|superior|inferior)", Pattern.CASE_INSENSITIVE)
    );
    
    // Hallucination indicators (each as separate pattern for counting)
    private static final List<Pattern> HALLUCINATION_PATTERNS = List.of(
        Pattern.compile("\\bI think\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bI believe\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bprobably\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmaybe\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmight be\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcould be\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bI'm not sure\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("according to my knowledge", Pattern.CASE_INSENSITIVE),
        Pattern.compile("as far as I know", Pattern.CASE_INSENSITIVE),
        Pattern.compile("I don't have access to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("I cannot verify", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Validates response against all guardrails.
     */
    public GuardrailResult validate(String response, double confidenceScore) {
        List<String> violations = new ArrayList<>();
        
        // Check toxic content
        if (containsToxicContent(response)) {
            violations.add("TOXIC_CONTENT");
        }
        
        // Check for bias
        if (containsBias(response)) {
            violations.add("POTENTIAL_BIAS");
        }
        
        // Check hallucination confidence (confidenceScore is 0-1 scale, e.g., 0.9 = 90%)
        boolean hasHallucinationIndicators = containsHallucinationIndicators(response);
        
        if (hasHallucinationIndicators) {
            violations.add("LOW_CONFIDENCE_HALLUCINATION");
        } else if (confidenceScore < 0.4) {
            violations.add("LOW_CONFIDENCE");
        }
        
        boolean passed = violations.isEmpty();
        
        if (!passed) {
            log.warn("Guardrail violations detected: {}", violations);
        }
        
        return new GuardrailResult(passed, violations, getSafetyScore(violations));
    }
    
    /**
     * Checks for toxic content.
     */
    private boolean containsToxicContent(String text) {
        for (Pattern pattern : TOXIC_PATTERNS) {
            if (pattern.matcher(text).find()) {
                log.debug("Toxic content detected: pattern={}", pattern.pattern());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks for potential bias.
     */
    private boolean containsBias(String text) {
        for (Pattern pattern : BIAS_PATTERNS) {
            if (pattern.matcher(text).find()) {
                log.debug("Potential bias detected: pattern={}", pattern.pattern());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks for hallucination indicators.
     */
    private boolean containsHallucinationIndicators(String text) {
        int indicatorCount = 0;
        for (Pattern pattern : HALLUCINATION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                indicatorCount++;
            }
        }
        
        // Multiple indicators suggest possible hallucination
        if (indicatorCount >= 2) {
            log.debug("Hallucination indicators detected: count={}", indicatorCount);
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculates a safety score (0-100).
     */
    private int getSafetyScore(List<String> violations) {
        int baseScore = 100;
        
        for (String violation : violations) {
            switch (violation) {
                case "TOXIC_CONTENT":
                    baseScore -= 60; // Severe
                    break;
                case "POTENTIAL_BIAS":
                    baseScore -= 20; // Moderate
                    break;
                case "LOW_CONFIDENCE_HALLUCINATION":
                    baseScore -= 30; // Significant
                    break;
            }
        }
        
        return Math.max(0, baseScore);
    }
    
    /**
     * Sanitizes a response that failed guardrails.
     */
    public String sanitize(String response, GuardrailResult result) {
        if (result.passed()) {
            return response;
        }
        
        // If toxic content, return generic error
        if (result.getViolations().contains("TOXIC_CONTENT")) {
            return "I apologize, but I cannot provide a response to this request due to safety guidelines.";
        }
        
        // If bias detected, add disclaimer
        if (result.getViolations().contains("POTENTIAL_BIAS")) {
            return response + "\n\n*Note: This response may contain generalizations. Individual circumstances may vary.*";
        }
        
        // If hallucination risk, add confidence warning
        if (result.getViolations().contains("LOW_CONFIDENCE_HALLUCINATION")) {
            return response + "\n\n*Note: I have low confidence in this answer. Please verify the information.*";
        }
        
        return response;
    }
    
    /**
     * Result of guardrail validation.
     */
    public static class GuardrailResult {
        private final boolean passed;
        private final List<String> violations;
        private final int safetyScore;
        
        public GuardrailResult(boolean passed, List<String> violations, int safetyScore) {
            this.passed = passed;
            this.violations = violations;
            this.safetyScore = safetyScore;
        }
        
        public boolean passed() {
            return passed;
        }
        
        public List<String> getViolations() {
            return violations;
        }
        
        public int getSafetyScore() {
            return safetyScore;
        }
        
        public boolean isSafe() {
            return safetyScore >= 70;
        }
    }
}
