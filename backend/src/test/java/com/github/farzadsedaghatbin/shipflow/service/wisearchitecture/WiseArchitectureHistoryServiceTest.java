package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.AdviceHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.StackSolutionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.WiseArchitectureResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.WiseArchitectureAdvice;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.WiseArchitectureAdviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("WiseArchitectureHistoryService")
class WiseArchitectureHistoryServiceTest {

    @Mock
    private WiseArchitectureAdviceRepository adviceRepository;

    @InjectMocks
    private WiseArchitectureHistoryService service;

    private Pitch testPitch;
    private WiseArchitectureResponseDTO testResponse;
    private WiseArchitectureAdvice testAdvice;

    @BeforeEach
    void setUp() {
        testPitch = new Pitch();
        testPitch.setId(1L);
        testPitch.setTitle("Test Feature");
        testPitch.setProblemStatement("Users need X");
        testPitch.setSolution("Build Y");
        testPitch.setAppetiteDays(5);

        StackSolutionDTO backendSolution = StackSolutionDTO.builder()
            .architectureOverview("Backend solution overview")
            .estimatedHours(40)
            .build();

        Map<TechStackType, StackSolutionDTO> solutions = new LinkedHashMap<>();
        solutions.put(TechStackType.BACKEND_JAVA, backendSolution);

        testResponse = WiseArchitectureResponseDTO.builder()
            .pitchId(1L)
            .pitchName("Test Feature")
            .solutions(solutions)
            .totalEstimatedHours(40)
            .generatedAt(LocalDateTime.now())
            .build();

        testAdvice = WiseArchitectureAdvice.builder()
            .id(1L)
            .conversationId("test-conv-123")
            .pitch(testPitch)
            .userId(100L)
            .messageType("INITIAL_SOLUTION")
            .userMessage("Initial solution request")
            .aiResponse("## Backend Spring\n\nBackend solution overview")
            .techStacks("BACKEND_SPRING")
            .hasFigmaContext(true)
            .hasGitHubContext(true)
            .hasRoadmapContext(false)
            .processingTimeMs(5000L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("saveInitialSolution")
    class SaveInitialSolution {

        @Test
        @DisplayName("should save initial solution with all parameters")
        void shouldSaveInitialSolutionWithAllParameters() {
            String conversationId = "conv-123";
            Long userId = 100L;
            
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> {
                    WiseArchitectureAdvice advice = inv.getArgument(0);
                    advice.setId(1L);
                    advice.setCreatedAt(LocalDateTime.now());
                    advice.setUpdatedAt(LocalDateTime.now());
                    return advice;
                });

            WiseArchitectureAdvice result = service.saveInitialSolution(
                testPitch, userId, conversationId, testResponse, 
                5000L, true, true, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            
            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            WiseArchitectureAdvice captured = captor.getValue();
            assertThat(captured.getConversationId()).isEqualTo(conversationId);
            assertThat(captured.getPitch()).isEqualTo(testPitch);
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getMessageType()).isEqualTo("INITIAL_SOLUTION");
            assertThat(captured.getTechStacks()).contains("BACKEND_JAVA");
            assertThat(captured.getHasFigmaContext()).isTrue();
            assertThat(captured.getHasGitHubContext()).isTrue();
            assertThat(captured.getHasRoadmapContext()).isFalse();
            assertThat(captured.getProcessingTimeMs()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("should format solution response properly")
        void shouldFormatSolutionResponseProperly() {
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            service.saveInitialSolution(
                testPitch, 100L, "conv-123", testResponse, 
                5000L, false, false, false);

            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            String aiResponse = captor.getValue().getAiResponse();
            assertThat(aiResponse).contains("Backend (Java/Spring)");
            assertThat(aiResponse).contains("Backend solution overview");
        }

        @Test
        @DisplayName("should handle empty solutions map")
        void shouldHandleEmptySolutionsMap() {
            WiseArchitectureResponseDTO emptyResponse = WiseArchitectureResponseDTO.builder()
                .pitchId(1L)
                .pitchName("Test")
                .solutions(new LinkedHashMap<>())
                .build();
            
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            service.saveInitialSolution(
                testPitch, 100L, "conv-123", emptyResponse, 
                1000L, false, false, false);

            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            assertThat(captor.getValue().getTechStacks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveFollowUp")
    class SaveFollowUp {

        @Test
        @DisplayName("should save follow-up question and answer")
        void shouldSaveFollowUpQuestionAndAnswer() {
            String conversationId = "conv-123";
            Long userId = 100L;
            String question = "How should I implement the authentication?";
            String answer = "Use JWT tokens with refresh mechanism...";
            
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> {
                    WiseArchitectureAdvice advice = inv.getArgument(0);
                    advice.setId(2L);
                    advice.setCreatedAt(LocalDateTime.now());
                    advice.setUpdatedAt(LocalDateTime.now());
                    return advice;
                });

            WiseArchitectureAdvice result = service.saveFollowUp(
                testPitch, userId, conversationId, question, answer, 2000L);

            assertThat(result).isNotNull();
            
            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            WiseArchitectureAdvice captured = captor.getValue();
            assertThat(captured.getConversationId()).isEqualTo(conversationId);
            assertThat(captured.getMessageType()).isEqualTo("FOLLOW_UP");
            assertThat(captured.getUserMessage()).isEqualTo(question);
            assertThat(captured.getAiResponse()).isEqualTo(answer);
            assertThat(captured.getProcessingTimeMs()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("should not set context flags for follow-ups")
        void shouldNotSetContextFlagsForFollowUps() {
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            service.saveFollowUp(testPitch, 100L, "conv-123", "question", "answer", 1000L);

            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            WiseArchitectureAdvice captured = captor.getValue();
            // Builder defaults initialize these to false, not null
            assertThat(captured.getHasFigmaContext()).isFalse();
            assertThat(captured.getHasGitHubContext()).isFalse();
            assertThat(captured.getHasRoadmapContext()).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserConversations")
    class GetUserConversations {

        @Test
        @DisplayName("should return paginated conversations for user")
        void shouldReturnPaginatedConversationsForUser() {
            Long userId = 100L;
            List<WiseArchitectureAdvice> adviceList = List.of(testAdvice);
            Page<WiseArchitectureAdvice> advicePage = new PageImpl<>(adviceList, PageRequest.of(0, 10), 1);
            
            when(adviceRepository.findConversationsByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(advicePage);
            when(adviceRepository.countMessagesByConversationIds(anySet()))
                .thenReturn(Collections.singletonList(new Object[]{"test-conv-123", 3L}));

            Page<AdviceHistoryDTO.ConversationSummary> result = service.getUserConversations(userId, 0, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            
            AdviceHistoryDTO.ConversationSummary summary = result.getContent().get(0);
            assertThat(summary.getConversationId()).isEqualTo("test-conv-123");
            assertThat(summary.getPitchTitle()).isEqualTo("Test Feature");
            assertThat(summary.getMessageCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty page when no conversations exist")
        void shouldReturnEmptyPageWhenNoConversationsExist() {
            Long userId = 999L;
            Page<WiseArchitectureAdvice> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            
            when(adviceRepository.findConversationsByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

            Page<AdviceHistoryDTO.ConversationSummary> result = service.getUserConversations(userId, 0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should truncate long response previews")
        void shouldTruncateLongResponsePreviews() {
            String longResponse = "A".repeat(250);
            WiseArchitectureAdvice adviceWithLongResponse = WiseArchitectureAdvice.builder()
                .id(1L)
                .conversationId("conv-long")
                .pitch(testPitch)
                .userId(100L)
                .messageType("INITIAL_SOLUTION")
                .aiResponse(longResponse)
                .techStacks("BACKEND_SPRING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            Page<WiseArchitectureAdvice> advicePage = new PageImpl<>(
                List.of(adviceWithLongResponse), PageRequest.of(0, 10), 1);
            
            when(adviceRepository.findConversationsByUserId(eq(100L), any(Pageable.class)))
                .thenReturn(advicePage);
            when(adviceRepository.countMessagesByConversationIds(anySet()))
                .thenReturn(Collections.singletonList(new Object[]{"conv-long", 1L}));

            Page<AdviceHistoryDTO.ConversationSummary> result = service.getUserConversations(100L, 0, 10);

            AdviceHistoryDTO.ConversationSummary summary = result.getContent().get(0);
            assertThat(summary.getResponsePreview()).hasSize(200);
            assertThat(summary.getResponsePreview()).endsWith("...");
        }

        @Test
        @DisplayName("should include project name when available")
        void shouldIncludeProjectNameWhenAvailable() {
            Project project = new Project();
            project.setId(1L);
            project.setName("Mobile App Project");
            
            Cycle cycle = new Cycle();
            cycle.setId(1L);
            cycle.setProject(project);
            
            Pitch pitchWithCycle = new Pitch();
            pitchWithCycle.setId(2L);
            pitchWithCycle.setTitle("Mobile Feature");
            pitchWithCycle.setCycle(cycle);
            
            WiseArchitectureAdvice adviceWithProject = WiseArchitectureAdvice.builder()
                .id(2L)
                .conversationId("conv-proj")
                .pitch(pitchWithCycle)
                .userId(100L)
                .messageType("INITIAL_SOLUTION")
                .aiResponse("Solution content")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            Page<WiseArchitectureAdvice> advicePage = new PageImpl<>(
                List.of(adviceWithProject), PageRequest.of(0, 10), 1);
            
            when(adviceRepository.findConversationsByUserId(eq(100L), any(Pageable.class)))
                .thenReturn(advicePage);
            when(adviceRepository.countMessagesByConversationIds(anySet()))
                .thenReturn(Collections.singletonList(new Object[]{"conv-proj", 1L}));

            Page<AdviceHistoryDTO.ConversationSummary> result = service.getUserConversations(100L, 0, 10);

            assertThat(result.getContent().get(0).getProjectName()).isEqualTo("Mobile App Project");
        }
    }

    @Nested
    @DisplayName("getConversation")
    class GetConversation {

        @Test
        @DisplayName("should return all messages in conversation ordered by time")
        void shouldReturnAllMessagesInConversationOrderedByTime() {
            String conversationId = "conv-123";
            WiseArchitectureAdvice followUp = WiseArchitectureAdvice.builder()
                .id(2L)
                .conversationId(conversationId)
                .pitch(testPitch)
                .userId(100L)
                .messageType("FOLLOW_UP")
                .userMessage("Follow-up question")
                .aiResponse("Follow-up answer")
                .createdAt(LocalDateTime.now().plusMinutes(5))
                .updatedAt(LocalDateTime.now().plusMinutes(5))
                .build();
            
            when(adviceRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(testAdvice, followUp));

            List<AdviceHistoryDTO> result = service.getConversation(conversationId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMessageType()).isEqualTo("INITIAL_SOLUTION");
            assertThat(result.get(1).getMessageType()).isEqualTo("FOLLOW_UP");
        }

        @Test
        @DisplayName("should throw when conversation not found")
        void shouldThrowWhenConversationNotFound() {
            String conversationId = "non-existent";
            when(adviceRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of());

            assertThatThrownBy(() -> service.getConversation(conversationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conversation not found: " + conversationId);
        }
    }

    @Nested
    @DisplayName("submitFeedback")
    class SubmitFeedback {

        @Test
        @DisplayName("should submit positive feedback for own advice")
        void shouldSubmitPositiveFeedbackForOwnAdvice() {
            Long adviceId = 1L;
            Long userId = 100L;
            
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.of(testAdvice));
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AdviceHistoryDTO result = service.submitFeedback(adviceId, userId, true, "Very helpful!");

            assertThat(result).isNotNull();
            
            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            WiseArchitectureAdvice saved = captor.getValue();
            assertThat(saved.getFeedbackHelpful()).isTrue();
            assertThat(saved.getFeedbackText()).isEqualTo("Very helpful!");
            assertThat(saved.getFeedbackAt()).isNotNull();
        }

        @Test
        @DisplayName("should submit negative feedback with text")
        void shouldSubmitNegativeFeedbackWithText() {
            Long adviceId = 1L;
            Long userId = 100L;
            
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.of(testAdvice));
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AdviceHistoryDTO result = service.submitFeedback(adviceId, userId, false, "Didn't address my needs");

            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            assertThat(captor.getValue().getFeedbackHelpful()).isFalse();
            assertThat(captor.getValue().getFeedbackText()).isEqualTo("Didn't address my needs");
        }

        @Test
        @DisplayName("should reject feedback from different user")
        void shouldRejectFeedbackFromDifferentUser() {
            Long adviceId = 1L;
            Long differentUserId = 999L;
            
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.of(testAdvice));

            assertThatThrownBy(() -> service.submitFeedback(adviceId, differentUserId, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot submit feedback for another user's advice");
            
            verify(adviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when advice not found")
        void shouldThrowWhenAdviceNotFound() {
            Long adviceId = 999L;
            Long userId = 100L;
            
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitFeedback(adviceId, userId, true, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Advice not found: " + adviceId);
        }

        @Test
        @DisplayName("should allow feedback without text")
        void shouldAllowFeedbackWithoutText() {
            Long adviceId = 1L;
            Long userId = 100L;
            
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.of(testAdvice));
            when(adviceRepository.save(any(WiseArchitectureAdvice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AdviceHistoryDTO result = service.submitFeedback(adviceId, userId, true, null);

            ArgumentCaptor<WiseArchitectureAdvice> captor = ArgumentCaptor.forClass(WiseArchitectureAdvice.class);
            verify(adviceRepository).save(captor.capture());
            
            assertThat(captor.getValue().getFeedbackHelpful()).isTrue();
            assertThat(captor.getValue().getFeedbackText()).isNull();
        }
    }

    @Nested
    @DisplayName("getAdvice")
    class GetAdvice {

        @Test
        @DisplayName("should return advice by ID")
        void shouldReturnAdviceById() {
            Long adviceId = 1L;
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.of(testAdvice));

            AdviceHistoryDTO result = service.getAdvice(adviceId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(adviceId);
            assertThat(result.getConversationId()).isEqualTo("test-conv-123");
            assertThat(result.getPitchTitle()).isEqualTo("Test Feature");
            assertThat(result.getMessageType()).isEqualTo("INITIAL_SOLUTION");
        }

        @Test
        @DisplayName("should throw when advice not found")
        void shouldThrowWhenAdviceNotFound() {
            Long adviceId = 999L;
            when(adviceRepository.findById(adviceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAdvice(adviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Advice not found: " + adviceId);
        }
    }

    @Nested
    @DisplayName("getPitchConversations")
    class GetPitchConversations {

        @Test
        @DisplayName("should return all conversations for a pitch")
        void shouldReturnAllConversationsForPitch() {
            Long pitchId = 1L;
            
            when(adviceRepository.findConversationsByPitchId(pitchId))
                .thenReturn(List.of(testAdvice));
            when(adviceRepository.countMessagesByConversationIds(anySet()))
                .thenReturn(Collections.singletonList(new Object[]{"test-conv-123", 2L}));

            List<AdviceHistoryDTO.ConversationSummary> result = service.getPitchConversations(pitchId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConversationId()).isEqualTo("test-conv-123");
            assertThat(result.get(0).getMessageCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when no conversations for pitch")
        void shouldReturnEmptyListWhenNoConversationsForPitch() {
            Long pitchId = 999L;
            when(adviceRepository.findConversationsByPitchId(pitchId))
                .thenReturn(List.of());

            List<AdviceHistoryDTO.ConversationSummary> result = service.getPitchConversations(pitchId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseTechStacks and parseRepoIds")
    class ParseMethods {

        @Test
        @DisplayName("should parse tech stacks correctly")
        void shouldParseTechStacksCorrectly() {
            WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
                .id(1L)
                .conversationId("conv-1")
                .pitch(testPitch)
                .userId(100L)
                .messageType("INITIAL_SOLUTION")
                .aiResponse("Response")
                .techStacks("BACKEND_SPRING,WEB_REACT,MOBILE_FLUTTER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(advice));

            AdviceHistoryDTO result = service.getAdvice(1L);

            assertThat(result.getTechStacks()).containsExactly("BACKEND_SPRING", "WEB_REACT", "MOBILE_FLUTTER");
        }

        @Test
        @DisplayName("should handle null tech stacks")
        void shouldHandleNullTechStacks() {
            WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
                .id(1L)
                .conversationId("conv-1")
                .pitch(testPitch)
                .userId(100L)
                .messageType("FOLLOW_UP")
                .aiResponse("Response")
                .techStacks(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(advice));

            AdviceHistoryDTO result = service.getAdvice(1L);

            assertThat(result.getTechStacks()).isEmpty();
        }

        @Test
        @DisplayName("should parse repository IDs correctly")
        void shouldParseRepositoryIdsCorrectly() {
            WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
                .id(1L)
                .conversationId("conv-1")
                .pitch(testPitch)
                .userId(100L)
                .messageType("INITIAL_SOLUTION")
                .aiResponse("Response")
                .repositoryIds("1,2,3")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(advice));

            AdviceHistoryDTO result = service.getAdvice(1L);

            assertThat(result.getRepositoryIds()).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("should handle blank repository IDs")
        void shouldHandleBlankRepositoryIds() {
            WiseArchitectureAdvice advice = WiseArchitectureAdvice.builder()
                .id(1L)
                .conversationId("conv-1")
                .pitch(testPitch)
                .userId(100L)
                .messageType("INITIAL_SOLUTION")
                .aiResponse("Response")
                .repositoryIds("  ")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(advice));

            AdviceHistoryDTO result = service.getAdvice(1L);

            assertThat(result.getRepositoryIds()).isEmpty();
        }
    }
}
