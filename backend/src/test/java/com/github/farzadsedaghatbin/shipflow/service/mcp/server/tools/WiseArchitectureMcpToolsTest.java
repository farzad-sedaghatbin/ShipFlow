package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.AdviceHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.DetectStacksResponseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.DetectedStackDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.GeneratedMarkdownFile;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.WiseArchitectureRequestDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.WiseArchitectureResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureHistoryService;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("WiseArchitectureMcpTools")
class WiseArchitectureMcpToolsTest {

    @Mock
    private WiseArchitectureService wiseArchitectureService;

    @Mock
    private WiseArchitectureHistoryService historyService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WiseArchitectureMcpTools tools;

    @Mock
    private Authentication auth;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(99L);
        testUser.setUsername("alice");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void givenAuthenticatedAs(User user) {
        when(auth.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private GeneratedMarkdownFile sampleFile(String filename, String category) {
        return GeneratedMarkdownFile.builder()
            .filename(filename)
            .title("Title of " + filename)
            .category(category)
            .stackType(TechStackType.BACKEND_JAVA)
            .content("# Content\nSome markdown")
            .build();
    }

    private AdviceHistoryDTO.ConversationSummary buildSummary(String convId, String pitchTitle) {
        return AdviceHistoryDTO.ConversationSummary.builder()
            .conversationId(convId)
            .pitchId(1L)
            .pitchTitle(pitchTitle)
            .techStacks(List.of("BACKEND_JAVA", "WEB_REACT"))
            .messageCount(3)
            .createdAt(LocalDateTime.of(2026, 4, 10, 12, 0))
            .build();
    }

    // ── Tool definitions ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("tool definitions")
    class ToolDefinitions {

        @Test
        @DisplayName("listAnalysesDefinition has correct name and required fields")
        void listAnalysesDefinitionIsValid() {
            Map<String, Object> def = WiseArchitectureMcpTools.listAnalysesDefinition();
            assertThat(def.get("name")).isEqualTo(WiseArchitectureMcpTools.TOOL_LIST_ANALYSES);
            assertThat(def.get("description").toString()).contains("wise_architecture_get_files");
            assertThat(def.get("inputSchema")).isNotNull();
        }

        @Test
        @DisplayName("getFilesDefinition has correct name and required conversationId")
        void getFilesDefinitionIsValid() {
            Map<String, Object> def = WiseArchitectureMcpTools.getFilesDefinition();
            assertThat(def.get("name")).isEqualTo(WiseArchitectureMcpTools.TOOL_GET_FILES);
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) def.get("inputSchema");
            assertThat(schema.get("required").toString()).contains("conversationId");
        }

        @Test
        @DisplayName("analyzeDefinition has correct name and required pitchId + repositoryIds")
        void analyzeDefinitionIsValid() {
            Map<String, Object> def = WiseArchitectureMcpTools.analyzeDefinition();
            assertThat(def.get("name")).isEqualTo(WiseArchitectureMcpTools.TOOL_ANALYZE);
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) def.get("inputSchema");
            assertThat(schema.get("required").toString())
                .contains("pitchId")
                .contains("repositoryIds");
        }
    }

    // ── listAnalyses ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAnalyses")
    class ListAnalyses {

        @Test
        @DisplayName("returns paged user analyses when no pitchId provided")
        void returnsPagedAnalysesForUser() {
            givenAuthenticatedAs(testUser);
            Page<AdviceHistoryDTO.ConversationSummary> page =
                new PageImpl<>(List.of(buildSummary("conv-1", "Pitch Alpha")));
            when(historyService.getUserConversations(99L, 0, 10)).thenReturn(page);

            List<Map<String, Object>> result = tools.listAnalyses(Map.of(), auth);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("conversationId")).isEqualTo("conv-1");
            assertThat(result.get(0).get("pitchTitle")).isEqualTo("Pitch Alpha");
            assertThat(result.get(0).get("messageCount")).isEqualTo(3);
            verify(historyService).getUserConversations(99L, 0, 10);
        }

        @Test
        @DisplayName("filters by pitchId when provided")
        void filtersByPitchId() {
            givenAuthenticatedAs(testUser);
            when(historyService.getPitchConversations(42L))
                .thenReturn(List.of(buildSummary("conv-pitch", "My Pitch")));

            List<Map<String, Object>> result =
                tools.listAnalyses(Map.of("pitchId", 42), auth);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("conversationId")).isEqualTo("conv-pitch");
            verify(historyService).getPitchConversations(42L);
            verify(historyService, never()).getUserConversations(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("respects custom page and size params")
        void respectsPageAndSize() {
            givenAuthenticatedAs(testUser);
            when(historyService.getUserConversations(99L, 2, 5))
                .thenReturn(new PageImpl<>(List.of()));

            tools.listAnalyses(Map.of("page", 2, "size", 5), auth);

            verify(historyService).getUserConversations(99L, 2, 5);
        }

        @Test
        @DisplayName("caps size at 25 even when larger value requested")
        void capsSizeAt25() {
            givenAuthenticatedAs(testUser);
            when(historyService.getUserConversations(99L, 0, 25))
                .thenReturn(new PageImpl<>(List.of()));

            tools.listAnalyses(Map.of("size", 100), auth);

            verify(historyService).getUserConversations(99L, 0, 25);
        }

        @Test
        @DisplayName("techStacks field is included in summary map")
        void includesTechStacks() {
            givenAuthenticatedAs(testUser);
            AdviceHistoryDTO.ConversationSummary summary = buildSummary("conv-x", "Stack Test");
            when(historyService.getUserConversations(99L, 0, 10))
                .thenReturn(new PageImpl<>(List.of(summary)));

            List<Map<String, Object>> result = tools.listAnalyses(Map.of(), auth);

            Object stacks = result.get(0).get("techStacks");
            assertThat(stacks).isInstanceOf(List.class);
            assertThat((List<String>) stacks).contains("BACKEND_JAVA", "WEB_REACT");
        }

        @Test
        @DisplayName("throws SecurityException when no authenticated user")
        void throwsWhenUnauthenticated() {
            when(auth.getName()).thenReturn(null);

            assertThatThrownBy(() -> tools.listAnalyses(Map.of(), auth))
                .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("throws SecurityException when user not found in DB")
        void throwsWhenUserNotFound() {
            when(auth.getName()).thenReturn("ghost");
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tools.listAnalyses(Map.of(), auth))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("ghost");
        }
    }

    // ── getFiles ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getFiles")
    class GetFiles {

        @Test
        @DisplayName("returns files for a valid conversationId")
        void returnsFilesForConversation() {
            List<GeneratedMarkdownFile> files = List.of(
                sampleFile("architecture-overview.md", "overview"),
                sampleFile("java-implementation-guide.md", "implementation")
            );
            when(historyService.getGeneratedFilesByConversationId("conv-abc")).thenReturn(files);

            List<Map<String, Object>> result =
                tools.getFiles(Map.of("conversationId", "conv-abc"));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("filename")).isEqualTo("architecture-overview.md");
            assertThat(result.get(0).get("category")).isEqualTo("overview");
            assertThat(result.get(0).get("content")).isEqualTo("# Content\nSome markdown");
            assertThat(result.get(1).get("filename")).isEqualTo("java-implementation-guide.md");
        }

        @Test
        @DisplayName("includes stackType in each file map")
        void includesStackType() {
            when(historyService.getGeneratedFilesByConversationId("conv-st"))
                .thenReturn(List.of(sampleFile("guide.md", "implementation")));

            List<Map<String, Object>> result =
                tools.getFiles(Map.of("conversationId", "conv-st"));

            assertThat(result.get(0).get("stackType")).isEqualTo("BACKEND_JAVA");
        }

        @Test
        @DisplayName("returns informational message when no files found")
        void returnsMessageWhenNoFiles() {
            when(historyService.getGeneratedFilesByConversationId("conv-empty"))
                .thenReturn(List.of());

            List<Map<String, Object>> result =
                tools.getFiles(Map.of("conversationId", "conv-empty"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("message").toString()).contains("No generated files");
        }

        @Test
        @DisplayName("throws when conversationId is missing")
        void throwsWhenConversationIdMissing() {
            assertThatThrownBy(() -> tools.getFiles(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversationId");
        }

        @Test
        @DisplayName("throws when conversationId is blank")
        void throwsWhenConversationIdBlank() {
            assertThatThrownBy(() -> tools.getFiles(Map.of("conversationId", "   ")))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("uses 'cross-stack' stackType for files with null stackType")
        void usesCrossStackWhenStackTypeNull() {
            GeneratedMarkdownFile crossStack = GeneratedMarkdownFile.builder()
                .filename("api-design.md")
                .title("API Design")
                .category("api")
                .stackType(null)
                .content("## APIs")
                .build();
            when(historyService.getGeneratedFilesByConversationId("conv-cs"))
                .thenReturn(List.of(crossStack));

            List<Map<String, Object>> result =
                tools.getFiles(Map.of("conversationId", "conv-cs"));

            assertThat(result.get(0).get("stackType")).isEqualTo("cross-stack");
        }
    }

    // ── analyze ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyze")
    class Analyze {

        private WiseArchitectureResponseDTO buildResponse(List<GeneratedMarkdownFile> files) {
            return WiseArchitectureResponseDTO.builder()
                .pitchId(1L)
                .pitchName("Test Pitch")
                .generatedFiles(files)
                .sessionId("session-abc")
                .build();
        }

        @Test
        @DisplayName("calls wiseArchitectureService.analyze with correct request and user")
        void callsServiceWithCorrectRequest() {
            givenAuthenticatedAs(testUser);
            List<GeneratedMarkdownFile> files = List.of(
                sampleFile("architecture-overview.md", "overview"));
            when(wiseArchitectureService.analyze(any(), eq(99L))).thenReturn(buildResponse(files));

            tools.analyze(Map.of(
                "pitchId", 1,
                "repositoryIds", List.of(10, 20),
                "selectedStacks", List.of("BACKEND_JAVA", "WEB_REACT")
            ), auth);

            ArgumentCaptor<WiseArchitectureRequestDTO> captor =
                ArgumentCaptor.forClass(WiseArchitectureRequestDTO.class);
            verify(wiseArchitectureService).analyze(captor.capture(), eq(99L));

            WiseArchitectureRequestDTO captured = captor.getValue();
            assertThat(captured.getPitchId()).isEqualTo(1L);
            assertThat(captured.getRepositoryIds()).containsExactly(10L, 20L);
            assertThat(captured.getSelectedStacks())
                .containsExactlyInAnyOrder(TechStackType.BACKEND_JAVA, TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("returns generated files as list of maps")
        void returnsGeneratedFiles() {
            givenAuthenticatedAs(testUser);
            List<GeneratedMarkdownFile> files = List.of(
                sampleFile("overview.md", "overview"),
                sampleFile("impl.md", "implementation")
            );
            when(wiseArchitectureService.analyze(any(), anyLong()))
                .thenReturn(buildResponse(files));

            List<Map<String, Object>> result = tools.analyze(Map.of(
                "pitchId", 1,
                "repositoryIds", List.of(5),
                "selectedStacks", List.of("BACKEND_JAVA")
            ), auth);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("filename")).isEqualTo("overview.md");
            assertThat(result.get(1).get("filename")).isEqualTo("impl.md");
        }

        @Test
        @DisplayName("auto-detects stacks when selectedStacks is omitted")
        void autoDetectsStacksWhenOmitted() {
            givenAuthenticatedAs(testUser);
            DetectStacksResponseDTO detected = DetectStacksResponseDTO.builder()
                .detectedStacks(List.of(
                    DetectedStackDTO.builder()
                        .stackType(TechStackType.BACKEND_JAVA)
                        .confidence(85)
                        .build()))
                .build();
            when(wiseArchitectureService.detectStacks(any())).thenReturn(detected);
            when(wiseArchitectureService.analyze(any(), anyLong()))
                .thenReturn(buildResponse(List.of(sampleFile("guide.md", "impl"))));

            tools.analyze(Map.of(
                "pitchId", 1,
                "repositoryIds", List.of(5)
                // no selectedStacks
            ), auth);

            ArgumentCaptor<WiseArchitectureRequestDTO> captor =
                ArgumentCaptor.forClass(WiseArchitectureRequestDTO.class);
            verify(wiseArchitectureService).analyze(captor.capture(), anyLong());
            assertThat(captor.getValue().getSelectedStacks())
                .containsExactly(TechStackType.BACKEND_JAVA);
        }

        @Test
        @DisplayName("falls back to all detected stacks when none meet confidence threshold")
        void fallsBackToAllStacksWhenBelowThreshold() {
            givenAuthenticatedAs(testUser);
            DetectStacksResponseDTO detected = DetectStacksResponseDTO.builder()
                .detectedStacks(List.of(
                    DetectedStackDTO.builder()
                        .stackType(TechStackType.WEB_REACT)
                        .confidence(30) // below threshold
                        .build()))
                .build();
            when(wiseArchitectureService.detectStacks(any())).thenReturn(detected);
            when(wiseArchitectureService.analyze(any(), anyLong()))
                .thenReturn(buildResponse(List.of(sampleFile("guide.md", "impl"))));

            tools.analyze(Map.of("pitchId", 1, "repositoryIds", List.of(5)), auth);

            ArgumentCaptor<WiseArchitectureRequestDTO> captor =
                ArgumentCaptor.forClass(WiseArchitectureRequestDTO.class);
            verify(wiseArchitectureService).analyze(captor.capture(), anyLong());
            // Falls back to WEB_REACT despite low confidence
            assertThat(captor.getValue().getSelectedStacks())
                .containsExactly(TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("throws when no stacks can be auto-detected")
        void throwsWhenNoStacksDetected() {
            givenAuthenticatedAs(testUser);
            when(wiseArchitectureService.detectStacks(any()))
                .thenReturn(DetectStacksResponseDTO.builder().detectedStacks(List.of()).build());

            assertThatThrownBy(() -> tools.analyze(
                Map.of("pitchId", 1, "repositoryIds", List.of(5)), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("auto-detect");
        }

        @Test
        @DisplayName("throws for an unknown stack type string")
        void throwsForUnknownStackType() {
            givenAuthenticatedAs(testUser);

            assertThatThrownBy(() -> tools.analyze(Map.of(
                "pitchId", 1,
                "repositoryIds", List.of(5),
                "selectedStacks", List.of("INVALID_STACK")
            ), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_STACK");
        }

        @Test
        @DisplayName("throws when pitchId is missing")
        void throwsWhenPitchIdMissing() {
            givenAuthenticatedAs(testUser);

            assertThatThrownBy(() -> tools.analyze(
                Map.of("repositoryIds", List.of(1)), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pitchId");
        }

        @Test
        @DisplayName("throws when repositoryIds is empty")
        void throwsWhenRepositoryIdsEmpty() {
            givenAuthenticatedAs(testUser);

            assertThatThrownBy(() -> tools.analyze(
                Map.of("pitchId", 1, "repositoryIds", List.of()), auth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repositoryIds");
        }

        @Test
        @DisplayName("returns informational message when analysis produces no files")
        void returnsMessageWhenNoFilesGenerated() {
            givenAuthenticatedAs(testUser);
            when(wiseArchitectureService.analyze(any(), anyLong()))
                .thenReturn(buildResponse(List.of()));

            List<Map<String, Object>> result = tools.analyze(Map.of(
                "pitchId", 1,
                "repositoryIds", List.of(5),
                "selectedStacks", List.of("BACKEND_JAVA")
            ), auth);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("message").toString())
                .contains("no Markdown files were generated");
        }

        @Test
        @DisplayName("accepts integer or long repositoryId values")
        void acceptsIntegerOrLongRepoIds() {
            givenAuthenticatedAs(testUser);
            when(wiseArchitectureService.analyze(any(), anyLong()))
                .thenReturn(buildResponse(List.of(sampleFile("f.md", "impl"))));

            // Should not throw
            tools.analyze(Map.of(
                "pitchId", 1L,          // Long
                "repositoryIds", List.of(10, 20L),  // mixed Integer / Long
                "selectedStacks", List.of("BACKEND_JAVA")
            ), auth);
        }
    }
}
