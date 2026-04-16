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
                testPitch, testStack, "// code context", List.of("ExistingService"), "Java, Spring Boot", "Design with 3 screens", "Epic: Mobile App", "", "", "");

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
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getStackType()).isEqualTo(TechStackType.BACKEND_JAVA);
            assertThat(result.getArchitectureOverview()).contains("AI model not available");
            assertThat(result.getRiskFactors()).contains("AI features not configured");
        }

        @Test
        @DisplayName("should handle LLM error gracefully with user-safe message")
        void shouldHandleLLMErrorGracefully() {
            when(chatLanguageModel.generate(anyString()))
                .thenThrow(new RuntimeException("API error"));

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getArchitectureOverview()).contains("Failed to parse AI response");
            assertThat(result.getRiskFactors()).anyMatch(r -> r.contains("Failed to parse AI response"));
            // Must NOT leak internal exception details
            assertThat(result.getArchitectureOverview()).doesNotContain("API error");
        }

        @Test
        @DisplayName("should retry and succeed when initial response is non-JSON markdown")
        void shouldRetryAndSucceedWhenInitialResponseIsMarkdown() {
            String markdownResponse = """
                # Solution Architecture
                
                ## Overview
                We propose a RESTful API with clean architecture...
                
                ### Components
                - TaskService: Manages tasks
                - TaskRepository: Data access
                """;

            String validJsonOnRetry = """
                {
                    "architectureOverview": "RESTful API from retry",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [
                        {"stepNumber": 1, "title": "Create API", "description": "Build endpoints", "estimatedHours": 4, "filesToCreate": ["TaskController.java"], "filesToModify": []}
                    ],
                    "riskFactors": ["Retry was needed"],
                    "bestPractices": []
                }
                """;

            // First call returns markdown, second call (retry) returns valid JSON
            when(chatLanguageModel.generate(anyString()))
                .thenReturn(markdownResponse)
                .thenReturn(validJsonOnRetry);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getArchitectureOverview()).isEqualTo("RESTful API from retry");
            assertThat(result.getRiskFactors()).contains("Retry was needed");
            assertThat(result.getEstimatedHours()).isEqualTo(4);
            // LLM should have been called twice: initial + retry
            verify(chatLanguageModel, times(2)).generate(anyString());
        }

        @Test
        @DisplayName("should retry and succeed when initial JSON is malformed")
        void shouldRetryAndSucceedWhenInitialJsonIsMalformed() {
            // Valid-looking JSON start but truncated/malformed
            String malformedJson = """
                {"architectureOverview": "Test", "reusableServices": [, BROKEN }
                """;

            String validJsonOnRetry = """
                {
                    "architectureOverview": "Fixed on retry",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [
                        {"stepNumber": 1, "title": "Setup", "description": "Init", "estimatedHours": 2, "filesToCreate": [], "filesToModify": []}
                    ],
                    "riskFactors": [],
                    "bestPractices": []
                }
                """;

            when(chatLanguageModel.generate(anyString()))
                .thenReturn(malformedJson)
                .thenReturn(validJsonOnRetry);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getArchitectureOverview()).isEqualTo("Fixed on retry");
            verify(chatLanguageModel, times(2)).generate(anyString());
        }

        @Test
        @DisplayName("should return error solution when both initial and retry fail")
        void shouldReturnErrorSolutionWhenBothAttemptsFail() {
            String markdownResponse = "# Not JSON at all\nJust markdown text.";

            // Both calls return non-JSON
            when(chatLanguageModel.generate(anyString()))
                .thenReturn(markdownResponse)
                .thenReturn("Still not valid JSON, sorry!");

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getArchitectureOverview()).contains("AI returned non-JSON response after retry");
            verify(chatLanguageModel, times(2)).generate(anyString());
        }

        @Test
        @DisplayName("retry prompt should include JSON schema")
        void retryPromptShouldIncludeJsonSchema() {
            String markdownResponse = "# Markdown response without JSON";
            String validJsonOnRetry = """
                {
                    "architectureOverview": "Schema-guided",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [],
                    "riskFactors": [],
                    "bestPractices": []
                }
                """;

            when(chatLanguageModel.generate(anyString()))
                .thenReturn(markdownResponse)
                .thenReturn(validJsonOnRetry);

            service.generateStackSolution(
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

            // Capture the retry prompt (second invocation)
            org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(chatLanguageModel, times(2)).generate(promptCaptor.capture());
            String retryPrompt = promptCaptor.getAllValues().get(1);
            // Retry prompt must contain the schema fields
            assertThat(retryPrompt).contains("architectureDetail");
            assertThat(retryPrompt).contains("implementationSteps");
            assertThat(retryPrompt).contains("riskFactors");
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
                testPitch, testStack, "", List.of(), null, null, null, null, null, null);

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

    @Nested
    @DisplayName("analyzeProjectConventions")
    class AnalyzeProjectConventions {

        @Test
        @DisplayName("should return empty string when LLM is not available")
        void shouldReturnEmptyWhenLLMNotAvailable() {
            ReflectionTestUtils.setField(service, "chatLanguageModel", null);

            String result = service.analyzeProjectConventions("public class UserService {}");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when codeContext is null")
        void shouldReturnEmptyWhenCodeContextIsNull() {
            String result = service.analyzeProjectConventions(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when codeContext is blank")
        void shouldReturnEmptyWhenCodeContextIsBlank() {
            String result = service.analyzeProjectConventions("   ");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should invoke LLM with a prompt containing the code context")
        void shouldInvokeLLMWithCodeContext() {
            String codeSnippet = "public class UserService { @Autowired UserRepository repo; }";
            String expectedConventions = "Uses @Service and @Autowired. Controller -> Service -> Repository layering.";
            when(chatLanguageModel.generate(anyString())).thenReturn(expectedConventions);

            String result = service.analyzeProjectConventions(codeSnippet);

            assertThat(result).isEqualTo(expectedConventions);
            // Verify that the code context was included in the LLM prompt
            org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(chatLanguageModel).generate(promptCaptor.capture());
            assertThat(promptCaptor.getValue()).contains(codeSnippet);
        }

        @Test
        @DisplayName("should return empty string and not throw when LLM throws an exception")
        void shouldReturnEmptyOnLLMException() {
            when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("LLM unavailable"));

            String result = service.analyzeProjectConventions("public class Foo {}");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse method resilience")
    class ParseMethodResilience {

        private static final String MOCK_MALFORMED_ARCH_DETAIL = """
            {
                "architectureOverview": "Test",
                "reusableServices": [],
                "recommendedLibraries": [],
                "implementationSteps": [],
                "architectureDetail": {
                    "summary": "Test summary",
                    "components": "not-a-list",
                    "apiContracts": "not-a-list",
                    "dataModel": "not-a-list",
                    "configChanges": "not-a-list"
                },
                "riskFactors": []
            }
            """;

        @Test
        @DisplayName("should return empty collections for components/apiContracts/dataModel/configChanges when LLM returns wrong type")
        void shouldReturnEmptyCollectionsOnMalformedArchitectureDetail() {
            when(chatLanguageModel.generate(anyString())).thenReturn(MOCK_MALFORMED_ARCH_DETAIL);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "code", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            // architectureOverview is derived from architectureDetail.summary (see parseSolutionResponse)
            assertThat(result.getArchitectureOverview()).isEqualTo("Test summary");
            // Malformed sub-fields inside architectureDetail must fall back to empty, not throw
            assertThat(result.getArchitectureDetail()).isNotNull();
            assertThat(result.getArchitectureDetail().getComponents()).isEmpty();
            assertThat(result.getArchitectureDetail().getApiContracts()).isEmpty();
            assertThat(result.getArchitectureDetail().getDataModel()).isEmpty();
            assertThat(result.getArchitectureDetail().getConfigChanges()).isEmpty();
        }

        @Test
        @DisplayName("should return empty subTasks when implementation step has non-list subTasks")
        void shouldReturnEmptySubTasksOnMalformedValue() {
            String malformedStepsResponse = """
                {
                    "architectureOverview": "Test",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [
                        {
                            "stepNumber": 1,
                            "title": "Step",
                            "description": "Desc",
                            "estimatedHours": 2,
                            "filesToCreate": [],
                            "filesToModify": [],
                            "subTasks": "should-be-a-list",
                            "methodSignatures": [],
                            "dependsOnSteps": []
                        }
                    ],
                    "riskFactors": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(malformedStepsResponse);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "code", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getImplementationSteps()).hasSize(1);
            // subTasks should fall back to empty rather than throw ClassCastException
            assertThat(result.getImplementationSteps().get(0).getSubTasks()).isEmpty();
        }

        @Test
        @DisplayName("should return empty dependsOnSteps when value is not a number list")
        void shouldReturnEmptyDependsOnStepsOnMalformedValue() {
            String malformedResponse = """
                {
                    "architectureOverview": "Test",
                    "reusableServices": [],
                    "recommendedLibraries": [],
                    "implementationSteps": [
                        {
                            "stepNumber": 1,
                            "title": "Step",
                            "description": "Desc",
                            "estimatedHours": 2,
                            "filesToCreate": [],
                            "filesToModify": [],
                            "subTasks": [],
                            "methodSignatures": [],
                            "dependsOnSteps": "not-a-number-list"
                        }
                    ],
                    "riskFactors": []
                }
                """;
            when(chatLanguageModel.generate(anyString())).thenReturn(malformedResponse);

            StackSolutionDTO result = service.generateStackSolution(
                testPitch, testStack, "code", List.of(), null, null, null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getImplementationSteps().get(0).getDependsOnSteps()).isEmpty();
        }
    }
}
