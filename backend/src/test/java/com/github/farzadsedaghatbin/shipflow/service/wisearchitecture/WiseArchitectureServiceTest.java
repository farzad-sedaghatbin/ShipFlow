package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubRepositoryRepository;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WiseArchitectureService")
class WiseArchitectureServiceTest {

    @Mock
    private OrganizationSettingsService settingsService;

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private GitHubRepositoryRepository repositoryRepository;

    @Mock
    private TechStackDetectorService techStackDetectorService;

    @Mock
    private TechnicalSolutionGeneratorService solutionGeneratorService;

    @Mock
    private WiseArchitectureConversationService conversationService;

    @InjectMocks
    private WiseArchitectureService service;

    private OrganizationSettingsDTO enabledSettings;
    private OrganizationSettingsDTO disabledSettings;
    private Pitch testPitch;
    private GitHubRepository testRepo;

    @BeforeEach
    void setUp() {
        enabledSettings = OrganizationSettingsDTO.builder()
            .enableWiseArchitecture(true)
            .enableAIFeatures(true)
            .build();

        disabledSettings = OrganizationSettingsDTO.builder()
            .enableWiseArchitecture(false)
            .enableAIFeatures(true)
            .build();

        testPitch = new Pitch();
        testPitch.setId(1L);
        testPitch.setTitle("Test Feature");
        testPitch.setProblemStatement("Users need X");
        testPitch.setSolution("Build Y");
        testPitch.setAppetiteDays(5);

        testRepo = GitHubRepository.builder()
            .id(1L)
            .owner("test")
            .name("repo")
            .fullName("test/repo")
            .build();
    }

    @Nested
    @DisplayName("checkFeatureEnabled")
    class CheckFeatureEnabled {

        @Test
        @DisplayName("should not throw when feature is enabled")
        void shouldNotThrowWhenFeatureEnabled() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);

            service.checkFeatureEnabled(); // Should not throw

            verify(settingsService).getSettings();
        }

        @Test
        @DisplayName("should throw when Wise Architecture is disabled")
        void shouldThrowWhenWiseArchitectureDisabled() {
            when(settingsService.getSettings()).thenReturn(disabledSettings);

            assertThatThrownBy(() -> service.checkFeatureEnabled())
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("Wise Architecture feature is not enabled");
        }

        @Test
        @DisplayName("should throw when AI features are disabled")
        void shouldThrowWhenAIFeaturesDisabled() {
            OrganizationSettingsDTO aiDisabled = OrganizationSettingsDTO.builder()
                .enableWiseArchitecture(true)
                .enableAIFeatures(false)
                .build();
            when(settingsService.getSettings()).thenReturn(aiDisabled);

            assertThatThrownBy(() -> service.checkFeatureEnabled())
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("AI features are disabled");
        }
    }

    @Nested
    @DisplayName("detectStacks")
    class DetectStacks {

        @Test
        @DisplayName("should detect stacks in repositories")
        void shouldDetectStacksInRepositories() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
            when(techStackDetectorService.detectStacks(eq(testRepo), anyList()))
                .thenReturn(List.of(
                    DetectedStackDTO.builder()
                        .stackType(TechStackType.BACKEND_JAVA)
                        .confidence(90)
                        .repositoryId(1L)
                        .build()
                ));

            DetectStacksRequestDTO request = DetectStacksRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .build();

            DetectStacksResponseDTO response = service.detectStacks(request);

            assertThat(response).isNotNull();
            assertThat(response.getPitchId()).isEqualTo(1L);
            assertThat(response.getPitchName()).isEqualTo("Test Feature");
            assertThat(response.getDetectedStacks()).hasSize(1);
            assertThat(response.getDetectedStacks().get(0).getStackType()).isEqualTo(TechStackType.BACKEND_JAVA);
            assertThat(response.getRepositoriesScanned()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw when pitch not found")
        void shouldThrowWhenPitchNotFound() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.empty());

            DetectStacksRequestDTO request = DetectStacksRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .build();

            assertThatThrownBy(() -> service.detectStacks(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pitch not found");
        }

        @Test
        @DisplayName("should throw when feature is disabled")
        void shouldThrowWhenFeatureDisabled() {
            when(settingsService.getSettings()).thenReturn(disabledSettings);

            DetectStacksRequestDTO request = DetectStacksRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .build();

            assertThatThrownBy(() -> service.detectStacks(request))
                .isInstanceOf(FeatureDisabledException.class);
        }

        @Test
        @DisplayName("should return empty stacks when no repositories found")
        void shouldReturnEmptyWhenNoRepositoriesFound() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(anyLong())).thenReturn(Optional.empty());

            DetectStacksRequestDTO request = DetectStacksRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(999L))
                .build();

            DetectStacksResponseDTO response = service.detectStacks(request);

            assertThat(response.getDetectedStacks()).isEmpty();
            assertThat(response.getRepositoriesScanned()).isEqualTo(0);
            assertThat(response.getMessage()).contains("No valid repositories");
        }
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("should generate solution for selected stacks")
        void shouldGenerateSolutionForSelectedStacks() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            lenient().when(settingsService.getFigmaAccessToken()).thenReturn(null);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
            when(solutionGeneratorService.generateStackSolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .architectureOverview("Spring Boot REST API")
                    .estimatedHours(24)
                    .build());
            when(conversationService.createSession(any(), any())).thenReturn("session-123");

            WiseArchitectureRequestDTO request = WiseArchitectureRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .selectedStacks(List.of(TechStackType.BACKEND_JAVA))
                .build();

            WiseArchitectureResponseDTO response = service.analyze(request);

            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo("session-123");
            assertThat(response.getPitchId()).isEqualTo(1L);
            assertThat(response.getSolutions()).containsKey(TechStackType.BACKEND_JAVA);
            assertThat(response.getTotalEstimatedHours()).isEqualTo(24);
            
            verify(solutionGeneratorService).generateStackSolution(any(), any(), any(), any(), any(), any());
            verify(conversationService).createSession(eq(testPitch), any());
        }

        @Test
        @DisplayName("should pass appetite check when hours are within budget")
        void shouldPassAppetiteCheckWhenWithinBudget() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            lenient().when(settingsService.getFigmaAccessToken()).thenReturn(null);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
            when(solutionGeneratorService.generateStackSolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .estimatedHours(32) // 5 days * 8 hours = 40, so 32 is within budget
                    .build());            when(conversationService.createSession(any(), any())).thenReturn("session-123");            when(conversationService.createSession(any(), any())).thenReturn("session-123");

            WiseArchitectureRequestDTO request = WiseArchitectureRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .selectedStacks(List.of(TechStackType.BACKEND_JAVA))
                .build();

            WiseArchitectureResponseDTO response = service.analyze(request);

            assertThat(response.getAppetiteCheck().getPassed()).isTrue();
            assertThat(response.getAppetiteCheck().getAvailableHours()).isEqualTo(40);
            assertThat(response.getAppetiteCheck().getEstimatedHours()).isEqualTo(32);
            assertThat(response.getReducedScope()).isNull();
        }

        @Test
        @DisplayName("should fail appetite check and generate reduced scope when over budget")
        void shouldFailAppetiteCheckWhenOverBudget() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            lenient().when(settingsService.getFigmaAccessToken()).thenReturn(null);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
            when(solutionGeneratorService.generateStackSolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .estimatedHours(56) // Over 40 hour budget
                    .build());
            when(solutionGeneratorService.generateReducedScope(any(), any(), anyInt(), anyInt()))
                .thenReturn(WiseArchitectureResponseDTO.ReducedScopeDTO.builder()
                    .explanation("Reduced to MVP")
                    .reducedTotalHours(40)
                    .build());
            when(conversationService.createSession(any(), any())).thenReturn("session-123");

            WiseArchitectureRequestDTO request = WiseArchitectureRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .selectedStacks(List.of(TechStackType.BACKEND_JAVA))
                .build();

            WiseArchitectureResponseDTO response = service.analyze(request);

            assertThat(response.getAppetiteCheck().getPassed()).isFalse();
            assertThat(response.getReducedScope()).isNotNull();
            assertThat(response.getReducedScope().getExplanation()).isEqualTo("Reduced to MVP");
            
            verify(solutionGeneratorService).generateReducedScope(any(), any(), eq(40), eq(56));
        }

        @Test
        @DisplayName("should generate solutions for multiple stacks")
        void shouldGenerateSolutionsForMultipleStacks() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            lenient().when(settingsService.getFigmaAccessToken()).thenReturn(null);
            when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
            when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepo));
            when(solutionGeneratorService.generateStackSolution(any(), any(), any(), any(), any(), any()))
                .thenReturn(StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .estimatedHours(16)
                    .build())
                .thenReturn(StackSolutionDTO.builder()
                    .stackType(TechStackType.WEB_REACT)
                    .estimatedHours(12)
                    .build());
            when(conversationService.createSession(any(), any())).thenReturn("session-123");

            WiseArchitectureRequestDTO request = WiseArchitectureRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .selectedStacks(List.of(TechStackType.BACKEND_JAVA, TechStackType.WEB_REACT))
                .build();

            WiseArchitectureResponseDTO response = service.analyze(request);

            assertThat(response.getSolutions()).hasSize(2);
            assertThat(response.getTotalEstimatedHours()).isEqualTo(28);
        }

        @Test
        @DisplayName("should throw when feature is disabled")
        void shouldThrowWhenFeatureDisabled() {
            when(settingsService.getSettings()).thenReturn(disabledSettings);

            WiseArchitectureRequestDTO request = WiseArchitectureRequestDTO.builder()
                .pitchId(1L)
                .repositoryIds(List.of(1L))
                .selectedStacks(List.of(TechStackType.BACKEND_JAVA))
                .build();

            assertThatThrownBy(() -> service.analyze(request))
                .isInstanceOf(FeatureDisabledException.class);
        }
    }

    @Nested
    @DisplayName("handleFollowUp")
    class HandleFollowUp {

        @Test
        @DisplayName("should delegate to conversation service")
        void shouldDelegateToConversationService() {
            when(settingsService.getSettings()).thenReturn(enabledSettings);
            when(conversationService.handleFollowUp(any()))
                .thenReturn(FollowUpResponseDTO.builder()
                    .answer("Here's the answer")
                    .isCodeRequest(false)
                    .build());

            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId("session-123")
                .question("Explain the architecture")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getAnswer()).isEqualTo("Here's the answer");
            
            verify(conversationService).handleFollowUp(request);
        }

        @Test
        @DisplayName("should throw when feature is disabled")
        void shouldThrowWhenFeatureDisabled() {
            when(settingsService.getSettings()).thenReturn(disabledSettings);

            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId("session-123")
                .question("Any question")
                .build();

            assertThatThrownBy(() -> service.handleFollowUp(request))
                .isInstanceOf(FeatureDisabledException.class);
        }
    }
}
