package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.mcp.McpTestCaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.BulkCreateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.TestCaseService;
import com.github.farzadsedaghatbin.shipflow.service.TestRunService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class TestCaseMcpToolsTest {

  @Mock private TestCaseService testCaseService;
  @Mock private TestRunService testRunService;
  @Mock private UserRepository userRepository;
  @Mock private Authentication auth;

  private TestCaseMcpTools tools;

  @BeforeEach
  void setUp() {
    tools = new TestCaseMcpTools(testCaseService, testRunService, userRepository);
  }

  private void callerIs(long id) {
    User u = new User();
    u.setId(id);
    when(auth.getName()).thenReturn("farzad");
    when(userRepository.findByUsernameWithPerson("farzad")).thenReturn(Optional.of(u));
  }

  private static TestCaseDTO dto(long id, TestCaseStatus status) {
    return TestCaseDTO.builder().id(id).testCaseKey("TC-" + id).title("t" + id).status(status).build();
  }

  @Test
  void createTestCase_mapsAllFieldsAndUsesCaller() {
    callerIs(7L);
    when(testCaseService.createTestCase(any(), eq(7L))).thenReturn(dto(1, TestCaseStatus.DRAFT));

    McpTestCaseDTO out =
        tools.createTestCase(
            Map.of(
                "title", "Login works", "type", "e2e", "priority", "high",
                "steps", "open page", "expectedResult", "logged in",
                "tags", List.of("auth", "smoke"), "taskId", 8, "estimatedMinutes", 5),
            auth);

    assertThat(out.getTestCaseKey()).isEqualTo("TC-1");
    ArgumentCaptor<CreateTestCaseRequest> cap = ArgumentCaptor.forClass(CreateTestCaseRequest.class);
    verify(testCaseService).createTestCase(cap.capture(), eq(7L));
    CreateTestCaseRequest r = cap.getValue();
    assertThat(r.getTitle()).isEqualTo("Login works");
    assertThat(r.getType()).isEqualTo(TestCaseType.E2E);
    assertThat(r.getPriority()).isEqualTo(TestCasePriority.HIGH);
    assertThat(r.getTags()).containsExactly("auth", "smoke");
    assertThat(r.getTaskId()).isEqualTo(8L);
    assertThat(r.getEstimatedMinutes()).isEqualTo(5);
  }

  @Test
  void createTestCase_rejectsMissingOrInvalidFields() {
    assertThatThrownBy(() -> tools.createTestCase(Map.of("type", "UNIT", "priority", "LOW"), auth))
        .hasMessageContaining("title");
    assertThatThrownBy(() -> tools.createTestCase(Map.of("title", "x", "priority", "LOW"), auth))
        .hasMessageContaining("'type' and 'priority'");
    assertThatThrownBy(
            () -> tools.createTestCase(Map.of("title", "x", "type", "NOPE", "priority", "LOW"), auth))
        .hasMessageContaining("Invalid type 'NOPE'")
        .hasMessageContaining("FUNCTIONAL");
  }

  @Test
  void createTestCases_isOneAtomicBatch() {
    callerIs(7L);
    when(testCaseService.createTestCasesBulk(any(), eq(7L)))
        .thenReturn(List.of(dto(1, TestCaseStatus.DRAFT), dto(2, TestCaseStatus.DRAFT)));

    List<McpTestCaseDTO> out =
        tools.createTestCases(
            Map.of(
                "testCases",
                List.of(
                    Map.of("title", "a", "type", "UNIT", "priority", "LOW"),
                    Map.of("title", "b", "type", "SMOKE", "priority", "CRITICAL"))),
            auth);

    assertThat(out).hasSize(2);
    ArgumentCaptor<BulkCreateTestCaseRequest> cap =
        ArgumentCaptor.forClass(BulkCreateTestCaseRequest.class);
    verify(testCaseService).createTestCasesBulk(cap.capture(), eq(7L));
    assertThat(cap.getValue().getTestCases()).extracting(CreateTestCaseRequest::getTitle)
        .containsExactly("a", "b");
  }

  @Test
  void createTestCases_rejectsEmptyOrNonObjectItems() {
    assertThatThrownBy(() -> tools.createTestCases(Map.of("testCases", List.of()), auth))
        .hasMessageContaining("non-empty");
    assertThatThrownBy(() -> tools.createTestCases(Map.of("testCases", List.of("x")), auth))
        .hasMessageContaining("must be an object");
  }

  @Test
  void updateTestCase_sendsOnlyProvidedFields() {
    callerIs(7L);
    when(testCaseService.updateTestCase(eq(3L), any(), eq(7L)))
        .thenReturn(dto(3, TestCaseStatus.APPROVED));

    McpTestCaseDTO out =
        tools.updateTestCase(Map.of("testCaseId", 3, "status", "approved"), auth);

    assertThat(out.getStatus()).isEqualTo("APPROVED");
    ArgumentCaptor<UpdateTestCaseRequest> cap = ArgumentCaptor.forClass(UpdateTestCaseRequest.class);
    verify(testCaseService).updateTestCase(eq(3L), cap.capture(), eq(7L));
    UpdateTestCaseRequest r = cap.getValue();
    assertThat(r.getStatus()).isEqualTo(TestCaseStatus.APPROVED);
    assertThat(r.getTitle()).isNull();
    assertThat(r.getType()).isNull();
    assertThat(r.getTags()).isNull();
  }

  @Test
  void deleteTestCase_delegatesToSoftDelete() {
    Map<String, Object> out = tools.deleteTestCase(Map.of("testCaseId", 4));
    verify(testCaseService).deleteTestCase(4L);
    assertThat(out).containsEntry("deleted", true).containsEntry("testCaseId", 4L);
  }

  @Test
  void getTestCases_byProject_filtersByStatus() {
    when(testCaseService.getTestCasesWithFilters(eq(9L), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of(dto(1, TestCaseStatus.DRAFT), dto(2, TestCaseStatus.APPROVED)));

    assertThat(tools.getTestCases(Map.of("projectId", 9, "status", "APPROVED")))
        .extracting(McpTestCaseDTO::getId)
        .containsExactly(2L);
    assertThat(tools.getTestCases(Map.of("projectId", 9))).hasSize(2);
  }

  @Test
  void getTestCases_requiresAScope() {
    assertThatThrownBy(() -> tools.getTestCases(Map.of("status", "DRAFT")))
        .hasMessageContaining("projectId");
  }
}
