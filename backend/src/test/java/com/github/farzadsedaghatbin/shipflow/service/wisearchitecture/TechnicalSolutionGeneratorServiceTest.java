package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicalSolutionGeneratorService")
class TechnicalSolutionGeneratorServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private TechnicalSolutionGeneratorService service;
    private ObjectMapper objectMapper;
    private Pitch testPitch;
    private DetectedStackDTO testStack;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TechnicalSolutionGeneratorService(objectMapper);
        ReflectionTestUtils.setField(service, "chatLanguageModel", chatLanguageModel);

        testPitch = new Pitch();
        testPitch.setId(1L);
        testPitch.setTitle("Test Feature");
        testPitch.setProblemStatement("Users need a better way to manage tasks");
        testPitch.setSolution("Implement a drag-and-drop task board");
        testPitch.setRabbitHoles("Don't over-engineer the drag logic");
        testPitch.setNoGos("No real-time collaboration in this cycle");
        testPitch.setAppetiteDays(5);

        testStack = DetectedStackDTO.builder()
            .stackType(TechStackType.BACKEND_JAVA)
            .confidence(90)
            .primaryLanguage("Java")
            .framework("Spring Boot")
            .repositoryId(1L)
            .repositoryName("test/repo")
            .build();
    }

    @Nested
    @DisplayName("generateStackSolution")
    class GenerateStackSolution {

        @Test
        @DisplayName("should generate solution when LLM is available")
        void shouldGenerateSolutionWhenLLMAvailable() {
            String mockResponse = """
                {
                    "architectureOverview": "RESTful API with service layer",
                    "reusableServices": [
                        {"serviceName": "UserService", "filePath": "src/main/java/service/UserService.java", "description": "Handles user operations", "howToUse": "Inject via constructor"}
                    ],
                    "recommendedLibraries": [
                        {"name": "MapStruct", "version": "1.5.5", "purpose": "DTO mapping", "documentationUrl": "https://mapstruct.org", "alreadyInProject": false}
                    ],
                    "implementationSteps": [
                        {"stepNumber": 1, "title": "Create DTOs", "description": "Define request/response DTOs", "estimatedHours": 2, "filesToCreate": ["TaskDTO.java"], "filesToModify": []}
                    ],
                    "riskFactors": ["Complex drag-and-drop logic"]
                }
                """;

            when(chatLanguageModel.generate(anyString())).thenReturn(mockResponse);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "// code context", List.of("ExistingService"), "Java, Spring Boot", "Design with 3 screens", "Epic: Mobile App");

            assertThat(result).isNotNull();
            assertThat(result.getStackType()).isEqualTo(TechStackType.BACKEND_JAVA);
            assertThat(result.getArchitectureOverview()).isEqualTo("RESTful API with service layer");
            assertThat(result.getReusableServices()).hasSize(1);
            assertThat(result.getRecommendedLibraries()).hasSize(1);
            assertThat(result.getImplementationSteps()).hasSize(1);
            assertThat(result.getRiskFactors()).contains("Complex drag-and-drop logic");
            assertThat(result.getEstimatedHours()).isEqualTo(2);

            verify(chatLanguageModel).generate(anyString());
        }

        @Test
        @DisplayName("should return placeholder when LLM is not available")
        void shouldReturnPlaceholderWhenLLMNotAvailable() {
            ReflectionTestUtils.setField(service, "chatLanguageModel", null);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getStackType()).isEqualTo(TechStackType.BACKEND_JAVA);
            assertThat(result.getArchitectureOverview()).contains("AI model not available");
            assertThat(result.getRiskFactors()).contains("AI features not configured");
        }

        @Test
        @DisplayName("should handle LLM error gracefully")
        void shouldHandleLLMErrorGracefully() {
            when(chatLanguageModel.generate(anyString()))
                .thenThrow(new RuntimeException("API error"));

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getArchitectureOverview()).contains("Error generating solution");
            assertThat(result.getRiskFactors()).anyMatch(r -> r.contains("failed"));
        }

        @Test
        @DisplayName("should extract JSON from markdown code block")
        void shouldExtractJsonFromMarkdownCodeBlock() {
            String mockResponse = """
                Here's the solution:
                
                ```json
                {
                    "architectureOverview": "Clean Architecture",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [
                        {"stepNumber": 1, "title": "Setup", "description": "Initial setup", "estimatedHours": 1, "filesToCreate": [], "filesToModify": []}
                    ],
                    "riskFactors": []
                }
                ```
                
                This follows best practices.
                """;

            when(chatLanguageModel.generate(anyString())).thenReturn(mockResponse);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null);

            assertThat(result.getArchitectureOverview()).isEqualTo("Clean Architecture");
        }
    }

    @Nested
    @DisplayName("generateReducedScope")
    class GenerateReducedScope {

        @Test
        @DisplayName("should generate reduced scope when appetite exceeded")
        void shouldGenerateReducedScopeWhenAppetiteExceeded() {
            String mockResponse = """
                {
                    "explanation": "Focusing on MVP features only",
                    "reducedSteps": [
                        {"stepNumber": 1, "title": "Core API", "description": "Basic CRUD operations", "estimatedHours": 16, "filesToCreate": [], "filesToModify": []}
                    ],
                    "deferredItems": ["Advanced filtering", "Export feature"],
                    "reducedTotalHours": 32
                }
                """;

            when(chatLanguageModel.generate(anyString())).thenReturn(mockResponse);

            Map<TechStackType, StackSolutionDTO> solutions = Map.of(
                TechStackType.BACKEND_JAVA, StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .estimatedHours(48)
                    .implementationSteps(List.of(
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(1).title("Setup").estimatedHours(8).build(),
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(2).title("API").estimatedHours(16).build(),
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(3).title("Advanced").estimatedHours(24).build()
                    ))
                    .build()
            );

            WiseArchitectureResponseDTO.ReducedScopeDTO result = service.generateReducedScope(
                testPitch, solutions, 32, 48);

            assertThat(result).isNotNull();
            assertThat(result.getExplanation()).isEqualTo("Focusing on MVP features only");
            assertThat(result.getDeferredItems()).contains("Advanced filtering");
            assertThat(result.getReducedTotalHours()).isEqualTo(32);
        }

        @Test
        @DisplayName("should return placeholder when LLM not available")
        void shouldReturnPlaceholderWhenLLMNotAvailable() {
            ReflectionTestUtils.setField(service, "chatLanguageModel", null);

            Map<TechStackType, StackSolutionDTO> solutions = Map.of(
                TechStackType.BACKEND_JAVA, StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .estimatedHours(48)
                    .implementationSteps(List.of(
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(1).title("Step 1").estimatedHours(16).build(),
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(2).title("Step 2").estimatedHours(16).build(),
                        StackSolutionDTO.ImplementationStepDTO.builder()
                            .stepNumber(3).title("Step 3").estimatedHours(16).build()
                    ))
                    .build()
            );

            WiseArchitectureResponseDTO.ReducedScopeDTO result = service.generateReducedScope(
                testPitch, solutions, 32, 48);

            assertThat(result).isNotNull();
            assertThat(result.getExplanation()).contains("AI model not available");
            assertThat(result.getReducedTotalHours()).isEqualTo(32);
        }
    }
}
