package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptCompressorTest {

  private PromptCompressor promptCompressor;

  @BeforeEach
  void setUp() {
    promptCompressor = new PromptCompressor();
  }

  @Test
  void compress_removesFillerWords() {
    // Given
    String prompt =
        "Actually, this is basically a very important question that is literally essential.";

    // When
    String compressed = promptCompressor.compress(prompt);

    // Then
    assertThat(compressed).doesNotContain("Actually");
    assertThat(compressed).doesNotContain("basically");
    assertThat(compressed).doesNotContain("very");
    assertThat(compressed).doesNotContain("literally");
    assertThat(compressed).contains("important");
    assertThat(compressed).contains("essential");
  }

  @Test
  void compress_removesMultipleSpaces() {
    // Given
    String prompt = "This  has    multiple     spaces.";

    // When
    String compressed = promptCompressor.compress(prompt);

    // Then
    assertThat(compressed).doesNotContain("  "); // No double spaces
  }

  @Test
  void compress_removesMultipleNewlines() {
    // Given
    String prompt = "Line 1\n\n\nLine 2\n\n\n\nLine 3";

    // When
    String compressed = promptCompressor.compress(prompt);

    // Then
    assertThat(compressed).doesNotContain("\n\n\n"); // Max 2 consecutive newlines
  }

  @Test
  void compress_preservesImportantContent() {
    // Given
    String prompt = "What are the payment feature risks for Q1 2024?";

    // When
    String compressed = promptCompressor.compress(prompt);

    // Then
    assertThat(compressed).contains("payment");
    assertThat(compressed).contains("risks");
    assertThat(compressed).contains("Q1");
    assertThat(compressed).contains("2024");
  }

  @Test
  void compressContext_underTokenLimit_returnsAsIs() {
    // Given
    String context = "Short context";
    int maxTokens = 1000;

    // When
    String compressed = promptCompressor.compressContext(context, maxTokens);

    // Then
    assertThat(compressed).isEqualTo(context);
  }

  @Test
  void compressContext_overTokenLimit_truncates() {
    // Given
    String context = "A".repeat(20000); // 20k characters = ~5000 tokens (4 chars/token)
    int maxTokens = 1000; // Allow 1000 tokens = ~4000 characters

    // When
    String compressed = promptCompressor.compressContext(context, maxTokens);

    // Then
    assertThat(compressed.length()).isLessThanOrEqualTo(4000);
    assertThat(compressed).endsWith("...[truncated]");
  }

  @Test
  void compress_nullPrompt_returnsNull() {
    // When
    String compressed = promptCompressor.compress(null);

    // Then
    assertThat(compressed).isNull();
  }

  @Test
  void compressContext_nullContext_returnsNull() {
    // When
    String compressed = promptCompressor.compressContext(null, 1000);

    // Then
    assertThat(compressed).isNull();
  }

  @Test
  void compress_complexExample_reducesSize() {
    // Given
    String prompt = "Actually basically very " + "A".repeat(1000);

    // When
    String compressed = promptCompressor.compress(prompt);

    // Then
    assertThat(compressed.length()).isLessThan(prompt.length());
  }
}
