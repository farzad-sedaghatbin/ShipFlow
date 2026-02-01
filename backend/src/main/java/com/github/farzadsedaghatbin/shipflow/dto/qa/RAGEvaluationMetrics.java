package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/** Evaluation metrics for RAG (Retrieval-Augmented Generation) quality. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGEvaluationMetrics {

  /** How relevant the retrieved documents are (0-100). */
  private Double retrievalRelevance;

  /** Whether the answer stays grounded in sources (0-100). Low score indicates hallucination. */
  private Double faithfulness;

  /** How well the answer addresses the question (0-100). */
  private Double answerRelevance;

  /** Percentage of retrieved docs actually used in answer (0-100). */
  private Double contextUtilization;

  /** Number of documents retrieved. */
  private Integer documentsRetrieved;

  /** Number of documents actually cited. */
  private Integer documentsCited;

  /** Average similarity score of retrieved docs. */
  private Double averageSimilarityScore;
}
