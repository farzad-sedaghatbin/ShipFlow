package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for handling follow-up conversations about generated solutions.
 * Maintains session context and generates Copilot prompts for code requests.
 */
@Service
@Slf4j
public class WiseArchitectureConversationService {

    private static final long SESSION_EXPIRY_MINUTES = 60;
    private static final int MAX_CONVERSATION_HISTORY = 10;

    // In-memory session storage (consider Redis for production)
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    /**
     * Create a new conversation session after solution generation.
     */
    public String createSession(Pitch pitch, WiseArchitectureResponseDTO solution) {
        String sessionId = UUID.randomUUID().toString();
        
        ConversationSession session = ConversationSession.builder()
            .sessionId(sessionId)
            .pitchId(pitch.getId())
            .pitchTitle(pitch.getTitle())
            .pitchProblem(pitch.getProblemStatement())
            .pitchSolution(pitch.getSolution())
            .generatedSolution(solution)
            .conversationHistory(new ArrayList<>())
            .createdAt(System.currentTimeMillis())
            .lastAccessedAt(System.currentTimeMillis())
            .build();
        
        sessions.put(sessionId, session);
        log.info("Created conversation session {} for pitch '{}'", sessionId, pitch.getTitle());
        
        return sessionId;
    }

    /**
     * Handle a follow-up question from the user.
     */
    public FollowUpResponseDTO handleFollowUp(FollowUpQuestionDTO request) {
        ConversationSession session = sessions.get(request.getSessionId());
        
        if (session == null) {
            return FollowUpResponseDTO.builder()
                .answer("Session expired or not found. Please generate a new solution to start a conversation.")
                .isCodeRequest(false)
                .sessionId(request.getSessionId())
                .build();
        }
        
        session.setLastAccessedAt(System.currentTimeMillis());
        
        String question = request.getQuestion().trim();
        boolean isCodeRequest = detectCodeRequest(question);
        
        if (isCodeRequest) {
            return handleCodeRequest(session, question);
        } else {
            return handleClarificationQuestion(session, question);
        }
    }

    /**
     * Get session by ID.
     */
    public ConversationSession getSession(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessedAt(System.currentTimeMillis());
        }
        return session;
    }

    /**
     * Check if a question is asking for code implementation.
     */
    private boolean detectCodeRequest(String question) {
        String lower = question.toLowerCase();
        
        // Keywords that indicate a code request
        List<String> codeKeywords = List.of(
            "write code", "write the code", "generate code", "create code",
            "implement", "implementation", "code for", "code to",
            "show me the code", "give me code", "write a function",
            "write a method", "write a class", "create a", "build a",
            "write me", "can you code", "code this", "implement this"
        );
        
        for (String keyword : codeKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Handle a code implementation request by generating a Copilot prompt.
     */
    private FollowUpResponseDTO handleCodeRequest(ConversationSession session, String question) {
        log.info("Detected code request in session {}: {}", session.getSessionId(), question);
        
        // Add to conversation history
        session.getConversationHistory().add(new ConversationMessage("user", question));
        trimConversationHistory(session);
        
        // Generate Copilot prompt
        String copilotPrompt = generateCopilotPrompt(session, question);
        
        String answer = "Code generation is outside this tool's scope. " +
            "However, I've prepared a detailed prompt you can use with your Copilot assistant or other AI coding tools. " +
            "Just copy the prompt below and paste it in your IDE or chat.";
        
        session.getConversationHistory().add(new ConversationMessage("assistant", answer));
        
        return FollowUpResponseDTO.builder()
            .answer(answer)
            .isCodeRequest(true)
            .copilotPrompt(copilotPrompt)
            .sessionId(session.getSessionId())
            .build();
    }

    /**
     * Handle a clarification question about the solution.
     */
    private FollowUpResponseDTO handleClarificationQuestion(ConversationSession session, String question) {
        log.info("Processing clarification question in session {}: {}", session.getSessionId(), question);
        
        // Add to conversation history
        session.getConversationHistory().add(new ConversationMessage("user", question));
        trimConversationHistory(session);
        
        if (chatLanguageModel == null) {
            String answer = "AI model not available. Please configure an LLM provider to enable follow-up conversations.";
            return FollowUpResponseDTO.builder()
                .answer(answer)
                .isCodeRequest(false)
                .sessionId(session.getSessionId())
                .build();
        }
        
        // Build context-aware prompt
        String prompt = buildClarificationPrompt(session, question);
        
        try {
            String answer = chatLanguageModel.generate(prompt);
            session.getConversationHistory().add(new ConversationMessage("assistant", answer));
            
            return FollowUpResponseDTO.builder()
                .answer(answer)
                .isCodeRequest(false)
                .sessionId(session.getSessionId())
                .build();
                
        } catch (Exception e) {
            log.error("Error generating clarification answer: {}", e.getMessage(), e);
            return FollowUpResponseDTO.builder()
                .answer("Sorry, I encountered an error processing your question. Please try again.")
                .isCodeRequest(false)
                .sessionId(session.getSessionId())
                .build();
        }
    }

    /**
     * Generate a ready-to-use prompt for Copilot or other AI coding assistants.
     */
    private String generateCopilotPrompt(ConversationSession session, String userRequest) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("I'm implementing a feature for \"").append(session.getPitchTitle()).append("\".\n\n");
        
        prompt.append("## Context\n");
        prompt.append("**Problem Being Solved:** ").append(nullSafe(session.getPitchProblem())).append("\n");
        prompt.append("**Proposed Solution:** ").append(nullSafe(session.getPitchSolution())).append("\n\n");
        
        // Add relevant stack information
        WiseArchitectureResponseDTO solution = session.getGeneratedSolution();
        if (solution != null && solution.getSolutions() != null && !solution.getSolutions().isEmpty()) {
            prompt.append("## Technology Stacks\n");
            for (Map.Entry<TechStackType, StackSolutionDTO> entry : solution.getSolutions().entrySet()) {
                prompt.append("- **").append(entry.getKey().getDisplayName()).append("**\n");
                
                // Add architecture overview
                if (entry.getValue().getArchitectureOverview() != null) {
                    prompt.append("  Architecture: ").append(truncate(entry.getValue().getArchitectureOverview(), 200)).append("\n");
                }
                
                // Add reusable services
                if (entry.getValue().getReusableServices() != null && !entry.getValue().getReusableServices().isEmpty()) {
                    prompt.append("  Existing services to reuse:\n");
                    for (StackSolutionDTO.ReusableServiceDTO service : entry.getValue().getReusableServices()) {
                        prompt.append("    - ").append(service.getServiceName());
                        if (service.getFilePath() != null) {
                            prompt.append(" (").append(service.getFilePath()).append(")");
                        }
                        prompt.append("\n");
                    }
                }
                
                // Add recommended libraries
                if (entry.getValue().getRecommendedLibraries() != null && !entry.getValue().getRecommendedLibraries().isEmpty()) {
                    prompt.append("  Libraries to use:\n");
                    for (StackSolutionDTO.RecommendedLibraryDTO lib : entry.getValue().getRecommendedLibraries()) {
                        prompt.append("    - ").append(lib.getName());
                        if (lib.getVersion() != null) {
                            prompt.append(" (").append(lib.getVersion()).append(")");
                        }
                        prompt.append(": ").append(lib.getPurpose()).append("\n");
                    }
                }
            }
            prompt.append("\n");
        }
        
        prompt.append("## What I Need\n");
        prompt.append(userRequest).append("\n\n");
        
        prompt.append("## Requirements\n");
        prompt.append("1. Follow best practices for the technology stack\n");
        prompt.append("2. Reuse existing services where applicable\n");
        prompt.append("3. Include proper error handling\n");
        prompt.append("4. Add appropriate comments and documentation\n");
        prompt.append("5. Follow the existing code patterns in the project\n\n");
        
        prompt.append("Please implement this following the context above.");
        
        return prompt.toString();
    }

    /**
     * Build a prompt for answering clarification questions.
     */
    private String buildClarificationPrompt(ConversationSession session, String question) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful software architect assistant. ");
        prompt.append("Answer the user's question based on the technical solution you previously generated.\n\n");
        
        prompt.append("## Pitch Context\n");
        prompt.append("Title: ").append(session.getPitchTitle()).append("\n");
        prompt.append("Problem: ").append(nullSafe(session.getPitchProblem())).append("\n\n");
        
        // Add solution summary with structured detail
        WiseArchitectureResponseDTO solution = session.getGeneratedSolution();
        if (solution != null && solution.getSolutions() != null) {
            prompt.append("## Generated Solution Summary\n");
            for (Map.Entry<TechStackType, StackSolutionDTO> entry : solution.getSolutions().entrySet()) {
                StackSolutionDTO stackSol = entry.getValue();
                prompt.append("### ").append(entry.getKey().getDisplayName()).append("\n");
                
                // Include structured detail if available
                if (stackSol.getArchitectureDetail() != null) {
                    StackSolutionDTO.ArchitectureDetailDTO detail = stackSol.getArchitectureDetail();
                    prompt.append("Architecture: ").append(nullSafe(detail.getSummary())).append("\n\n");
                    
                    if (detail.getComponents() != null && !detail.getComponents().isEmpty()) {
                        prompt.append("Components:\n");
                        for (StackSolutionDTO.ComponentDTO comp : detail.getComponents()) {
                            prompt.append("- ").append(comp.getName()).append(": ").append(comp.getResponsibility()).append("\n");
                        }
                    }
                    if (detail.getApiContracts() != null && !detail.getApiContracts().isEmpty()) {
                        prompt.append("API Contracts:\n");
                        for (StackSolutionDTO.ApiContractDTO api : detail.getApiContracts()) {
                            prompt.append("- ").append(api.getMethod()).append(" ").append(api.getEndpoint());
                            if (api.getDescription() != null) prompt.append(" — ").append(api.getDescription());
                            prompt.append("\n");
                        }
                    }
                    if (detail.getDataModel() != null && !detail.getDataModel().isEmpty()) {
                        prompt.append("Data Model:\n");
                        for (StackSolutionDTO.DataModelDTO entity : detail.getDataModel()) {
                            prompt.append("- ").append(entity.getEntityName());
                            if (entity.getFields() != null) prompt.append(" (").append(String.join(", ", entity.getFields())).append(")");
                            prompt.append("\n");
                        }
                    }
                } else {
                    prompt.append("Architecture: ").append(truncate(stackSol.getArchitectureOverview(), 500)).append("\n");
                }
                
                if (stackSol.getImplementationSteps() != null) {
                    prompt.append("Steps:\n");
                    for (StackSolutionDTO.ImplementationStepDTO step : stackSol.getImplementationSteps()) {
                        prompt.append("- ").append(step.getTitle()).append(" (").append(step.getEstimatedHours()).append("h)");
                        if (step.getMethodSignatures() != null && !step.getMethodSignatures().isEmpty()) {
                            prompt.append(" [").append(String.join(", ", step.getMethodSignatures())).append("]");
                        }
                        prompt.append("\n");
                    }
                }
                
                if (stackSol.getReusableServices() != null && !stackSol.getReusableServices().isEmpty()) {
                    prompt.append("Reusable Services:\n");
                    for (StackSolutionDTO.ReusableServiceDTO svc : stackSol.getReusableServices()) {
                        prompt.append("- ").append(svc.getServiceName()).append(" (").append(svc.getFilePath()).append(")\n");
                    }
                }
                prompt.append("\n");
            }
        }
        
        // Add conversation history
        if (!session.getConversationHistory().isEmpty()) {
            prompt.append("## Previous Conversation\n");
            for (ConversationMessage msg : session.getConversationHistory()) {
                prompt.append(msg.getRole().equals("user") ? "User: " : "Assistant: ");
                prompt.append(truncate(msg.getContent(), 200)).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("## Current Question\n");
        prompt.append(question).append("\n\n");
        
        prompt.append("Provide a helpful, concise answer. ");
        prompt.append("If the user asks for code implementation, politely explain that code generation ");
        prompt.append("is outside this tool's scope and suggest they ask for a Copilot prompt instead.");
        
        return prompt.toString();
    }

    private void trimConversationHistory(ConversationSession session) {
        while (session.getConversationHistory().size() > MAX_CONVERSATION_HISTORY) {
            session.getConversationHistory().remove(0);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String nullSafe(String value) {
        return value != null ? value : "Not specified";
    }

    /**
     * Clean up expired sessions periodically.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        long expiryTime = TimeUnit.MINUTES.toMillis(SESSION_EXPIRY_MINUTES);
        
        sessions.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastAccessedAt()) > expiryTime;
            if (expired) {
                log.debug("Removing expired session: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Session data class for storing conversation context.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationSession {
        private String sessionId;
        private Long pitchId;
        private String pitchTitle;
        private String pitchProblem;
        private String pitchSolution;
        private WiseArchitectureResponseDTO generatedSolution;
        private List<ConversationMessage> conversationHistory;
        private long createdAt;
        private long lastAccessedAt;
    }

    /**
     * Message in conversation history.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private String role; // "user" or "assistant"
        private String content;
    }
}
