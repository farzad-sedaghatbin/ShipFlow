package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
@DisplayName("WiseArchitectureConversationService")
class WiseArchitectureConversationServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private WiseArchitectureConversationService service;
    private Pitch testPitch;
    private WiseArchitectureResponseDTO testSolution;

    @BeforeEach
    void setUp() {
        service = new WiseArchitectureConversationService();
        ReflectionTestUtils.setField(service, "chatLanguageModel", chatLanguageModel);

        testPitch = new Pitch();
        testPitch.setId(1L);
        testPitch.setTitle("Authentication Feature");
        testPitch.setProblemStatement("Users need secure login");
        testPitch.setSolution("Implement OAuth2 with MFA");

        testSolution = WiseArchitectureResponseDTO.builder()
            .pitchId(1L)
            .pitchName("Authentication Feature")
            .solutions(Map.of(
                TechStackType.BACKEND_JAVA, StackSolutionDTO.builder()
                    .stackType(TechStackType.BACKEND_JAVA)
                    .architectureOverview("Spring Security with JWT")
                    .estimatedHours(24)
                    .build()
            ))
            .build();
    }

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("should create session and return session ID")
        void shouldCreateSessionAndReturnId() {
            String sessionId = service.createSession(testPitch, testSolution);

            assertThat(sessionId).isNotNull();
            assertThat(sessionId).isNotEmpty();
            
            var session = service.getSession(sessionId);
            assertThat(session).isNotNull();
            assertThat(session.getPitchId()).isEqualTo(1L);
            assertThat(session.getPitchTitle()).isEqualTo("Authentication Feature");
        }
    }

    @Nested
    @DisplayName("handleFollowUp")
    class HandleFollowUp {

        @Test
        @DisplayName("should detect code request and generate Copilot prompt")
        void shouldDetectCodeRequestAndGenerateCopilotPrompt() {
            String sessionId = service.createSession(testPitch, testSolution);
            
            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId(sessionId)
                .question("Can you write the code for the JWT token service?")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getIsCodeRequest()).isTrue();
            assertThat(response.getCopilotPrompt()).isNotNull();
            assertThat(response.getCopilotPrompt()).contains("Authentication Feature");
            assertThat(response.getCopilotPrompt()).contains("JWT token service");
            assertThat(response.getAnswer()).contains("Code generation is outside this tool's scope");
        }

        @Test
        @DisplayName("should handle clarification question using LLM")
        void shouldHandleClarificationQuestionUsingLLM() {
            String sessionId = service.createSession(testPitch, testSolution);
            
            when(chatLanguageModel.generate(anyString()))
                .thenReturn("The JWT tokens are validated using the public key...");
            
            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId(sessionId)
                .question("How does the JWT validation work?")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getIsCodeRequest()).isFalse();
            assertThat(response.getCopilotPrompt()).isNull();
            assertThat(response.getAnswer()).contains("JWT tokens are validated");
            
            verify(chatLanguageModel).generate(anyString());
        }

        @Test
        @DisplayName("should return error for expired session")
        void shouldReturnErrorForExpiredSession() {
            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId("non-existent-session")
                .question("What about the security?")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getAnswer()).contains("Session expired");
            assertThat(response.getIsCodeRequest()).isFalse();
        }

        @Test
        @DisplayName("should detect various code request patterns")
        void shouldDetectVariousCodeRequestPatterns() {
            String sessionId = service.createSession(testPitch, testSolution);
            
            // Test different code request patterns
            String[] codeRequests = {
                "write code for the auth service",
                "implement the login function",
                "generate code for user validation",
                "create a token generator class",
                "write me a middleware for authentication",
                "can you code the refresh token logic"
            };

            for (String question : codeRequests) {
                FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                    .sessionId(sessionId)
                    .question(question)
                    .build();

                FollowUpResponseDTO response = service.handleFollowUp(request);

                assertThat(response.getIsCodeRequest())
                    .as("Should detect '%s' as code request", question)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("should not detect non-code questions as code requests")
        void shouldNotDetectNonCodeQuestionsAsCodeRequests() {
            String sessionId = service.createSession(testPitch, testSolution);
            when(chatLanguageModel.generate(anyString())).thenReturn("Here's the explanation...");
            
            String[] nonCodeQuestions = {
                "What is the purpose of this architecture?",
                "Can you explain the authentication flow?",
                "Why did you choose JWT?",
                "What are the risks involved?"
            };

            for (String question : nonCodeQuestions) {
                FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                    .sessionId(sessionId)
                    .question(question)
                    .build();

                FollowUpResponseDTO response = service.handleFollowUp(request);

                assertThat(response.getIsCodeRequest())
                    .as("Should NOT detect '%s' as code request", question)
                    .isFalse();
            }
        }

        @Test
        @DisplayName("should handle LLM not available for clarification")
        void shouldHandleLLMNotAvailableForClarification() {
            String sessionId = service.createSession(testPitch, testSolution);
            ReflectionTestUtils.setField(service, "chatLanguageModel", null);
            
            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId(sessionId)
                .question("Explain the architecture")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getAnswer()).contains("AI model not available");
        }

        @Test
        @DisplayName("should include context in Copilot prompt")
        void shouldIncludeContextInCopilotPrompt() {
            String sessionId = service.createSession(testPitch, testSolution);
            
            FollowUpQuestionDTO request = FollowUpQuestionDTO.builder()
                .sessionId(sessionId)
                .question("Write the code for password hashing")
                .build();

            FollowUpResponseDTO response = service.handleFollowUp(request);

            assertThat(response.getCopilotPrompt()).contains("Authentication Feature");
            assertThat(response.getCopilotPrompt()).contains("Users need secure login");
            assertThat(response.getCopilotPrompt()).contains("password hashing");
            assertThat(response.getCopilotPrompt()).contains("Backend (Java/Spring)");
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("should return session by ID")
        void shouldReturnSessionById() {
            String sessionId = service.createSession(testPitch, testSolution);

            var session = service.getSession(sessionId);

            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isEqualTo(sessionId);
            assertThat(session.getPitchTitle()).isEqualTo("Authentication Feature");
        }

        @Test
        @DisplayName("should return null for non-existent session")
        void shouldReturnNullForNonExistentSession() {
            var session = service.getSession("non-existent");

            assertThat(session).isNull();
        }

        @Test
        @DisplayName("should update last accessed time")
        void shouldUpdateLastAccessedTime() {
            String sessionId = service.createSession(testPitch, testSolution);
            var session1 = service.getSession(sessionId);
            long firstAccess = session1.getLastAccessedAt();

            // Wait a bit
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            var session2 = service.getSession(sessionId);

            assertThat(session2.getLastAccessedAt()).isGreaterThan(firstAccess);
        }
    }
}
