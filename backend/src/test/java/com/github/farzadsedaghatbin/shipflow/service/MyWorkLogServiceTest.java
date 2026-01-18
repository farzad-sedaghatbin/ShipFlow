package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateWorkLogForSelfRequest;
import com.github.farzadsedaghatbin.shipflow.dto.WorkLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for "My Work Logs" functionality in WorkLogService.
 * Tests the ability for users to create and manage their own work logs.
 */
@ExtendWith(MockitoExtension.class)
class MyWorkLogServiceTest {

    @Mock
    private WorkLogRepository workLogRepository;

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private WorkLogService workLogService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private Person testPerson;
    private Person otherPerson;
    private User testUser;
    private Pitch testPitch;
    private WorkLog testWorkLog;
    private WorkLog otherPersonWorkLog;

    @BeforeEach
    void setUp() {        
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
            String key = i.getArgument(0);
            if (key.contains("delete") && key.contains("own")) return "You can only delete your own work logs";
            if (key.contains("update") && key.contains("own")) return "You can only update your own work logs";
            if (key.contains("own.only")) return "You can only update your own work logs";
            if (key.contains("no.person.profile")) return "User is not linked to a person profile";
            if (key.contains("update.own")) return "You can only update your own work logs";
            return key;
        });
        lenient().when(messageService.getMessage(anyString())).thenAnswer(i -> {
            String key = i.getArgument(0);
            if (key.contains("delete") && key.contains("own")) return "You can only delete your own work logs";
            if (key.contains("update") && key.contains("own")) return "You can only update your own work logs";
            if (key.contains("own.only")) return "You can only update your own work logs";
            if (key.contains("no.person.profile")) return "User is not linked to a person profile";
            if (key.contains("update.own")) return "You can only update your own work logs";
            return key;
        });
        // Set up security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        testPerson = Person.builder()
                .id(1L)
                .name("Test User")
                .email("testuser@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        otherPerson = Person.builder()
                .id(2L)
                .name("Other User")
                .email("other@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .role(UserRole.DEVELOPER)
                .person(testPerson)
                .isActive(true)
                .build();

        testPitch = Pitch.builder()
                .id(1L)
                .title("Test Pitch")
                .build();

        testWorkLog = WorkLog.builder()
                .id(1L)
                .person(testPerson)
                .pitch(testPitch)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(4.0))
                .note("My work log")
                .build();

        otherPersonWorkLog = WorkLog.builder()
                .id(2L)
                .person(otherPerson)
                .pitch(testPitch)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(3.0))
                .note("Other's work log")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyWorkLogs_ShouldReturnOnlyCurrentUserWorkLogs() {
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findByPersonId(1L)).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getMyWorkLogs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNote()).isEqualTo("My work log");
        verify(workLogRepository).findByPersonId(1L);
    }

    @Test
    void getMyWorkLogsByCycle_ShouldReturnUserWorkLogsForCycle() {
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findByPersonIdAndCycleId(1L, 10L)).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getMyWorkLogsByCycle(10L);

        assertThat(result).hasSize(1);
        verify(workLogRepository).findByPersonIdAndCycleId(1L, 10L);
    }

    @Test
    void getMyWorkLogsByDate_ShouldReturnUserWorkLogsForDate() {
        LocalDate today = LocalDate.now();
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findByPersonIdAndDate(1L, today)).thenReturn(Arrays.asList(testWorkLog));

        List<WorkLogDTO> result = workLogService.getMyWorkLogsByDate(today);

        assertThat(result).hasSize(1);
        verify(workLogRepository).findByPersonIdAndDate(1L, today);
    }

    @Test
    void createMyWorkLog_ShouldCreateWorkLogForCurrentUser() {
        CreateWorkLogForSelfRequest request = CreateWorkLogForSelfRequest.builder()
                .pitchId(1L)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(2.5))
                .note("New work")
                .build();

        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
        when(workLogRepository.save(any(WorkLog.class))).thenReturn(testWorkLog);

        WorkLogDTO result = workLogService.createMyWorkLog(request);

        assertThat(result).isNotNull();
        verify(workLogRepository).save(any(WorkLog.class));
    }

    @Test
    void createMyWorkLog_WhenUserNotLinkedToPerson_ShouldThrowException() {
        User userWithoutPerson = User.builder()
                .id(3L)
                .username("testuser")
                .email("nolink@example.com")
                .role(UserRole.DEVELOPER)
                .person(null)
                .isActive(true)
                .build();

        CreateWorkLogForSelfRequest request = CreateWorkLogForSelfRequest.builder()
                .pitchId(1L)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(2.5))
                .build();

        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(userWithoutPerson));

        assertThatThrownBy(() -> workLogService.createMyWorkLog(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not linked to a person profile");
    }

    @Test
    void updateMyWorkLog_WhenOwned_ShouldUpdateWorkLog() {
        CreateWorkLogForSelfRequest request = CreateWorkLogForSelfRequest.builder()
                .pitchId(1L)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(6.0))
                .note("Updated work")
                .build();

        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findById(1L)).thenReturn(Optional.of(testWorkLog));
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
        when(workLogRepository.save(any(WorkLog.class))).thenReturn(testWorkLog);

        WorkLogDTO result = workLogService.updateMyWorkLog(1L, request);

        assertThat(result).isNotNull();
        verify(workLogRepository).save(any(WorkLog.class));
    }

    @Test
    void updateMyWorkLog_WhenNotOwned_ShouldThrowException() {
        CreateWorkLogForSelfRequest request = CreateWorkLogForSelfRequest.builder()
                .pitchId(1L)
                .date(LocalDate.now())
                .hoursSpent(BigDecimal.valueOf(6.0))
                .note("Trying to update other's work")
                .build();

        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findById(2L)).thenReturn(Optional.of(otherPersonWorkLog));

        assertThatThrownBy(() -> workLogService.updateMyWorkLog(2L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only update your own work logs");
    }

    @Test
    void deleteMyWorkLog_WhenOwned_ShouldDeleteWorkLog() {
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findById(1L)).thenReturn(Optional.of(testWorkLog));
        doNothing().when(workLogRepository).deleteById(1L);

        workLogService.deleteMyWorkLog(1L);

        verify(workLogRepository).deleteById(1L);
    }

    @Test
    void deleteMyWorkLog_WhenNotOwned_ShouldThrowException() {
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.of(testUser));
        when(workLogRepository.findById(2L)).thenReturn(Optional.of(otherPersonWorkLog));

        assertThatThrownBy(() -> workLogService.deleteMyWorkLog(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("only delete your own work logs");

        verify(workLogRepository, never()).deleteById(any());
    }

    @Test
    void getMyWorkLogs_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findByUsernameWithPerson("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workLogService.getMyWorkLogs())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
