package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link QAService#mergeHelpGuideMatches}.
 *
 * <p>Regression for a production bug: the global, non-entity-scoped Q&A widget ran one plain
 * similarity search across the entire shared vector store, so help-guide chunks competed
 * unfiltered against every embedded business chunk and were statistically crowded out of the
 * top-K on a non-trivial org — e.g. "what is a hill chart" surfaced an unrelated risk summary
 * instead of the hill-chart help guide. This merges in a dedicated help-guide-filtered search,
 * mirroring {@link HelpGuideAIService#searchHelpGuideChunks}.
 */
@ExtendWith(MockitoExtension.class)
class QAServiceHelpGuideMergeTest {

  @Mock
  private KnowledgeItemRepository knowledgeItemRepository;

  @Mock
  private QAInteractionRepository qaInteractionRepository;

  @Mock
  private AICacheService cacheService;

  @Mock
  private MessageService messageService;

  @Mock
  private EmbeddingStore<TextSegment> embeddingStore;

  @InjectMocks
  private QAService qaService;

  private Method mergeMethod;

  @BeforeEach
  void setUp() throws Exception {
    mergeMethod = QAService.class.getDeclaredMethod("mergeHelpGuideMatches", List.class, Embedding.class,
        int.class, double.class);
    mergeMethod.setAccessible(true);
  }

  private EmbeddingMatch<TextSegment> businessMatch(double score, String text) {
    TextSegment segment = TextSegment.from(text, new Metadata());
    return new EmbeddingMatch<>(score, "embed-" + text.hashCode(), null, segment);
  }

  private EmbeddingMatch<TextSegment> helpGuideMatch(double score, String text) {
    Metadata metadata = new Metadata();
    metadata.put("source", "help-guide");
    TextSegment segment = TextSegment.from(text, metadata);
    return new EmbeddingMatch<>(score, "embed-" + text.hashCode(), null, segment);
  }

  private static final Embedding DUMMY_QUESTION_EMBEDDING = Embedding.from(new float[] {0.1f, 0.2f, 0.3f});

  @SuppressWarnings("unchecked")
  private List<EmbeddingMatch<TextSegment>> invokeMerge(List<EmbeddingMatch<TextSegment>> matches) throws Exception {
    return (List<EmbeddingMatch<TextSegment>>) mergeMethod.invoke(qaService, matches, DUMMY_QUESTION_EMBEDDING, 5,
        0.70);
  }

  @Test
  @DisplayName("merges in a help-guide chunk the general search missed")
  void mergesInMissingHelpGuideChunk() throws Exception {
    List<EmbeddingMatch<TextSegment>> generalMatches = List.of(businessMatch(0.80, "Risk summary: 0% progress"));
    EmbeddingMatch<TextSegment> guideMatch = helpGuideMatch(0.75, "A hill chart visualizes uphill/downhill progress");

    when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
        .thenReturn(new EmbeddingSearchResult<>(List.of(guideMatch)));

    List<EmbeddingMatch<TextSegment>> result = invokeMerge(generalMatches);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(m -> m.embedded().text())
        .contains("Risk summary: 0% progress", "A hill chart visualizes uphill/downhill progress");
    verify(embeddingStore, times(1)).search(any(EmbeddingSearchRequest.class));
  }

  @Test
  @DisplayName("does not duplicate a help-guide chunk the general search already found")
  void doesNotDuplicateAlreadyPresentChunk() throws Exception {
    EmbeddingMatch<TextSegment> guideMatchInGeneral = helpGuideMatch(0.85, "A hill chart visualizes progress");
    List<EmbeddingMatch<TextSegment>> generalMatches = List.of(guideMatchInGeneral);

    // The dedicated filtered search finds the SAME chunk (same text) again.
    when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
        .thenReturn(new EmbeddingSearchResult<>(List.of(helpGuideMatch(0.85, "A hill chart visualizes progress"))));

    List<EmbeddingMatch<TextSegment>> result = invokeMerge(generalMatches);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("falls back to an over-retrieve-then-filter search when metadata filtering returns nothing")
  void fallsBackToOverRetrieveWhenFilteredSearchIsEmpty() throws Exception {
    List<EmbeddingMatch<TextSegment>> generalMatches = List.of(businessMatch(0.80, "Unrelated business chunk"));
    EmbeddingMatch<TextSegment> guideMatch = helpGuideMatch(0.72, "Hill chart guide content");
    EmbeddingMatch<TextSegment> otherBusinessMatch = businessMatch(0.90, "Another business chunk");

    when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
        // 1st call: metadata-filtered search — store doesn't support it / returns nothing
        .thenReturn(new EmbeddingSearchResult<>(List.of()))
        // 2nd call: broad over-retrieve — filtered in-memory by source afterward
        .thenReturn(new EmbeddingSearchResult<>(List.of(otherBusinessMatch, guideMatch)));

    List<EmbeddingMatch<TextSegment>> result = invokeMerge(generalMatches);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(m -> m.embedded().text())
        .contains("Unrelated business chunk", "Hill chart guide content")
        .doesNotContain("Another business chunk");
    verify(embeddingStore, times(2)).search(any(EmbeddingSearchRequest.class));
  }

  @Test
  @DisplayName("returns the original matches unchanged when the help-guide search fails entirely")
  void returnsOriginalMatchesOnFailure() throws Exception {
    List<EmbeddingMatch<TextSegment>> generalMatches = List.of(businessMatch(0.80, "Some business chunk"));

    when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
        .thenThrow(new RuntimeException("vector store unavailable"));

    List<EmbeddingMatch<TextSegment>> result = invokeMerge(generalMatches);

    assertThat(result).isEqualTo(generalMatches);
  }
}
