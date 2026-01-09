package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Q&A feature status information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAStatusDTO {

    /**
     * Whether the Q&A feature is enabled.
     */
    private Boolean qaEnabled;

    /**
     * Whether AI (LLM) is available for answer generation.
     */
    private Boolean aiAvailable;

    /**
     * Type of vector store being used.
     */
    private String vectorStoreType;

    /**
     * Total number of knowledge items in the database.
     */
    private Long totalKnowledgeItems;

    /**
     * Number of knowledge items that have been embedded.
     */
    private Long embeddedKnowledgeItems;

    /**
     * Total number of Q&A interactions.
     */
    private Long totalInteractions;

    /**
     * Number of validated (accurate/corrected) Q&A interactions.
     */
    private Long validatedInteractions;

    /**
     * Name of the embedding model being used.
     */
    private String embeddingModel;

    /**
     * Name of the LLM model being used.
     */
    private String llmModel;
}
