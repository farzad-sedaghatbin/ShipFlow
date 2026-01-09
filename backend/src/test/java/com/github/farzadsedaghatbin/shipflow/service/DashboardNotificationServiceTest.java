package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardNotificationDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardNotificationServiceTest {

    @Mock
    private DashboardNotificationRepository notificationRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private HillChartPointRepository hillChartPointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardNotificationService notificationService;

    private User testUser;
    private DashboardNotification testNotification;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testNotification = DashboardNotification.builder()
                .id(1L)
                .user(testUser)
                .type("OVERDUE_TASK")
                .title("Test Notification")
                .message("Test message")
                .severity("WARNING")
                .isRead(false)
                .build();

        Person assignee = Person.builder()
                .id(1L)
                .name("Test Person")
                .build();
        assignee.setUser(testUser);

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .assignee(assignee)
                .dueDate(LocalDate.now().minusDays(1))
                .build();
    }

    @Test
    void getUserNotifications_ShouldReturnActiveNotifications() {
        // Arrange
        when(notificationRepository.findActiveNotifications(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testNotification));

        // Act
        List<DashboardNotificationDTO> result = notificationService.getUserNotifications(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("OVERDUE_TASK", result.get(0).getType());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    void getUnreadNotifications_ShouldReturnOnlyUnreadNotifications() {
        // Arrange
        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(Arrays.asList(testNotification));

        // Act
        List<DashboardNotificationDTO> result = notificationService.getUnreadNotifications(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        // Arrange
        when(notificationRepository.countByUserIdAndIsRead(1L, false))
                .thenReturn(3L);

        // Act
        Long count = notificationService.getUnreadCount(1L);

        // Assert
        assertEquals(3L, count);
    }

    @Test
    void markAsRead_WhenExists_ShouldMarkNotificationAsRead() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(DashboardNotification.class))).thenReturn(testNotification);

        // Act
        DashboardNotificationDTO result = notificationService.markAsRead(1L);

        // Assert
        assertNotNull(result);
        assertTrue(testNotification.getIsRead());
        assertNotNull(testNotification.getReadAt());
        verify(notificationRepository, times(1)).save(any(DashboardNotification.class));
    }

    @Test
    void markAsRead_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            notificationService.markAsRead(999L)
        );
    }

    @Test
    void markAllAsRead_ShouldMarkAllUnreadNotifications() {
        // Arrange
        DashboardNotification notification1 = DashboardNotification.builder()
                .id(1L)
                .user(testUser)
                .type("TEST")
                .title("Test 1")
                .severity("INFO")
                .isRead(false)
                .build();

        DashboardNotification notification2 = DashboardNotification.builder()
                .id(2L)
                .user(testUser)
                .type("TEST")
                .title("Test 2")
                .severity("INFO")
                .isRead(false)
                .build();

        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1L, false))
                .thenReturn(Arrays.asList(notification1, notification2));
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.markAllAsRead(1L);

        // Assert
        assertTrue(notification1.getIsRead());
        assertTrue(notification2.getIsRead());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void deleteNotification_ShouldDeleteNotification() {
        // Act
        notificationService.deleteNotification(1L);

        // Assert
        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    void generateOverdueTaskNotifications_ShouldCreateNotifications() {
        // Arrange
        when(taskRepository.findAll()).thenReturn(Arrays.asList(testTask));
        when(notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(notificationRepository.save(any(DashboardNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.generateDailyNotifications();

        // Assert
        verify(notificationRepository, atLeastOnce()).save(any(DashboardNotification.class));
    }

    @Test
    void generateBlockedTaskNotifications_ShouldCreateNotifications() {
        // Arrange
        testTask.setStatus(TaskStatus.BLOCKED);
        when(taskRepository.findAll()).thenReturn(Arrays.asList(testTask));
        when(notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(notificationRepository.save(any(DashboardNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.generateDailyNotifications();

        // Assert
        verify(notificationRepository, atLeastOnce()).save(any(DashboardNotification.class));
    }

    @Test
    void cleanupOldNotifications_ShouldDeleteExpiredNotifications() {
        // Arrange
        DashboardNotification expiredNotification = DashboardNotification.builder()
                .id(1L)
                .user(testUser)
                .type("TEST")
                .title("Expired")
                .severity("INFO")
                .isRead(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(notificationRepository.findExpiredNotifications(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(expiredNotification));

        // Act
        notificationService.cleanupOldNotifications();

        // Assert
        verify(notificationRepository, times(1)).deleteAll(anyList());
        verify(notificationRepository, times(1)).deleteOldReadNotifications(any(LocalDateTime.class));
    }

    @Test
    void notificationDTO_ShouldContainAllRequiredFields() {
        // Arrange
        testNotification.setActionUrl("/tasks/1");
        testNotification.setEntityType("TASK");
        testNotification.setEntityId(1L);

        when(notificationRepository.findActiveNotifications(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testNotification));

        // Act
        List<DashboardNotificationDTO> result = notificationService.getUserNotifications(1L);

        // Assert
        DashboardNotificationDTO dto = result.get(0);
        assertNotNull(dto.getId());
        assertNotNull(dto.getUserId());
        assertNotNull(dto.getType());
        assertNotNull(dto.getTitle());
        assertNotNull(dto.getSeverity());
        assertEquals("/tasks/1", dto.getActionUrl());
        assertEquals("TASK", dto.getEntityType());
        assertEquals(1L, dto.getEntityId());
    }
}
