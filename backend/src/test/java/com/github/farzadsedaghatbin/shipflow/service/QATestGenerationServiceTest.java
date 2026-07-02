package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.GenerateTestCasesResponse;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QATestGenerationServiceTest {

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private MeetingRepository meetingRepository;

  @Mock
  private ChatLanguageModel chatLanguageModel;

  @Mock
  private EmbeddingModel embeddingModel;

  private QATestGenerationService service;

  private Pitch testPitch;

  @BeforeEach
  void setUp() {
    testPitch = Pitch.builder().id(1L).title("Split Bill Feature").status(PitchStatus.IN_PROGRESS)
        .appetiteDays(14).build();

    // embeddingStore/promptBuilder/testCaseValidator are left null (as Spring would leave them
    // when no bean is configured) so buildTestGenerationPrompt() — the plain fallback prompt —
    // is exercised directly, independent of TestCasePromptBuilder. embeddingModel is mocked
    // (never stubbed to return anything, so retrieveRelevantKnowledge()'s embeddingStore==null
    // short-circuit fires first) purely to pass the "AI models are not available" guard.
    service = new QATestGenerationService(pitchRepository, meetingRepository, embeddingModel, null,
        chatLanguageModel, null, null);
    ReflectionTestUtils.setField(service, "testManagementEnabled", true);
    ReflectionTestUtils.setField(service, "aiTestGenerationEnabled", true);
  }

  @Test
  void generateTestCases_QATeamAdditionalContext_IsIncludedAndFlaggedMandatory() {
    when(pitchRepository.findById(anyLong())).thenReturn(Optional.of(testPitch));
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    when(chatLanguageModel.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
        ---TEST CASE---
        TITLE: Split bill evenly among participants
        DESCRIPTION: Verifies an even split
        PRECONDITIONS: User has an active group
        STEPS:
        1. Open the split screen
        2. Enter total amount
        EXPECTED: Each participant owes an equal share
        TYPE: FUNCTIONAL
        PRIORITY: HIGH
        TAGS: split, money
        ---END---
        """);

    GenerateTestCasesRequest request = GenerateTestCasesRequest.builder().pitchId(1L)
        .additionalContext("Must cover: splitting a bill with a non-divisible remainder (e.g. 10.01 / 3).")
        .build();

    GenerateTestCasesResponse response = service.generateTestCases(request);

    assertThat(response.getAiEnabled()).isTrue();
    assertThat(response.getContextUsed()).contains("QA TEAM REQUIREMENTS")
        .contains("non-divisible remainder");
  }

  @Test
  void parseTestCaseSuggestions_MultiLineExpectedResult_IsNotDroppedByFirstNewline() throws Exception {
    when(pitchRepository.findById(anyLong())).thenReturn(Optional.of(testPitch));
    when(meetingRepository.findByPitchId(anyLong())).thenReturn(Collections.emptyList());
    // The expected result starts on the line AFTER the "EXPECTED:" label — the field-boundary
    // parser must not truncate this to an empty string at the first newline.
    when(chatLanguageModel.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
        ---TEST CASE---
        TITLE: Split bill evenly among participants
        DESCRIPTION: Verifies an even split
        PRECONDITIONS: User has an active group
        STEPS:
        1. Open the split screen
        2. Enter total amount
        EXPECTED:
        Each participant owes an equal share.
        The total of all shares equals the original amount.
        TYPE: FUNCTIONAL
        PRIORITY: HIGH
        TAGS: split, money
        ---END---
        """);

    GenerateTestCasesRequest request = GenerateTestCasesRequest.builder().pitchId(1L).build();

    GenerateTestCasesResponse response = service.generateTestCases(request);

    List<GenerateTestCasesResponse.TestCaseSuggestion> suggestions = response.getSuggestions();
    assertThat(suggestions).hasSize(1);
    assertThat(suggestions.get(0).getExpectedResult()).isNotNull()
        .contains("Each participant owes an equal share")
        .contains("The total of all shares equals the original amount");
    assertThat(suggestions.get(0).getSuggestedType()).isEqualTo("FUNCTIONAL");
  }
}
