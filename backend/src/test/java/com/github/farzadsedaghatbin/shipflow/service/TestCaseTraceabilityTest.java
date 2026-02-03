package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.TestCaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateTestCaseRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for TestCase traceability relationships - scope and task linking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestCase Traceability Tests")
class TestCaseTraceabilityTest {

  @Mock
  private TestCaseRepository testCaseRepository;

  @Mock
  private TestRunRepository testRunRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HillChartPointRepository hillChartPointRepository;

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private TestCaseService testCaseService;

  private User user;
  private Cycle cycle;
  private Pitch pitch;
  private HillChartPoint scope;
  private Task task;

  @BeforeEach
  void setUp() {
    // Enable QA Test Management feature for tests
    ReflectionTestUtils.setField(testCaseService, "testManagementEnabled", true);

    user = User.builder().id(1L).username("testuser").build();

    cycle = Cycle.builder().id(1L).name("Cycle 1").build();

    pitch = Pitch.builder().id(1L).title("User Authentication").cycle(cycle).build();

    scope = HillChartPoint.builder().id(1L).scope("Login Form").description("Build login form UI").pitch(pitch)
        .build();

    task = Task.builder().id(1L).title("Implement login validation").cycle(cycle).pitch(pitch).scope(scope).build();
  }

  @Test
  @DisplayName("Should create test case with scope and task links")
  void shouldCreateTestCaseWithScopeAndTask() {
    // Arrange
    CreateTestCaseRequest request = CreateTestCaseRequest.builder()
        .title("TC-001: Verify email validation on login")
        .description("Test that email field shows error for invalid format").priority(TestCasePriority.HIGH)
        .pitchId(1L).scopeId(1L).taskId(1L).build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

    TestCase savedTestCase = TestCase.builder().id(1L).testCaseKey("TC-001").title(request.getTitle())
        .description(request.getDescription()).priority(request.getPriority()).status(TestCaseStatus.READY)
        .pitch(pitch).cycle(cycle).scope(scope).task(task).createdBy(user).build();

    when(testCaseRepository.save(any(TestCase.class))).thenReturn(savedTestCase);

    // Act
    TestCaseDTO result = testCaseService.createTestCase(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals("TC-001: Verify email validation on login", result.getTitle());
    assertEquals(1L, result.getScopeId());
    assertEquals("Login Form", result.getScopeName());
    assertEquals(1L, result.getTaskId());
    assertEquals("Implement login validation", result.getTaskTitle());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository).findById(1L);
    verify(testCaseRepository).save(any(TestCase.class));
  }

  @Test
  @DisplayName("Should create test case without scope/task for general tests")
  void shouldCreateTestCaseWithoutScopeOrTask() {
    // Arrange
    CreateTestCaseRequest request = CreateTestCaseRequest.builder()
        .title("TC-100: Verify app loads on all supported browsers")
        .description("Cross-browser compatibility smoke test").priority(TestCasePriority.MEDIUM).build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    TestCase savedTestCase = TestCase.builder().id(2L).testCaseKey("TC-100").title(request.getTitle())
        .description(request.getDescription()).priority(request.getPriority()).status(TestCaseStatus.READY)
        .scope(null) // General test not tied to specific scope
        .task(null) // General test not tied to specific task
        .createdBy(user).build();

    when(testCaseRepository.save(any(TestCase.class))).thenReturn(savedTestCase);

    // Act
    TestCaseDTO result = testCaseService.createTestCase(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals("TC-100: Verify app loads on all supported browsers", result.getTitle());
    assertNull(result.getScopeId());
    assertNull(result.getScopeName());
    assertNull(result.getTaskId());
    assertNull(result.getTaskTitle());

    verify(hillChartPointRepository, never()).findById(anyLong());
    verify(taskRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should update test case to link with scope and task")
  void shouldUpdateTestCaseToLinkWithScopeAndTask() {
    // Arrange
    TestCase existingTestCase = TestCase.builder().id(1L).testCaseKey("TC-001").title("Generic test")
        .description("Some test").priority(TestCasePriority.LOW).status(TestCaseStatus.READY).createdBy(user)
        .scope(null).task(null).build();

    UpdateTestCaseRequest updateRequest = UpdateTestCaseRequest.builder().scopeId(1L).taskId(1L).build();

    when(testCaseRepository.findById(1L)).thenReturn(Optional.of(existingTestCase));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
    when(testCaseRepository.save(any(TestCase.class))).thenReturn(existingTestCase);

    // Act
    TestCaseDTO result = testCaseService.updateTestCase(1L, updateRequest, 1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getScopeId());
    assertEquals(1L, result.getTaskId());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository).findById(1L);
  }

  @Test
  @DisplayName("Should link test case to scope but not task")
  void shouldLinkTestCaseToScopeOnly() {
    // Arrange
    CreateTestCaseRequest request = CreateTestCaseRequest.builder().title("TC-050: Verify login form UI layout")
        .description("Check all UI elements are properly aligned").priority(TestCasePriority.LOW).scopeId(1L)
        // No taskId - test covers entire scope, not specific task
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));

    TestCase savedTestCase = TestCase.builder().id(3L).testCaseKey("TC-050").title(request.getTitle())
        .description(request.getDescription()).priority(request.getPriority()).status(TestCaseStatus.READY)
        .scope(scope).task(null).createdBy(user).build();

    when(testCaseRepository.save(any(TestCase.class))).thenReturn(savedTestCase);

    // Act
    TestCaseDTO result = testCaseService.createTestCase(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getScopeId());
    assertEquals("Login Form", result.getScopeName());
    assertNull(result.getTaskId());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should throw exception when scope not found")
  void shouldThrowExceptionWhenScopeNotFound() {
    // Arrange
    CreateTestCaseRequest request = CreateTestCaseRequest.builder().title("Test case with invalid scope")
        .description("Test case").priority(TestCasePriority.HIGH).scopeId(999L) // Non-existent scope
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(hillChartPointRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> testCaseService.createTestCase(request, 1L));
    verify(testCaseRepository, never()).save(any(TestCase.class));
  }

  @Test
  @DisplayName("Should throw exception when task not found")
  void shouldThrowExceptionWhenTaskNotFound() {
    // Arrange
    CreateTestCaseRequest request = CreateTestCaseRequest.builder().title("Test case with invalid task")
        .description("Test case").priority(TestCasePriority.HIGH).taskId(999L) // Non-existent task
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> testCaseService.createTestCase(request, 1L));
    verify(testCaseRepository, never()).save(any(TestCase.class));
  }

  @Test
  @DisplayName("Should not change links when update request has no scope/task fields")
  void shouldNotChangeLinksWhenUpdateRequestHasNoScopeOrTaskFields() {
    // Arrange
    TestCase existingTestCase = TestCase.builder().id(1L).testCaseKey("TC-001").title("Linked test")
        .description("Test with links").priority(TestCasePriority.HIGH).status(TestCaseStatus.READY)
        .createdBy(user).scope(scope).task(task).build();

    UpdateTestCaseRequest updateRequest = UpdateTestCaseRequest.builder().title("Updated title")
        // No scopeId or taskId - existing links should remain
        .build();

    when(testCaseRepository.findById(1L)).thenReturn(Optional.of(existingTestCase));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(testCaseRepository.save(any(TestCase.class))).thenReturn(existingTestCase);

    // Act
    TestCaseDTO result = testCaseService.updateTestCase(1L, updateRequest, 1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getScopeId()); // Links should still be present
    assertEquals(1L, result.getTaskId());

    verify(hillChartPointRepository, never()).findById(anyLong());
    verify(taskRepository, never()).findById(anyLong());
  }
}
