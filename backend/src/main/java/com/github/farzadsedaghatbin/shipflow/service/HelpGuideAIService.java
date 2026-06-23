package com.github.farzadsedaghatbin.shipflow.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Dedicated AI service for the Help Guide search feature.
 * Completely separate from the business-logic Q&A (QAService).
 *
 * Uses the shared EmbeddingStore and EmbeddingModel infrastructure
 * to do semantic retrieval of help documentation, ensuring only
 * the most relevant chunks are included in the LLM prompt
 * for token efficiency.
 *
 * Knowledge base is loaded from classpath:knowledgebase/help-guides/*.md
 * at startup, chunked, and embedded into the vector store with metadata
 * tag "source=help-guide" to distinguish from business Q&A data.
 */
@Service
@Slf4j
public class HelpGuideAIService {

    private static final String HELP_GUIDE_SOURCE = "help-guide";
    private static final int CHUNK_SIZE = 500; // ~500 chars per chunk
    private static final int TOP_K = 5; // Retrieve top 5 relevant chunks
    private static final int FALLBACK_MAX_RESULTS = 100; // Over-retrieve when metadata filtering is unavailable
    private static final double MIN_SCORE = 0.40; // Recall-friendly threshold for documentation retrieval

    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private boolean knowledgeBaseLoaded = false;

    public HelpGuideAIService(
            @Autowired(required = false) ChatLanguageModel chatLanguageModel,
            @Autowired(required = false) EmbeddingModel embeddingModel,
            @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Load help guide markdown files, chunk them, and embed into the vector store.
     * Uses metadata tag "source=help-guide" to namespace from business data.
     */
    @PostConstruct
    void loadAndEmbedKnowledgeBase() {
        if (embeddingModel == null || embeddingStore == null) {
            log.warn("HelpGuideAIService: EmbeddingModel or EmbeddingStore not available, "
                    + "knowledge base will not be loaded. AI answers will use system prompt only.");
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledgebase/help-guides/*.md");

            int totalChunks = 0;

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || "README.md".equalsIgnoreCase(filename)) {
                    continue;
                }
                try {
                    String content = resource.getContentAsString(StandardCharsets.UTF_8);
                    List<TextSegment> chunks = chunkDocument(content, filename);

                    for (TextSegment chunk : chunks) {
                        Embedding embedding = embeddingModel.embed(chunk).content();
                        embeddingStore.add(embedding, chunk);
                        totalChunks++;
                    }

                    log.debug("Embedded {} chunks from {}", chunks.size(), filename);
                } catch (IOException e) {
                    log.warn("Failed to read help guide resource: {}", filename, e);
                }
            }

            knowledgeBaseLoaded = totalChunks > 0;
            log.info("HelpGuideAIService: embedded {} chunks from help guide knowledge base", totalChunks);

        } catch (IOException e) {
            log.error("Failed to scan help guide knowledge base resources", e);
        }
    }

    /**
     * Split document content into semantic chunks based on markdown headings.
     * Each chunk includes the section heading for context.
     */
    private List<TextSegment> chunkDocument(String content, String filename) {
        List<TextSegment> chunks = new ArrayList<>();
        // Split before every H1 or H2 heading. Splitting on H1 too ensures a mid-file H1
        // (e.g. "# Bug Reports" inside a multi-topic guide) correctly becomes the parent
        // context for the H2 sections that follow it — previously only the very first H1
        // was ever recorded, so later sections were mislabeled with the wrong title.
        String[] sections = content.split("(?=^#{1,2} )", java.util.regex.Pattern.MULTILINE);

        String currentH1 = filename; // Fallback title
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty())
                continue;

            // An H1 heading (single "# ", not "## ") sets the parent context for the chunks
            // that follow it.
            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                int newlineIdx = trimmed.indexOf('\n');
                currentH1 = newlineIdx > 0 ? trimmed.substring(2, newlineIdx).trim() : trimmed.substring(2).trim();

                // Don't emit a tiny heading-only chunk — when an H1 has no body of its own it
                // just updates the parent context for the next section.
                String body = newlineIdx > 0 ? trimmed.substring(newlineIdx).trim() : "";
                if (body.isEmpty())
                    continue;
            }

            // If the section is too long, split into smaller sub-chunks
            if (trimmed.length() > CHUNK_SIZE * 2) {
                List<String> subChunks = splitLargeSection(trimmed);
                for (String subChunk : subChunks) {
                    chunks.add(TextSegment.from(subChunk, helpMetadata(filename, currentH1)));
                }
            } else {
                chunks.add(TextSegment.from(trimmed, helpMetadata(filename, currentH1)));
            }
        }

        return chunks;
    }

    private Metadata helpMetadata(String filename, String title) {
        return new Metadata()
                .put("source", HELP_GUIDE_SOURCE)
                .put("filename", filename)
                .put("title", title);
    }

    private List<String> splitLargeSection(String section) {
        List<String> parts = new ArrayList<>();
        String[] paragraphs = section.split("\n\n");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() > CHUNK_SIZE && current.length() > 0) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(paragraph).append("\n\n");
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    public boolean isAvailable() {
        return chatLanguageModel != null;
    }

    /**
     * Answer a help-guide question using vector search retrieval + guardrails.
     * Only the top-K relevant document chunks are included in the prompt.
     */
    public Map<String, Object> ask(String question) {
        long startTime = System.currentTimeMillis();

        if (chatLanguageModel == null) {
            return buildResponse(question,
                    "AI is not currently available. Please browse the guide cards below for help.",
                    false, startTime, null, getDefaultSuggestions());
        }

        try {
            // Retrieve relevant documentation chunks via vector search
            String context = retrieveRelevantContext(question);

            // Build prompt with only the relevant context
            String prompt = buildPromptWithContext(question, context);
            String answer = chatLanguageModel.generate(prompt);
            long elapsed = System.currentTimeMillis() - startTime;

            return buildResponse(question, answer, true, startTime, null, getSuggestedFollowUps(question));

        } catch (Exception e) {
            log.error("Help Guide AI error: {}", e.getMessage(), e);
            return buildResponse(question,
                    "Sorry, I couldn't process your question right now. Please try again or browse the guides below.",
                    true, startTime, e.getMessage(), getDefaultSuggestions());
        }
    }

    /**
     * Search the vector store for chunks relevant to the user's question.
     * Returns formatted context string with the top-K matches.
     */
    private String retrieveRelevantContext(String question) {
        if (embeddingModel == null || embeddingStore == null || !knowledgeBaseLoaded) {
            log.debug("Vector search not available, returning empty context");
            return "";
        }

        try {
            Embedding questionEmbedding = embeddingModel.embed(question).content();

            List<EmbeddingMatch<TextSegment>> helpMatches = searchHelpGuideChunks(questionEmbedding);

            if (helpMatches.isEmpty()) {
                log.debug("No relevant help guide chunks found for question: {}", question);
                return "";
            }

            // Build context from matched chunks
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < helpMatches.size(); i++) {
                EmbeddingMatch<TextSegment> match = helpMatches.get(i);
                TextSegment segment = match.embedded();
                String title = segment.metadata().getString("title");
                context.append("[Source ").append(i + 1);
                if (title != null)
                    context.append(": ").append(title);
                context.append("] (relevance: ").append(String.format("%.0f%%", match.score() * 100)).append(")\n");
                context.append(segment.text()).append("\n\n");
            }

            log.info("Retrieved {} relevant help guide chunks for question (avg score: {})",
                    helpMatches.size(),
                    String.format("%.2f", helpMatches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0)));

            return context.toString();

        } catch (Exception e) {
            log.warn("Vector search failed for help guide: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Retrieve the top help-guide chunks for a query.
     *
     * <p>The vector store is shared with the business Q&A knowledge base, so a plain similarity
     * search can return only higher-scoring business chunks and leave nothing once we filter by
     * {@code source=help-guide}. To avoid that, we first restrict the search to help-guide chunks
     * with a metadata filter. If the configured store does not support metadata filtering, we fall
     * back to over-retrieving a large candidate set and filtering by source in memory.
     */
    private List<EmbeddingMatch<TextSegment>> searchHelpGuideChunks(Embedding queryEmbedding) {
        try {
            EmbeddingSearchRequest filtered = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(TOP_K)
                    .minScore(MIN_SCORE)
                    .filter(metadataKey("source").isEqualTo(HELP_GUIDE_SOURCE))
                    .build();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(filtered).matches();
            if (!matches.isEmpty()) {
                return matches.stream().limit(TOP_K).toList();
            }
        } catch (RuntimeException e) {
            log.debug("Metadata-filtered help guide search unavailable ({}), falling back to over-retrieve",
                    e.getMessage());
        }

        EmbeddingSearchRequest broad = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(FALLBACK_MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();
        return embeddingStore.search(broad).matches().stream()
                .filter(match -> match.embedded() != null && match.embedded().metadata() != null
                        && HELP_GUIDE_SOURCE.equals(match.embedded().metadata().getString("source")))
                .limit(TOP_K)
                .toList();
    }

    private String buildPromptWithContext(String question, String context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                """
                        You are a ShipFlow documentation assistant. Your ONLY job is to help users understand how to use ShipFlow features.

                        GUARDRAILS:
                        - ONLY answer questions about how to use ShipFlow.
                        - If the question is NOT about ShipFlow, politely decline: "I can only help with ShipFlow-related questions. Try asking about cycles, pitches, hill charts, exports, webhooks, API, or other ShipFlow features!"
                        - Base your answers on the documentation provided below.
                        - Keep answers practical, clear, and step-by-step.
                        - Use markdown formatting for readability.
                        - If the documentation doesn't cover a topic, say so honestly and suggest browsing the guide cards.

                        """);

        if (!context.isEmpty()) {
            prompt.append("===== RELEVANT DOCUMENTATION =====\n");
            prompt.append(context);
            prompt.append("===== END DOCUMENTATION =====\n\n");
        } else {
            prompt.append("Note: No specific documentation was found for this question. "
                    + "Answer based on your general knowledge of ShipFlow and the Shape Up methodology, "
                    + "but warn the user that they should check the official guides for accuracy.\n\n");
        }

        prompt.append("User Question: ").append(question).append("\n\nAnswer:\n");

        return prompt.toString();
    }

    private Map<String, Object> buildResponse(String question, String answer, boolean aiEnabled,
            long startTime, String errorMessage, List<String> suggestions) {
        long elapsed = System.currentTimeMillis() - startTime;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("aiEnabled", aiEnabled);
        response.put("processingTimeMs", elapsed);
        response.put("suggestedFollowUps", suggestions);
        if (errorMessage != null) {
            response.put("errorMessage", errorMessage);
        }
        return response;
    }

    private List<String> getSuggestedFollowUps(String question) {
        String lower = question.toLowerCase();

        if (lower.contains("cycle"))
            return List.of("How do I run a betting meeting?", "How do cooldown activities work?",
                    "How do I use hill charts?");
        if (lower.contains("pitch"))
            return List.of("How does the betting table work?", "How do I track pitch progress?",
                    "What is appetite in Shape Up?");
        if (lower.contains("hill") || lower.contains("chart"))
            return List.of("How do I move scopes on the hill chart?", "What does 'over the hill' mean?",
                    "What is the circuit breaker?");
        if (lower.contains("export") || lower.contains("data"))
            return List.of("How do I use the public API?", "How do I set up webhooks?", "How do I generate reports?");
        if (lower.contains("webhook"))
            return List.of("How do I configure Slack alerts?", "How do I use the public API?",
                    "What is circuit breaker?");
        if (lower.contains("api"))
            return List.of("How do I create a personal access token?", "How do I export data?",
                    "How do I set up webhooks?");
        if (lower.contains("retro"))
            return List.of("How do I create a retrospective?", "How do I export cycle summaries?",
                    "How do cooldown activities work?");
        if (lower.contains("release"))
            return List.of("How do I create a release?", "How do I track bugs for a release?",
                    "How do I generate release notes?");
        if (lower.contains("backlog"))
            return List.of("How do I manage the backlog?", "How do I create pitches from backlog items?",
                    "How do I use the hill chart?");
        if (lower.contains("dashboard"))
            return List.of("How do I customize my dashboard?", "How do I add widgets?", "How do I view cycle health?");
        if (lower.contains("initiative") || lower.contains("epic") || lower.contains("roadmap"))
            return List.of("How do I create an initiative?", "How do epics relate to pitches?",
                    "How do I view the roadmap?");
        if (lower.contains("plugin") || lower.contains("extension") || lower.contains("sdk"))
            return List.of("How do I enable or disable a plugin?", "How do I build a custom plugin?",
                    "What built-in plugins does ShipFlow include?");
        if (lower.contains("mcp") || lower.contains("claude") || lower.contains("cursor") || lower.contains("ai tool"))
            return List.of("How do I connect Claude Code to ShipFlow?", "How do I create an API key?",
                    "How do I enable the MCP server?");
        if (lower.contains("notion") || lower.contains("confluence"))
            return List.of("How do I set up a Notion MCP connection?", "How do I set up a Confluence MCP connection?",
                    "How do I configure MCP integrations?");
        if (lower.contains("preview") || lower.contains("share") || lower.contains("slack") || lower.contains("whatsapp"))
            return List.of("How do I share a task link with a preview?", "How do I copy a shareable link?",
                    "How do I configure webhooks?");
        if (lower.contains("import") || lower.contains("migrat") || lower.contains("jira") || lower.contains("linear")
                || lower.contains("asana") || lower.contains("csv"))
            return List.of("How do I import a CSV file?", "How do I import live from Jira?",
                    "What gets created when I import?");
        if (lower.contains("scrum") || lower.contains("sprint") || lower.contains("story point")
                || lower.contains("velocity") || lower.contains("burndown") || lower.contains("kanban"))
            return List.of("How does Sprint Planning work?", "How do I choose a project methodology?",
                    "What's the difference between Shape Up, Kanban, and Scrum?");
        if (lower.contains("knowledge"))
            return List.of("How do I add a knowledge source?", "How do I refresh a knowledge source?",
                    "What does the AI use my documents for?");
        if (lower.contains("teams") || lower.contains("microsoft"))
            return List.of("How do I connect Microsoft Teams?", "Which events notify a Teams channel?",
                    "How do I send a Teams test notification?");
        if (lower.contains("saved view") || lower.contains("filter view") || lower.contains("attach")
                || lower.contains("@mention") || lower.contains("mention"))
            return List.of("How do I save a backlog filter view?", "How do I attach a file to a task?",
                    "How do I @mention a teammate?");

        return getDefaultSuggestions();
    }

    private List<String> getDefaultSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("How do I set up my first cycle?");
        suggestions.add("How do I export a cycle summary?");
        suggestions.add("How do I configure webhooks?");
        return suggestions;
    }
}
