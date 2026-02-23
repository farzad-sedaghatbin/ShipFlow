package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardNotificationDTO;
import com.github.farzadsedaghatbin.shipflow.dto.slack.SlackConfigurationDTO;
import com.github.farzadsedaghatbin.shipflow.dto.teams.TeamsConfigurationDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.slack.SlackIntegrationService;
import com.github.farzadsedaghatbin.shipflow.service.teams.TeamsIntegrationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @Mock
  private SlackIntegrationService slackService;

  @Mock
  private TeamsIntegrationService teamsService;

  @InjectMocks
  private DashboardNotificationService notificationService;

  private User testUser;
  private DashboardNotification testNotification;
  private Task testTask;

  @BeforeEach
  void setUp() {
    testUser = User.builder().id(1L).username("testuser").build();

    testNotification = DashboardNotification.builder().id(1L).user(testUser).type("OVERDUE_TASK")
        .title("Test Notification").message("Test message").severity("WARNING").isRead(false).build();

    Person assignee = Person.builder().id(1L).name("Test Person").build();
    assignee.setUser(testUser);

    testTask = Task.builder().id(1L).title("Test Task").status(TaskStatus.TODO).assignee(assignee)
        .dueDate(LocalDate.now().minusDays(1)).build();

    // Mock both services to return empty configuration (simulating no external integration)
    // Use lenient() to avoid UnnecessaryStubbingException for tests that don't use these mocks
    lenient().when(slackService.getActiveConfiguration()).thenReturn(Optional.empty());
    lenient().when(teamsService.getActiveConfiguration()).thenReturn(Optional.empty());
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
    when(notificationRepository.countByUserIdAndIsRead(1L, false)).thenReturn(3L);

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
    assertThrows(RuntimeException.class, () -> notificationService.markAsRead(999L));
  }

  @Test
  void markAllAsRead_ShouldMarkAllUnreadNotifications() {
    // Arrange
    DashboardNotification notification1 = DashboardNotification.builder().id(1L).user(testUser).type("TEST")
        .title("Test 1").severity("INFO").isRead(false).build();

    DashboardNotification notification2 = DashboardNotification.builder().id(2L).user(testUser).type("TEST")
        .title("Test 2").severity("INFO").isRead(false).build();

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
    DashboardNotification expiredNotification = DashboardNotification.builder().id(1L).user(testUser).type("TEST")
        .title("Expired").severity("INFO").isRead(true).expiresAt(LocalDateTime.now().minusDays(1)).build();

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

  @Test
  void notifyCyclePhaseChange_ShouldCreateNotificationsForAllInvolvedUsers() {
    // Arrange
    User user1 = User.builder().id(1L).username("user1").build();
    User user2 = User.builder().id(2L).username("user2").build();

    Person person1 = Person.builder().id(1L).name("Person 1").build();
    person1.setUser(user1);
    Person person2 = Person.builder().id(2L).name("Person 2").build();
    person2.setUser(user2);

    TeamAssignment assignment1 = TeamAssignment.builder().id(1L).person(person1).build();
    TeamAssignment assignment2 = TeamAssignment.builder().id(2L).person(person2).build();

    Team team = Team.builder().id(1L).name("Test Team")
        .assignments(new ArrayList<>(Arrays.asList(assignment1, assignment2))).build();

    Pitch pitch = Pitch.builder().id(1L).title("Test Pitch").team(team).build();

    Cycle cycle = Cycle.builder().id(1L).name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING)
        .pitches(new ArrayList<>(Arrays.asList(pitch))).build();

    when(notificationRepository.save(any(DashboardNotification.class))).thenReturn(testNotification);

    // Act
    notificationService.notifyCyclePhaseChange(cycle, CyclePhase.SHAPING_BUILDING, CyclePhase.BETTING_COOLDOWN);

    // Assert
    verify(notificationRepository, times(2)).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCyclePhaseChange_ShouldNotCreateNotificationsWhenPhaseUnchanged() {
    // Arrange
    Cycle cycle = Cycle.builder().id(1L).name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING)
        .pitches(new ArrayList<>()).build();

    // Act
    notificationService.notifyCyclePhaseChange(cycle, CyclePhase.SHAPING_BUILDING, CyclePhase.SHAPING_BUILDING);

    // Assert
    verify(notificationRepository, never()).save(any(DashboardNotification.class));
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCyclePhaseChange_ShouldHandleCycleWithNoUsers() {
    // Arrange
    Cycle cycle = Cycle.builder().id(1L).name("Test Cycle").phase(CyclePhase.SHAPING_BUILDING)
        .pitches(new ArrayList<>()).build();

    // Act
    notificationService.notifyCyclePhaseChange(cycle, CyclePhase.SHAPING_BUILDING, CyclePhase.BETTING_COOLDOWN);

    // Assert
    verify(notificationRepository, never()).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCircuitBreakerTriggered_ShouldCreateNotificationsForTeamMembers() {
    // Arrange
    User user1 = User.builder().id(1L).username("user1").build();
    User user2 = User.builder().id(2L).username("user2").build();

    Person person1 = Person.builder().id(1L).name("Person 1").build();
    person1.setUser(user1);
    Person person2 = Person.builder().id(2L).name("Person 2").build();
    person2.setUser(user2);

    TeamAssignment assignment1 = TeamAssignment.builder().id(1L).person(person1).build();
    TeamAssignment assignment2 = TeamAssignment.builder().id(2L).person(person2).build();

    Team team = Team.builder().id(1L).name("Test Team")
        .assignments(new ArrayList<>(Arrays.asList(assignment1, assignment2))).build();

    Pitch pitch = Pitch.builder().id(1L).title("Overflowing Pitch").team(team).isCircuitBreakerTriggered(true)
        .circuitBreakerReason("Exceeded appetite by 50%").build();

    when(notificationRepository.save(any(DashboardNotification.class))).thenReturn(testNotification);

    // Act
    notificationService.notifyCircuitBreakerTriggered(pitch);

    // Assert
    verify(notificationRepository, times(2)).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCircuitBreakerTriggered_ShouldHandlePitchWithNoTeam() {
    // Arrange
    Pitch pitch = Pitch.builder().id(1L).title("Overflowing Pitch").team(null).isCircuitBreakerTriggered(true)
        .circuitBreakerReason("Exceeded appetite").build();

    // Act
    notificationService.notifyCircuitBreakerTriggered(pitch);

    // Assert
    verify(notificationRepository, never()).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyPitchKilled_ShouldCreateNotificationsForTeamMembers() {
    // Arrange
    User user1 = User.builder().id(1L).username("user1").build();
    User user2 = User.builder().id(2L).username("user2").build();

    Person person1 = Person.builder().id(1L).name("Person 1").build();
    person1.setUser(user1);
    Person person2 = Person.builder().id(2L).name("Person 2").build();
    person2.setUser(user2);

    TeamAssignment assignment1 = TeamAssignment.builder().id(1L).person(person1).build();
    TeamAssignment assignment2 = TeamAssignment.builder().id(2L).person(person2).build();

    Team team = Team.builder().id(1L).name("Test Team")
        .assignments(new ArrayList<>(Arrays.asList(assignment1, assignment2))).build();

    Pitch pitch = Pitch.builder().id(1L).title("Killed Pitch").team(team).build();

    String reason = "Could not fit in time box";

    when(notificationRepository.save(any(DashboardNotification.class))).thenReturn(testNotification);

    // Act
    notificationService.notifyPitchKilled(pitch, reason);

    // Assert
    verify(notificationRepository, times(2)).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyPitchKilled_ShouldHandlePitchWithNoTeam() {
    // Arrange
    Pitch pitch = Pitch.builder().id(1L).title("Killed Pitch").team(null).build();

    String reason = "Could not fit in time box";

    // Act
    notificationService.notifyPitchKilled(pitch, reason);

    // Assert
    verify(notificationRepository, never()).save(any(DashboardNotification.class));
    // No external notifications since neither Slack nor Teams are configured
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCircuitBreakerTriggered_ShouldSendToSlackWhenConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(
        SlackConfigurationDTO.builder().id(1L).workspaceName("test").build())); // Simulate Slack is configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.empty()); // Teams not configured

    Pitch pitch = Pitch.builder().id(1L).title("Overflowing Pitch").team(null).build();

    // Act
    notificationService.notifyCircuitBreakerTriggered(pitch);

    // Assert
    verify(slackService, times(1)).sendNotification(eq("CIRCUIT_BREAKER_TRIGGERED"), any(String.class), eq(null),
        eq("PITCH"), eq(1L));
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyCircuitBreakerTriggered_ShouldSendToTeamsWhenConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.empty()); // Slack not configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.of(
        TeamsConfigurationDTO.builder().id(1L).tenantName("test").build())); // Simulate Teams is configured

    Pitch pitch = Pitch.builder().id(1L).title("Overflowing Pitch").team(null).build();

    // Act
    notificationService.notifyCircuitBreakerTriggered(pitch);

    // Assert
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, times(1)).sendNotification(eq("CIRCUIT_BREAKER_TRIGGERED"), any(String.class), eq(null),
        eq("PITCH"), eq(1L));
  }

  @Test
  void notifyCircuitBreakerTriggered_ShouldSendToBothWhenBothConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(
        SlackConfigurationDTO.builder().id(1L).workspaceName("test").build())); // Slack configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.of(
        TeamsConfigurationDTO.builder().id(1L).tenantName("test").build())); // Teams configured

    Pitch pitch = Pitch.builder().id(1L).title("Overflowing Pitch").team(null).build();

    // Act
    notificationService.notifyCircuitBreakerTriggered(pitch);

    // Assert
    verify(slackService, times(1)).sendNotification(eq("CIRCUIT_BREAKER_TRIGGERED"), any(String.class), eq(null),
        eq("PITCH"), eq(1L));
    verify(teamsService, times(1)).sendNotification(eq("CIRCUIT_BREAKER_TRIGGERED"), any(String.class), eq(null),
        eq("PITCH"), eq(1L));
  }

  @Test
  void notifyPitchKilled_ShouldSendToSlackWhenConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(
        SlackConfigurationDTO.builder().id(1L).workspaceName("test").build())); // Simulate Slack is configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.empty()); // Teams not configured

    Pitch pitch = Pitch.builder().id(1L).title("Killed Pitch").team(null).build();
    String reason = "Could not fit in time box";

    // Act
    notificationService.notifyPitchKilled(pitch, reason);

    // Assert
    verify(slackService, times(1)).sendNotification(eq("PITCH_KILLED"), any(String.class), eq(null), eq("PITCH"),
        eq(1L));
    verify(teamsService, never()).sendNotification(any(), any(), any(), any(), any());
  }

  @Test
  void notifyPitchKilled_ShouldSendToTeamsWhenConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.empty()); // Slack not configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.of(
        TeamsConfigurationDTO.builder().id(1L).tenantName("test").build())); // Simulate Teams is configured

    Pitch pitch = Pitch.builder().id(1L).title("Killed Pitch").team(null).build();
    String reason = "Could not fit in time box";

    // Act
    notificationService.notifyPitchKilled(pitch, reason);

    // Assert
    verify(slackService, never()).sendNotification(any(), any(), any(), any(), any());
    verify(teamsService, times(1)).sendNotification(eq("PITCH_KILLED"), any(String.class), eq(null), eq("PITCH"),
        eq(1L));
  }

  @Test
  void notifyPitchKilled_ShouldSendToBothWhenBothConfigured() {
    // Arrange
    when(slackService.getActiveConfiguration()).thenReturn(Optional.of(
        SlackConfigurationDTO.builder().id(1L).workspaceName("test").build())); // Slack configured
    when(teamsService.getActiveConfiguration()).thenReturn(Optional.of(
        TeamsConfigurationDTO.builder().id(1L).tenantName("test").build())); // Teams configured

    Pitch pitch = Pitch.builder().id(1L).title("Killed Pitch").team(null).build();
    String reason = "Could not fit in time box";

    // Act
    notificationService.notifyPitchKilled(pitch, reason);

    // Assert
    verify(slackService, times(1)).sendNotification(eq("PITCH_KILLED"), any(String.class), eq(null), eq("PITCH"),
        eq(1L));
    verify(teamsService, times(1)).sendNotification(eq("PITCH_KILLED"), any(String.class), eq(null), eq("PITCH"),
        eq(1L));
  }
}
