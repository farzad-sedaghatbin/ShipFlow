package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link QAService#resolveMinScore()} — the provider-aware retrieval threshold.
 *
 * <p>Regression for the Qdrant retrieval bug: a single hardcoded 0.70 threshold (calibrated
 * for the in-memory store's {@code (cosine+1)/2} scale) rejected every Qdrant match (raw
 * cosine ~0.4–0.6) and silently collapsed retrieval to the keyword-only fallback.
 */
@ExtendWith(MockitoExtension.class)
class QAServiceMinScoreTest {

  @InjectMocks
  private QAService qaService;

  private void config(String provider, double configuredMinScore) {
    ReflectionTestUtils.setField(qaService, "vectorStoreProvider", provider);
    ReflectionTestUtils.setField(qaService, "configuredMinScore", configuredMinScore);
  }

  @Test
  @DisplayName("in-memory store keeps the 0.70 threshold")
  void inMemoryUsesHighThreshold() {
    config("in-memory", -1);
    assertThat(qaService.resolveMinScore()).isCloseTo(0.70, within(1e-9));
  }

  @Test
  @DisplayName("Qdrant (raw cosine) drops to the 0.40 equivalent threshold")
  void qdrantUsesRawCosineThreshold() {
    config("qdrant", -1);
    assertThat(qaService.resolveMinScore()).isCloseTo(0.40, within(1e-9));
  }

  @Test
  @DisplayName("Other external stores also use the raw-cosine threshold")
  void otherExternalStoresUseRawCosineThreshold() {
    config("chroma", -1);
    assertThat(qaService.resolveMinScore()).isCloseTo(0.40, within(1e-9));
  }

  @Test
  @DisplayName("an explicit min-score override wins regardless of provider")
  void explicitOverrideWins() {
    config("qdrant", 0.55);
    assertThat(qaService.resolveMinScore()).isCloseTo(0.55, within(1e-9));
  }

  @Test
  @DisplayName("null provider defaults to the in-memory threshold")
  void nullProviderDefaultsToInMemory() {
    config(null, -1);
    assertThat(qaService.resolveMinScore()).isCloseTo(0.70, within(1e-9));
  }

  @Test
  @DisplayName("keyword tokenizer strips trailing punctuation (intensive? -> intensive)")
  @SuppressWarnings("unchecked")
  void extractSearchTermsStripsPunctuation() throws Exception {
    Method m = QAService.class.getDeclaredMethod("extractSearchTerms", String.class);
    m.setAccessible(true);
    List<String> terms = (List<String>) m.invoke(qaService, "what is data intensive?");
    assertThat(terms).contains("intensive").doesNotContain("intensive?");
  }
}
