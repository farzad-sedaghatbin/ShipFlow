package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.cooldown.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CooldownActivityServiceTest {

  @Mock private CooldownActivityRepository cooldownActivityRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private UserRepository userRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private MessageService messageService;

  @InjectMocks private CooldownActivityService cooldownActivityService;

  private Cycle testCycle;
  private User testUser;
  private User testAssignee;
  private Pitch testPitch;
  private CooldownActivity testActivity;

  @BeforeEach
  void setUp() {
    testCycle = Cycle.builder()
        .id(1L)
        .name("Test Cycle")
        .build();

    testUser = User.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .build();

    testAssignee = User.builder()
        .id(2L)
        .username("assignee")
        .email("assignee@example.com")
        .build();

    testPitch = Pitch.builder()
        .id(1L)
        .title("Related Pitch")
        .build();

    testActivity = CooldownActivity.builder()
        .id(1L)
        .cycle(testCycle)
        .activityType(CooldownActivityType.TECH_DEBT)
        .title("Fix database indexes")
        .description("Add missing indexes for performance")
        .status(CooldownActivityStatus.PLANNED)
        .createdBy(testUser)
        .estimatedHours(4)
        .priority(2)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Nested
  @DisplayName("createActivity")
  class CreateActivityTests {

    @Test
    @DisplayName("should create activity with all fields")
    void shouldCreateActivityWithAllFields() {
      CreateCooldownActivityRequest request = CreateCooldownActivityRequest.builder()
          .cycleId(1L)
          .activityType(CooldownActivityType.TECH_DEBT)
          .title("Fix database indexes")
          .description("Add missing indexes for performance")
          .estimatedHours(4)
          .priority(2)
          .assigneeId(2L)
          .relatedPitchId(1L)
          .notes("High priority for next cycle")
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userRepository.findById(2L)).thenReturn(Optional.of(testAssignee));
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(invocation -> {
        CooldownActivity activity = invocation.getArgument(0);
        activity.setId(1L);
        activity.setCreatedAt(LocalDateTime.now());
        return activity;
      });

      CooldownActivityDTO result = cooldownActivityService.createActivity(request, 1L);

      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("Fix database indexes");
      assertThat(result.getActivityType()).isEqualTo(CooldownActivityType.TECH_DEBT);
      assertThat(result.getStatus()).isEqualTo(CooldownActivityStatus.PLANNED);

      ArgumentCaptor<CooldownActivity> captor = ArgumentCaptor.forClass(CooldownActivity.class);
      verify(cooldownActivityRepository).save(captor.capture());
      CooldownActivity saved = captor.getValue();
      assertThat(saved.getAssignee()).isEqualTo(testAssignee);
      assertThat(saved.getRelatedPitch()).isEqualTo(testPitch);
    }

    @Test
    @DisplayName("should create activity with minimal fields")
    void shouldCreateActivityWithMinimalFields() {
      CreateCooldownActivityRequest request = CreateCooldownActivityRequest.builder()
          .cycleId(1L)
          .activityType(CooldownActivityType.BUG_FIX)
          .title("Fix login bug")
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(invocation -> {
        CooldownActivity activity = invocation.getArgument(0);
        activity.setId(1L);
        activity.setCreatedAt(LocalDateTime.now());
        return activity;
      });

      CooldownActivityDTO result = cooldownActivityService.createActivity(request, 1L);

      assertThat(result).isNotNull();
      assertThat(result.getPriority()).isEqualTo(3); // Default priority
    }

    @Test
    @DisplayName("should throw when cycle not found")
    void shouldThrowWhenCycleNotFound() {
      CreateCooldownActivityRequest request = CreateCooldownActivityRequest.builder()
          .cycleId(999L)
          .activityType(CooldownActivityType.TECH_DEBT)
          .title("Test")
          .build();

      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("error.cycle.not.found"), any(Object[].class))).thenReturn("Cycle not found");

      assertThatThrownBy(() -> cooldownActivityService.createActivity(request, 1L))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when creator not found")
    void shouldThrowWhenCreatorNotFound() {
      CreateCooldownActivityRequest request = CreateCooldownActivityRequest.builder()
          .cycleId(1L)
          .activityType(CooldownActivityType.TECH_DEBT)
          .title("Test")
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findById(999L)).thenReturn(Optional.empty());
      when(messageService.getMessage(eq("error.user.not.found"), any(Object[].class))).thenReturn("User not found");

      assertThatThrownBy(() -> cooldownActivityService.createActivity(request, 999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getActivity")
  class GetActivityTests {

    @Test
    @DisplayName("should return activity by ID")
    void shouldReturnActivityById() {
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));

      CooldownActivityDTO result = cooldownActivityService.getActivity(1L);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getTitle()).isEqualTo("Fix database indexes");
    }

    @Test
    @DisplayName("should throw when activity not found")
    void shouldThrowWhenActivityNotFound() {
      when(cooldownActivityRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> cooldownActivityService.getActivity(999L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("999");
    }
  }

  @Nested
  @DisplayName("getActivitiesByCycle")
  class GetActivitiesByCycleTests {

    @Test
    @DisplayName("should return activities sorted by priority")
    void shouldReturnActivitiesSortedByPriority() {
      CooldownActivity activity2 = CooldownActivity.builder()
          .id(2L)
          .cycle(testCycle)
          .activityType(CooldownActivityType.BUG_FIX)
          .title("Fix bug")
          .status(CooldownActivityStatus.IN_PROGRESS)
          .createdBy(testUser)
          .priority(1)
          .createdAt(LocalDateTime.now())
          .build();

      when(cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(1L))
          .thenReturn(List.of(activity2, testActivity));

      List<CooldownActivityDTO> result = cooldownActivityService.getActivitiesByCycle(1L);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getPriority()).isEqualTo(1);
      assertThat(result.get(1).getPriority()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return empty list when no activities")
    void shouldReturnEmptyListWhenNoActivities() {
      when(cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(1L))
          .thenReturn(Collections.emptyList());

      List<CooldownActivityDTO> result = cooldownActivityService.getActivitiesByCycle(1L);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("updateStatus")
  class UpdateStatusTests {

    @Test
    @DisplayName("should update status to in-progress and set startedAt")
    void shouldUpdateStatusToInProgressAndSetStartedAt() {
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      CooldownActivityDTO result = cooldownActivityService.updateStatus(1L, CooldownActivityStatus.IN_PROGRESS, null);

      assertThat(result.getStatus()).isEqualTo(CooldownActivityStatus.IN_PROGRESS);
      
      ArgumentCaptor<CooldownActivity> captor = ArgumentCaptor.forClass(CooldownActivity.class);
      verify(cooldownActivityRepository).save(captor.capture());
      assertThat(captor.getValue().getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("should update status to completed and set completedAt and actualHours")
    void shouldUpdateStatusToCompletedWithActualHours() {
      testActivity.setStatus(CooldownActivityStatus.IN_PROGRESS);
      testActivity.setStartedAt(LocalDateTime.now().minusHours(3));
      
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      CooldownActivityDTO result = cooldownActivityService.updateStatus(1L, CooldownActivityStatus.COMPLETED, 5);

      assertThat(result.getStatus()).isEqualTo(CooldownActivityStatus.COMPLETED);
      assertThat(result.getActualHours()).isEqualTo(5);
      
      ArgumentCaptor<CooldownActivity> captor = ArgumentCaptor.forClass(CooldownActivity.class);
      verify(cooldownActivityRepository).save(captor.capture());
      assertThat(captor.getValue().getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("should not overwrite startedAt if already set")
    void shouldNotOverwriteStartedAtIfAlreadySet() {
      LocalDateTime originalStartTime = LocalDateTime.now().minusDays(1);
      testActivity.setStartedAt(originalStartTime);
      testActivity.setStatus(CooldownActivityStatus.BLOCKED);
      
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      cooldownActivityService.updateStatus(1L, CooldownActivityStatus.IN_PROGRESS, null);

      ArgumentCaptor<CooldownActivity> captor = ArgumentCaptor.forClass(CooldownActivity.class);
      verify(cooldownActivityRepository).save(captor.capture());
      assertThat(captor.getValue().getStartedAt()).isEqualTo(originalStartTime);
    }
  }

  @Nested
  @DisplayName("getCycleSummary")
  class GetCycleSummaryTests {

    @Test
    @DisplayName("should build comprehensive summary")
    void shouldBuildComprehensiveSummary() {
      CooldownActivity activity2 = CooldownActivity.builder()
          .id(2L)
          .cycle(testCycle)
          .activityType(CooldownActivityType.BUG_FIX)
          .title("Fix bug")
          .status(CooldownActivityStatus.COMPLETED)
          .createdBy(testUser)
          .estimatedHours(2)
          .actualHours(3)
          .priority(1)
          .createdAt(LocalDateTime.now())
          .build();

      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(1L))
          .thenReturn(List.of(activity2, testActivity));
      when(cooldownActivityRepository.countActivitiesByTypeForCycle(1L))
          .thenReturn(List.of(
              new Object[]{CooldownActivityType.TECH_DEBT, 1L},
              new Object[]{CooldownActivityType.BUG_FIX, 1L}
          ));
      when(cooldownActivityRepository.countActivitiesByStatusForCycle(1L))
          .thenReturn(List.of(
              new Object[]{CooldownActivityStatus.PLANNED, 1L},
              new Object[]{CooldownActivityStatus.COMPLETED, 1L}
          ));
      when(cooldownActivityRepository.sumEstimatedHoursByCycleId(1L)).thenReturn(6);
      when(cooldownActivityRepository.sumActualHoursForCompletedByCycleId(1L)).thenReturn(3);

      CooldownSummaryDTO result = cooldownActivityService.getCycleSummary(1L);

      assertThat(result.getCycleId()).isEqualTo(1L);
      assertThat(result.getCycleName()).isEqualTo("Test Cycle");
      assertThat(result.getTotalActivities()).isEqualTo(2);
      assertThat(result.getCompletedCount()).isEqualTo(1);
      assertThat(result.getPlannedCount()).isEqualTo(1);
      assertThat(result.getTotalEstimatedHours()).isEqualTo(6);
      assertThat(result.getTotalActualHours()).isEqualTo(3);
      assertThat(result.getCompletionPercentage()).isEqualTo(50.0);
      assertThat(result.getCountByType()).containsEntry(CooldownActivityType.TECH_DEBT, 1L);
      assertThat(result.getCountByType()).containsEntry(CooldownActivityType.BUG_FIX, 1L);
    }

    @Test
    @DisplayName("should handle empty activities")
    void shouldHandleEmptyActivities() {
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(cooldownActivityRepository.findByCycleIdOrderByPriorityAscCreatedAtDesc(1L))
          .thenReturn(Collections.emptyList());
      when(cooldownActivityRepository.countActivitiesByTypeForCycle(1L))
          .thenReturn(Collections.emptyList());
      when(cooldownActivityRepository.countActivitiesByStatusForCycle(1L))
          .thenReturn(Collections.emptyList());
      when(cooldownActivityRepository.sumEstimatedHoursByCycleId(1L)).thenReturn(0);
      when(cooldownActivityRepository.sumActualHoursForCompletedByCycleId(1L)).thenReturn(0);

      CooldownSummaryDTO result = cooldownActivityService.getCycleSummary(1L);

      assertThat(result.getTotalActivities()).isZero();
      assertThat(result.getCompletionPercentage()).isZero();
    }
  }

  @Nested
  @DisplayName("updateActivity")
  class UpdateActivityTests {

    @Test
    @DisplayName("should update activity details")
    void shouldUpdateActivityDetails() {
      CreateCooldownActivityRequest request = CreateCooldownActivityRequest.builder()
          .cycleId(1L)
          .activityType(CooldownActivityType.REFACTORING)
          .title("Updated Title")
          .description("Updated description")
          .estimatedHours(8)
          .priority(1)
          .build();

      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      CooldownActivityDTO result = cooldownActivityService.updateActivity(1L, request);

      assertThat(result.getTitle()).isEqualTo("Updated Title");
      assertThat(result.getActivityType()).isEqualTo(CooldownActivityType.REFACTORING);
      assertThat(result.getEstimatedHours()).isEqualTo(8);
      assertThat(result.getPriority()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("deleteActivity")
  class DeleteActivityTests {

    @Test
    @DisplayName("should delete existing activity")
    void shouldDeleteExistingActivity() {
      when(cooldownActivityRepository.existsById(1L)).thenReturn(true);

      cooldownActivityService.deleteActivity(1L);

      verify(cooldownActivityRepository).deleteById(1L);
    }

    @Test
    @DisplayName("should throw when activity not found")
    void shouldThrowWhenActivityNotFound() {
      when(cooldownActivityRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> cooldownActivityService.deleteActivity(999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("assignToUser")
  class AssignToUserTests {

    @Test
    @DisplayName("should assign activity to user")
    void shouldAssignActivityToUser() {
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(userRepository.findById(2L)).thenReturn(Optional.of(testAssignee));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      CooldownActivityDTO result = cooldownActivityService.assignToUser(1L, 2L);

      assertThat(result.getAssigneeUsername()).isEqualTo("assignee");
    }

    @Test
    @DisplayName("should unassign when userId is null")
    void shouldUnassignWhenUserIdIsNull() {
      testActivity.setAssignee(testAssignee);
      when(cooldownActivityRepository.findById(1L)).thenReturn(Optional.of(testActivity));
      when(cooldownActivityRepository.save(any(CooldownActivity.class))).thenAnswer(i -> i.getArgument(0));

      CooldownActivityDTO result = cooldownActivityService.assignToUser(1L, null);

      assertThat(result.getAssigneeId()).isNull();
    }
  }
}
