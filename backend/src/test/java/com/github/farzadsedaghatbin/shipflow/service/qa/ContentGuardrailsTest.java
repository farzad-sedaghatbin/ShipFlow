package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentGuardrailsTest {

  private ContentGuardrails contentGuardrails;

  @BeforeEach
  void setUp() {
    contentGuardrails = new ContentGuardrails();
  }

  @Test
  void validate_cleanContent_returnsSafe() {
    // Given
    String cleanAnswer = "The payment feature includes secure checkout and fraud detection.";
    double confidence = 0.9;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(cleanAnswer, confidence);

    // Then
    assertThat(result.isSafe()).isTrue();
    assertThat(result.getSafetyScore()).isGreaterThanOrEqualTo(50);
    assertThat(result.getViolations()).isEmpty();
  }

  @Test
  void validate_toxicContent_returnsUnsafe() {
    // Given
    String toxicAnswer = "This feature is stupid and you're an idiot for asking.";
    double confidence = 0.9;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(toxicAnswer, confidence);

    // Then
    assertThat(result.isSafe()).isFalse();
    assertThat(result.getSafetyScore()).isLessThan(50);
    assertThat(result.getViolations()).contains("TOXIC_CONTENT");
  }

  @Test
  void validate_biasedContent_flagsBias() {
    // Given
    String biasedAnswer = "This is always the best approach and obviously better than alternatives.";
    double confidence = 0.9;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(biasedAnswer, confidence);

    // Then
    assertThat(result.getViolations()).contains("POTENTIAL_BIAS");
  }

  @Test
  void validate_hallucinationIndicators_flagsLowConfidence() {
    // Given
    String uncertainAnswer = "I think the payment feature might work, but I'm not sure. Probably it does.";
    double confidence = 0.9;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(uncertainAnswer, confidence);

    // Then
    assertThat(result.getViolations()).contains("LOW_CONFIDENCE_HALLUCINATION");
  }

  @Test
  void sanitize_toxicContent_returnsError() {
    // Given
    String toxic = "This is stupid.";
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(toxic, 0.9);

    // When
    String sanitized = contentGuardrails.sanitize(toxic, result);

    // Then
    assertThat(sanitized).contains("cannot provide a response");
    assertThat(sanitized).contains("safety guidelines");
  }

  @Test
  void sanitize_biasedContent_addsDisclaimer() {
    // Given
    String biased = "This is always the best.";
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(biased, 0.9);

    // When
    String sanitized = contentGuardrails.sanitize(biased, result);

    // Then
    assertThat(sanitized).contains(biased);
    assertThat(sanitized).contains("Note:");
  }

  @Test
  void sanitize_hallucinationContent_addsConfidenceWarning() {
    // Given
    String uncertain = "I think this probably works.";
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(uncertain, 0.5);

    // When
    String sanitized = contentGuardrails.sanitize(uncertain, result);

    // Then
    assertThat(sanitized).contains(uncertain);
    assertThat(sanitized).containsIgnoringCase("confidence");
  }

  @Test
  void sanitize_cleanContent_returnsAsIs() {
    // Given
    String clean = "The feature works well.";
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(clean, 0.9);

    // When
    String sanitized = contentGuardrails.sanitize(clean, result);

    // Then
    assertThat(sanitized).isEqualTo(clean);
  }

  @Test
  void validate_lowConfidence_flagsAutomatically() {
    // Given
    String answer = "The payment feature works.";
    double lowConfidence = 0.3;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(answer, lowConfidence);

    // Then
    assertThat(result.getViolations()).contains("LOW_CONFIDENCE");
  }

  @Test
  void validate_highConfidence_noLowConfidenceFlag() {
    // Given
    String answer = "The payment feature works.";
    double highConfidence = 0.9;

    // When
    ContentGuardrails.GuardrailResult result = contentGuardrails.validate(answer, highConfidence);

    // Then
    assertThat(result.getViolations()).doesNotContain("LOW_CONFIDENCE");
  }
}
