package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.AdviceHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.GeneratedMarkdownFile;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.WiseArchitectureResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.WiseArchitectureAdvice;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.WiseArchitectureAdviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing WISE Architecture advice history.
 * Stores and retrieves AI-generated solutions for review and follow-up.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WiseArchitectureHistoryService {

    private final WiseArchitectureAdviceRepository adviceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save an initial solution to history.
     */
    @Transactional
    public WiseArchitectureAdvice saveInitialSolution(
            Pitch pitch,
            Long userId,
            String conversationId,
            WiseArchitectureResponseDTO response,
            long processingTimeMs,
            boolean hasFigmaContext,
            boolean hasGitHubContext,
            boolean hasRoadmapContext) {
        
        String techStacks = response.getSolutions() != null ?
            response.getSolutions().keySet().stream()
                .map(TechStackType::name)
                .collect(Collectors.joining(",")) : "";

        String generatedFilesJson = serializeGeneratedFiles(response.getGeneratedFiles());

        WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
            .conversationId(conversationId)
            .pitch(pitch)
            .userId(userId)
            .messageType("INITIAL_SOLUTION")
            .userMessage("Initial solution request")
            .aiResponse(formatSolutionResponse(response))
            .techStacks(techStacks)
            .hasFigmaContext(hasFigmaContext)
            .hasGitHubContext(hasGitHubContext)
            .hasRoadmapContext(hasRoadmapContext)
            .processingTimeMs(processingTimeMs)
            .generatedFilesJson(generatedFilesJson)
            .build();

        WiseArchitectureAdvice saved = adviceRepository.save(advice);
        log.info("Saved initial solution advice {} for pitch {} user {}", 
            saved.getId(), pitch.getId(), userId);
        return saved;
    }

    /**
     * Save a follow-up question and answer to history.
     */
    @Transactional
    public WiseArchitectureAdvice saveFollowUp(
            Pitch pitch,
            Long userId,
            String conversationId,
            String question,
            String answer,
            long processingTimeMs) {
        
        WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
            .conversationId(conversationId)
            .pitch(pitch)
            .userId(userId)
            .messageType("FOLLOW_UP")
            .userMessage(question)
            .aiResponse(answer)
            .processingTimeMs(processingTimeMs)
            .build();

        WiseArchitectureAdvice saved = adviceRepository.save(advice);
        log.info("Saved follow-up advice {} for conversation {} user {}", 
            saved.getId(), conversationId, userId);
        return saved;
    }

    /**
     * Get all conversations for a user.
     */
    @Transactional(readOnly = true)
    public Page<AdviceHistoryDTO.ConversationSummary> getUserConversations(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WiseArchitectureAdvice> advicePage = adviceRepository.findConversationsByUserId(userId, pageable);
        
        // Batch load message counts to avoid N+1 queries
        Set<String> conversationIds = advicePage.getContent().stream()
            .map(WiseArchitectureAdvice::getConversationId)
            .collect(Collectors.toSet());
        
        Map<String, Long> messageCounts = getMessageCountsForConversations(conversationIds);
        
        return advicePage.map(advice -> toConversationSummary(advice, messageCounts));
    }

    /**
     * Get all conversations for a pitch.
     */
    @Transactional(readOnly = true)
    public List<AdviceHistoryDTO.ConversationSummary> getPitchConversations(Long pitchId) {
        List<WiseArchitectureAdvice> adviceList = adviceRepository.findConversationsByPitchId(pitchId);
        
        // Batch load message counts to avoid N+1 queries
        Set<String> conversationIds = adviceList.stream()
            .map(WiseArchitectureAdvice::getConversationId)
            .collect(Collectors.toSet());
        
        Map<String, Long> messageCounts = getMessageCountsForConversations(conversationIds);
        
        return adviceList.stream()
            .map(advice -> toConversationSummary(advice, messageCounts))
            .toList();
    }

    /**
     * Get message counts for multiple conversations in a single query.
     */
    private Map<String, Long> getMessageCountsForConversations(Set<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        return adviceRepository.countMessagesByConversationIds(conversationIds).stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));
    }

    /**
     * Get full conversation thread.
     */
    @Transactional(readOnly = true)
    public List<AdviceHistoryDTO> getConversation(String conversationId) {
        List<WiseArchitectureAdvice> messages = adviceRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (messages.isEmpty()) {
            throw new ResourceNotFoundException("Conversation not found: " + conversationId);
        }
        return messages.stream()
            .map(this::toDTO)
            .toList();
    }

    /**
     * Submit feedback for an advice entry.
     */
    @Transactional
    public AdviceHistoryDTO submitFeedback(Long adviceId, Long userId, Boolean helpful, String feedbackText) {
        WiseArchitectureAdvice advice = adviceRepository.findById(adviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Advice not found: " + adviceId));
        
        if (!advice.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot submit feedback for another user's advice");
        }

        advice.setFeedbackHelpful(helpful);
        advice.setFeedbackText(feedbackText);
        advice.setFeedbackAt(LocalDateTime.now());
        
        WiseArchitectureAdvice saved = adviceRepository.save(advice);
        log.info("Feedback submitted for advice {} by user {}: helpful={}", adviceId, userId, helpful);
        
        return toDTO(saved);
    }

    /**
     * Get a single advice entry.
     */
    @Transactional(readOnly = true)
    public AdviceHistoryDTO getAdvice(Long adviceId) {
        WiseArchitectureAdvice advice = adviceRepository.findById(adviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Advice not found: " + adviceId));
        return toDTO(advice);
    }

    /**
     * Get the generated Markdown files for an advice entry by database ID.
     * Returns an empty list for FOLLOW_UP messages or entries created before this feature.
     */
    @Transactional(readOnly = true)
    public List<GeneratedMarkdownFile> getGeneratedFiles(Long adviceId) {
        WiseArchitectureAdvice advice = adviceRepository.findById(adviceId)
            .orElseThrow(() -> new ResourceNotFoundException("Advice not found: " + adviceId));
        return deserializeGeneratedFiles(advice.getGeneratedFilesJson());
    }

    /**
     * Get the generated Markdown files for a conversation (by UUID string).
     * Finds the INITIAL_SOLUTION entry for the conversation and returns its files.
     * Returns an empty list if the conversation doesn't exist or has no files (pre-v0.9).
     */
    @Transactional(readOnly = true)
    public List<GeneratedMarkdownFile> getGeneratedFilesByConversationId(String conversationId) {
        return adviceRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
            .filter(a -> "INITIAL_SOLUTION".equals(a.getMessageType()))
            .findFirst()
            .map(a -> deserializeGeneratedFiles(a.getGeneratedFilesJson()))
            .orElse(List.of());
    }

    private AdviceHistoryDTO toDTO(WiseArchitectureAdvice advice) {
        return AdviceHistoryDTO.builder()
            .id(advice.getId())
            .conversationId(advice.getConversationId())
            .pitchId(advice.getPitch().getId())
            .pitchTitle(advice.getPitch().getTitle())
            .userId(advice.getUserId())
            .messageType(advice.getMessageType())
            .userMessage(advice.getUserMessage())
            .aiResponse(advice.getAiResponse())
            .techStacks(parseTechStacks(advice.getTechStacks()))
            .repositoryIds(parseRepoIds(advice.getRepositoryIds()))
            .hasFigmaContext(advice.getHasFigmaContext())
            .hasGitHubContext(advice.getHasGitHubContext())
            .hasRoadmapContext(advice.getHasRoadmapContext())
            .processingTimeMs(advice.getProcessingTimeMs())
            .feedbackHelpful(advice.getFeedbackHelpful())
            .feedbackText(advice.getFeedbackText())
            .feedbackAt(advice.getFeedbackAt())
            .createdAt(advice.getCreatedAt())
            .build();
    }

    private AdviceHistoryDTO.ConversationSummary toConversationSummary(
            WiseArchitectureAdvice advice, Map<String, Long> messageCounts) {
        long messageCount = messageCounts.getOrDefault(advice.getConversationId(), 1L);
        
        String responsePreview = advice.getAiResponse();
        if (responsePreview != null && responsePreview.length() > 200) {
            responsePreview = responsePreview.substring(0, 197) + "...";
        }

        // Get project name via cycle if available (already eagerly loaded via @EntityGraph)
        String projectName = null;
        if (advice.getPitch().getCycle() != null && advice.getPitch().getCycle().getProject() != null) {
            projectName = advice.getPitch().getCycle().getProject().getName();
        }

        return AdviceHistoryDTO.ConversationSummary.builder()
            .conversationId(advice.getConversationId())
            .pitchId(advice.getPitch().getId())
            .pitchTitle(advice.getPitch().getTitle())
            .projectName(projectName)
            .techStacks(parseTechStacks(advice.getTechStacks()))
            .messageCount((int) messageCount)
            .createdAt(advice.getCreatedAt())
            .lastMessageAt(advice.getCreatedAt()) // Will be same for INITIAL_SOLUTION
            .responsePreview(responsePreview)
            .build();
    }

    private List<String> parseTechStacks(String techStacks) {
        if (techStacks == null || techStacks.isBlank()) {
            return List.of();
        }
        return Arrays.asList(techStacks.split(","));
    }

    private List<Long> parseRepoIds(String repoIds) {
        if (repoIds == null || repoIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(repoIds.split(","))
            .filter(s -> !s.isBlank())
            .map(Long::parseLong)
            .toList();
    }

    private String formatSolutionResponse(WiseArchitectureResponseDTO response) {
        StringBuilder sb = new StringBuilder();
        
        if (response.getSolutions() != null) {
            for (var entry : response.getSolutions().entrySet()) {
                var stackSol = entry.getValue();
                sb.append("## ").append(entry.getKey().getDisplayName()).append("\n\n");
                
                // Architecture overview / detail
                if (stackSol != null && stackSol.getArchitectureDetail() != null) {
                    var detail = stackSol.getArchitectureDetail();
                    if (detail.getSummary() != null) {
                        sb.append(detail.getSummary()).append("\n\n");
                    }
                    // Components
                    if (detail.getComponents() != null && !detail.getComponents().isEmpty()) {
                        sb.append("### Components\n\n");
                        for (var comp : detail.getComponents()) {
                            sb.append("- **").append(comp.getName()).append("**: ").append(comp.getResponsibility());
                            if (comp.getInteractsWith() != null && !comp.getInteractsWith().isEmpty()) {
                                sb.append(" _(interacts with: ").append(String.join(", ", comp.getInteractsWith())).append(")");
                            }
                            sb.append("\n");
                        }
                        sb.append("\n");
                    }
                    // API Contracts
                    if (detail.getApiContracts() != null && !detail.getApiContracts().isEmpty()) {
                        sb.append("### API Contracts\n\n");
                        for (var api : detail.getApiContracts()) {
                            sb.append("- `").append(api.getMethod()).append(" ").append(api.getEndpoint()).append("`");
                            if (api.getDescription() != null) sb.append(" — ").append(api.getDescription());
                            if (api.getRequestShape() != null) sb.append("  \n  Request: `").append(api.getRequestShape()).append("`");
                            if (api.getResponseShape() != null) sb.append("  \n  Response: `").append(api.getResponseShape()).append("`");
                            sb.append("\n");
                        }
                        sb.append("\n");
                    }
                    // Data Model
                    if (detail.getDataModel() != null && !detail.getDataModel().isEmpty()) {
                        sb.append("### Data Model\n\n");
                        for (var entity : detail.getDataModel()) {
                            sb.append("- **").append(entity.getEntityName()).append("**");
                            if (entity.getFields() != null && !entity.getFields().isEmpty()) {
                                sb.append(": ").append(String.join(", ", entity.getFields()));
                            }
                            if (entity.getRelationships() != null && !entity.getRelationships().isEmpty()) {
                                sb.append(" (").append(String.join(", ", entity.getRelationships())).append(")");
                            }
                            sb.append("\n");
                        }
                        sb.append("\n");
                    }
                    // Config Changes
                    if (detail.getConfigChanges() != null && !detail.getConfigChanges().isEmpty()) {
                        sb.append("### Configuration Changes\n\n");
                        for (var cfg : detail.getConfigChanges()) {
                            sb.append("- `").append(cfg.getKey()).append("=").append(cfg.getValue()).append("`");
                            if (cfg.getDescription() != null) sb.append(" — ").append(cfg.getDescription());
                            sb.append("\n");
                        }
                        sb.append("\n");
                    }
                } else if (stackSol != null && stackSol.getArchitectureOverview() != null) {
                    sb.append(stackSol.getArchitectureOverview()).append("\n\n");
                }
                
                // Reusable Services
                if (stackSol != null && stackSol.getReusableServices() != null && !stackSol.getReusableServices().isEmpty()) {
                    sb.append("### Reusable Services\n\n");
                    for (var svc : stackSol.getReusableServices()) {
                        sb.append("- **").append(svc.getServiceName()).append("** (`").append(svc.getFilePath()).append("`)\n");
                        sb.append("  ").append(svc.getDescription()).append("\n");
                        if (svc.getHowToUse() != null) sb.append("  Usage: ").append(svc.getHowToUse()).append("\n");
                        if (svc.getImportStatement() != null) sb.append("  Import: `").append(svc.getImportStatement()).append("`\n");
                        if (svc.getMethodsToCall() != null && !svc.getMethodsToCall().isEmpty()) {
                            sb.append("  Methods: ").append(String.join(", ", svc.getMethodsToCall())).append("\n");
                        }
                    }
                    sb.append("\n");
                }
                
                // Recommended Libraries
                if (stackSol != null && stackSol.getRecommendedLibraries() != null && !stackSol.getRecommendedLibraries().isEmpty()) {
                    sb.append("### Recommended Libraries\n\n");
                    for (var lib : stackSol.getRecommendedLibraries()) {
                        sb.append("- **").append(lib.getName()).append("**");
                        if (lib.getVersion() != null) sb.append(" v").append(lib.getVersion());
                        sb.append(" — ").append(lib.getPurpose());
                        if (lib.getAlreadyInProject() != null && lib.getAlreadyInProject()) sb.append(" ✓ already in project");
                        if (lib.getDocumentationUrl() != null) sb.append(" [docs](").append(lib.getDocumentationUrl()).append(")");
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                
                // Implementation Steps
                if (stackSol != null && stackSol.getImplementationSteps() != null && !stackSol.getImplementationSteps().isEmpty()) {
                    sb.append("### Implementation Steps\n\n");
                    for (var step : stackSol.getImplementationSteps()) {
                        sb.append(step.getStepNumber()).append(". **").append(step.getTitle()).append("** (~").append(step.getEstimatedHours()).append("h)\n");
                        sb.append("   ").append(step.getDescription()).append("\n");
                        if (step.getFilesToCreate() != null && !step.getFilesToCreate().isEmpty()) {
                            sb.append("   Create: ").append(String.join(", ", step.getFilesToCreate())).append("\n");
                        }
                        if (step.getFilesToModify() != null && !step.getFilesToModify().isEmpty()) {
                            sb.append("   Modify: ").append(String.join(", ", step.getFilesToModify())).append("\n");
                        }
                        if (step.getMethodSignatures() != null && !step.getMethodSignatures().isEmpty()) {
                            sb.append("   Methods: `").append(String.join("`, `", step.getMethodSignatures())).append("`\n");
                        }
                        if (step.getSubTasks() != null && !step.getSubTasks().isEmpty()) {
                            for (var sub : step.getSubTasks()) {
                                sb.append("   - [ ] ").append(sub.getTask());
                                if (sub.getAcceptanceCriteria() != null) sb.append(" — _").append(sub.getAcceptanceCriteria()).append("_");
                                sb.append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                }
                
                // Risk Factors
                if (stackSol != null && stackSol.getRiskFactors() != null && !stackSol.getRiskFactors().isEmpty()) {
                    sb.append("### Risk Factors\n\n");
                    for (var risk : stackSol.getRiskFactors()) {
                        sb.append("- ⚠️ ").append(risk).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * Serialize generated Markdown files to JSON for DB storage.
     * Returns null if files is null or empty.
     */
    private String serializeGeneratedFiles(List<GeneratedMarkdownFile> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(files);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize generated files to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize generated Markdown files from JSON DB storage.
     * Returns an empty list on error or if json is null.
     */
    public List<GeneratedMarkdownFile> deserializeGeneratedFiles(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, GeneratedMarkdownFile.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize generated files JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
