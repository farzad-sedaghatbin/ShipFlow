package com.github.farzadsedaghatbin.shipflow.service.qa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compresses prompts to reduce token usage while preserving meaning.
 * 
 * Techniques:
 * - Remove redundant whitespace
 * - Deduplicate repeated phrases
 * - Remove filler words
 * - Compress verbose formatting
 */
@Slf4j
@Component
public class PromptCompressor {
    
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    
    private static final Set<String> FILLER_WORDS = Set.of(
        "actually", "basically", "really", "very", "quite", "rather",
        "somewhat", "simply", "just", "literally", "essentially"
    );
    
    /**
     * Compresses a prompt to reduce token count.
     */
    public String compress(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return prompt;
        }
        
        String compressed = prompt;
        int originalLength = compressed.length();
        
        // Remove redundant whitespace
        compressed = MULTIPLE_SPACES.matcher(compressed).replaceAll(" ");
        compressed = MULTIPLE_NEWLINES.matcher(compressed).replaceAll("\n\n");
        
        // Remove filler words (carefully to avoid breaking meaning)
        compressed = removeFillerWords(compressed);
        
        // Deduplicate repeated sentences
        compressed = deduplicateSentences(compressed);
        
        // Trim each line
        compressed = compressed.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"));
        
        int compressedLength = compressed.length();
        double savings = ((double) (originalLength - compressedLength) / originalLength) * 100;
        
        if (savings > 5) {
            log.debug("Compressed prompt: {} chars → {} chars ({:.1f}% reduction)", 
                    originalLength, compressedLength, savings);
        }
        
        return compressed;
    }
    
    /**
     * Removes filler words that don't add meaning.
     */
    private String removeFillerWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            String lowerWord = word.toLowerCase().replaceAll("[^a-z]", "");
            if (!FILLER_WORDS.contains(lowerWord)) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Deduplicates repeated sentences in the prompt.
     */
    private String deduplicateSentences(String text) {
        String[] sentences = text.split("\\.");
        Set<String> seen = new HashSet<>();
        StringBuilder result = new StringBuilder();
        
        for (String sentence : sentences) {
            String normalized = sentence.trim().toLowerCase();
            if (!normalized.isEmpty() && !seen.contains(normalized)) {
                seen.add(normalized);
                if (result.length() > 0) {
                    result.append(". ");
                }
                result.append(sentence.trim());
            }
        }
        
        if (result.length() > 0 && !result.toString().endsWith(".")) {
            result.append(".");
        }
        
        return result.toString();
    }
    
    /**
     * Compresses context by removing redundant information.
     */
    public String compressContext(String context, int maxTokens) {
        if (context == null) {
            return null;
        }
        if (context.isEmpty()) {
            return context;
        }
        
        // Estimate tokens (rough: 4 chars per token)
        int estimatedTokens = context.length() / 4;
        
        // If under limit, return as-is without compression
        if (estimatedTokens <= maxTokens) {
            return context;
        }
        
        // Apply compression
        String compressed = compress(context);
        estimatedTokens = compressed.length() / 4;
        
        if (estimatedTokens > maxTokens) {
            // Truncate to target tokens, accounting for suffix length
            String suffix = "...[truncated]";
            int targetChars = maxTokens * 4 - suffix.length();
            compressed = compressed.substring(0, Math.min(targetChars, compressed.length())) + suffix;
        }
        
        return compressed;
    }
    
    /**
     * Gets compression statistics.
     */
    public CompressionStats analyzeCompression(String original, String compressed) {
        int originalLength = original != null ? original.length() : 0;
        int compressedLength = compressed != null ? compressed.length() : 0;
        int savedChars = originalLength - compressedLength;
        double percentSaved = originalLength > 0 ? ((double) savedChars / originalLength) * 100 : 0;
        
        // Estimate token savings (rough: 4 chars per token)
        int savedTokens = savedChars / 4;
        
        return new CompressionStats(originalLength, compressedLength, savedChars, percentSaved, savedTokens);
    }
    
    /**
     * Compression statistics.
     */
    public static class CompressionStats {
        private final int originalLength;
        private final int compressedLength;
        private final int savedChars;
        private final double percentSaved;
        private final int estimatedTokensSaved;
        
        public CompressionStats(int originalLength, int compressedLength, int savedChars, 
                double percentSaved, int estimatedTokensSaved) {
            this.originalLength = originalLength;
            this.compressedLength = compressedLength;
            this.savedChars = savedChars;
            this.percentSaved = percentSaved;
            this.estimatedTokensSaved = estimatedTokensSaved;
        }
        
        public int getOriginalLength() {
            return originalLength;
        }
        
        public int getCompressedLength() {
            return compressedLength;
        }
        
        public int getSavedChars() {
            return savedChars;
        }
        
        public double getPercentSaved() {
            return percentSaved;
        }
        
        public int getEstimatedTokensSaved() {
            return estimatedTokensSaved;
        }
    }
}
