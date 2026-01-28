package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BugReport traceability relationships - scope and task linking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BugReport Traceability Tests")
class BugReportTraceabilityTest {

    @Mock
    private BugReportRepository bugReportRepository;
    
    @Mock
    private TestRunRepository testRunRepository;
    
    @Mock
    private PitchRepository pitchRepository;
    
    @Mock
    private CycleRepository cycleRepository;
    
    @Mock
    private TeamRepository teamRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private HillChartPointRepository hillChartPointRepository;
    
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private BugReportService bugReportService;

    private User user;
    private Cycle cycle;
    private Pitch pitch;
    private HillChartPoint scope;
    private Task task;

    @BeforeEach
    void setUp() {
        // Enable QA Test Management feature for tests
        ReflectionTestUtils.setField(bugReportService, "testManagementEnabled", true);
        
        user = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        cycle = Cycle.builder()
                .id(1L)
                .name("Cycle 1")
                .build();

        pitch = Pitch.builder()
                .id(1L)
                .title("User Authentication")
                .cycle(cycle)
                .build();

        scope = HillChartPoint.builder()
                .id(1L)
                .scope("Login Form")
                .description("Build login form UI")
                .pitch(pitch)
                .build();

        task = Task.builder()
                .id(1L)
                .title("Implement login validation")
                .cycle(cycle)
                .pitch(pitch)
                .scope(scope)
                .build();
    }

    @Test
    @DisplayName("Should create bug report with scope and task links")
    void shouldCreateBugReportWithScopeAndTask() {
        // Arrange
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("Login form validation fails for empty email")
                .description("When submitting login form with empty email, no error is shown")
                .severity(BugSeverity.MAJOR)
                .pitchId(1L)
                .scopeId(1L)
                .taskId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        
        BugReport savedBug = BugReport.builder()
                .id(1L)
                .bugKey("BUG-001")
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status(BugStatus.OPEN)
                .pitch(pitch)
                .cycle(cycle)
                .scope(scope)
                .task(task)
                .reporter(user)
                .build();
        
        when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

        // Act
        BugReportDTO result = bugReportService.createBugReport(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Login form validation fails for empty email", result.getTitle());
        assertEquals(1L, result.getScopeId());
        assertEquals("Login Form", result.getScopeName());
        assertEquals(1L, result.getTaskId());
        assertEquals("Implement login validation", result.getTaskTitle());
        
        verify(hillChartPointRepository).findById(1L);
        verify(taskRepository).findById(1L);
        verify(bugReportRepository).save(any(BugReport.class));
    }

    @Test
    @DisplayName("Should create bug report without scope/task for general bugs")
    void shouldCreateBugReportWithoutScopeOrTask() {
        // Arrange
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("Application crashes on startup in IE11")
                .description("Legacy browser compatibility issue")
                .severity(BugSeverity.CRITICAL)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        BugReport savedBug = BugReport.builder()
                .id(2L)
                .bugKey("BUG-002")
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status(BugStatus.OPEN)
                .scope(null)  // No specific scope
                .task(null)   // No specific task
                .reporter(user)
                .build();
        
        when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

        // Act
        BugReportDTO result = bugReportService.createBugReport(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Application crashes on startup in IE11", result.getTitle());
        assertNull(result.getScopeId());
        assertNull(result.getScopeName());
        assertNull(result.getTaskId());
        assertNull(result.getTaskTitle());
        
        verify(hillChartPointRepository, never()).findById(anyLong());
        verify(taskRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should update bug report to link with scope and task")
    void shouldUpdateBugReportToLinkWithScopeAndTask() {
        // Arrange
        BugReport existingBug = BugReport.builder()
                .id(1L)
                .bugKey("BUG-001")
                .title("Generic bug")
                .description("Some issue")
                .severity(BugSeverity.MINOR)
                .status(BugStatus.OPEN)
                .reporter(user)
                .scope(null)
                .task(null)
                .build();

        UpdateBugReportRequest updateRequest = UpdateBugReportRequest.builder()
                .scopeId(1L)
                .taskId(1L)
                .build();

        when(bugReportRepository.findById(1L)).thenReturn(Optional.of(existingBug));
        when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(bugReportRepository.save(any(BugReport.class))).thenReturn(existingBug);

        // Act
        BugReportDTO result = bugReportService.updateBugReport(1L, updateRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getScopeId());
        assertEquals(1L, result.getTaskId());
        
        verify(hillChartPointRepository).findById(1L);
        verify(taskRepository).findById(1L);
    }

    @Test
    @DisplayName("Should link bug to scope but not task")
    void shouldLinkBugToScopeOnly() {
        // Arrange
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("UI inconsistency in login form")
                .description("Font size varies across fields")
                .severity(BugSeverity.MINOR)
                .scopeId(1L)
                // No taskId - bug is related to scope but not specific task
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
        
        BugReport savedBug = BugReport.builder()
                .id(3L)
                .bugKey("BUG-003")
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status(BugStatus.OPEN)
                .scope(scope)
                .task(null)
                .reporter(user)
                .build();
        
        when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

        // Act
        BugReportDTO result = bugReportService.createBugReport(request, 1L);

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
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("Bug with invalid scope")
                .description("Test bug")
                .severity(BugSeverity.MAJOR)
                .scopeId(999L)  // Non-existent scope
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hillChartPointRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> bugReportService.createBugReport(request, 1L));
        verify(bugReportRepository, never()).save(any(BugReport.class));
    }

    @Test
    @DisplayName("Should throw exception when task not found")
    void shouldThrowExceptionWhenTaskNotFound() {
        // Arrange
        CreateBugReportRequest request = CreateBugReportRequest.builder()
                .title("Bug with invalid task")
                .description("Test bug")
                .severity(BugSeverity.MAJOR)
                .taskId(999L)  // Non-existent task
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> bugReportService.createBugReport(request, 1L));
        verify(bugReportRepository, never()).save(any(BugReport.class));
    }
}
