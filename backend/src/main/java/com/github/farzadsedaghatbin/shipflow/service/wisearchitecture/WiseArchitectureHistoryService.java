package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.AdviceHistoryDTO;
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
                sb.append("## ").append(entry.getKey().getDisplayName()).append("\n\n");
                if (entry.getValue() != null && entry.getValue().getArchitectureOverview() != null) {
                    sb.append(entry.getValue().getArchitectureOverview()).append("\n\n");
                }
            }
        }
        
        return sb.toString().trim();
    }
}
