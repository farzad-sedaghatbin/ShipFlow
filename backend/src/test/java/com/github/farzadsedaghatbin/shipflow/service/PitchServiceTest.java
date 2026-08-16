package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PitchServiceTest {

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private AICacheService cacheService;

  @Mock
  private com.github.farzadsedaghatbin.shipflow.repository.UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @Mock
  private CapacityConfigService capacityConfigService;

  @Mock
  private PitchCycleAssignmentService pitchCycleAssignmentService;

  @InjectMocks
  private PitchService pitchService;

  private Pitch testPitch;
  private Cycle testCycle;
  private Team testTeam;
  private com.github.farzadsedaghatbin.shipflow.entity.User testUser;
  private CreatePitchRequest testRequest;

  @BeforeEach
  void setUp() {
    testCycle = Cycle.builder().id(1L).name("Test Cycle").build();

    testTeam = Team.builder().id(1L).name("Test Team").build();

    testUser = com.github.farzadsedaghatbin.shipflow.entity.User.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .build();

    testPitch = Pitch.builder().id(1L).title("Test Pitch").description("Test Description").appetiteDays(14)
        .cycle(testCycle).team(testTeam).status(PitchStatus.PENDING).build();

    testRequest = new CreatePitchRequest();
    testRequest.setTitle("Test Pitch");
    testRequest.setDescription("Test Description");
    testRequest.setAppetiteDays(14);
    testRequest.setCycleId(1L);
    testRequest.setTeamId(1L);
    testRequest.setStatus(PitchStatus.PENDING);

    // Setup capacity config mock - default 8 hours/day, 14 days * 8 hours = 112 hours
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any(Pitch.class))).thenReturn(112.0);
    lenient().when(capacityConfigService.getOrganizationDefaultHoursPerDay()).thenReturn(8.0);
    
    // Mock calculateTeamBudget to return a valid TeamBudget with empty member list
    var emptyTeamBudget = CapacityConfigService.TeamBudget.builder()
        .memberCount(0)
        .totalBudgetHours(0.0)
        .totalDailyCapacityHours(0.0)
        .memberBudgets(java.util.Collections.emptyList())
        .build();
    lenient().when(capacityConfigService.calculateTeamBudget(any(Team.class), any(Integer.class)))
        .thenReturn(emptyTeamBudget);
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    // Clear security context to prevent leaking authentication state into other tests
    SecurityContextHolder.clearContext();
  }

  @Test
  void getAllPitches_ShouldReturnAllPitches() {
    when(pitchRepository.findAllNotDeleted()).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getAllPitches();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void getPitchById_WhenExists_ShouldReturnPitch() {
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));

    PitchDTO result = pitchService.getPitchById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Pitch");
  }

  @Test
  void getPitchById_WhenNotExists_ShouldThrowException() {
    when(pitchRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> pitchService.getPitchById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Pitch not found");
  }

  @Test
  void createPitch_ShouldSavePitch() {
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    PitchDTO result = pitchService.createPitch(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Pitch");
    verify(pitchRepository).save(any(Pitch.class));
  }

  @Test
  void updatePitch_WhenExists_ShouldUpdatePitch() {
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    testRequest.setTitle("Updated Pitch");
    PitchDTO result = pitchService.updatePitch(1L, testRequest);

    assertThat(result).isNotNull();
    verify(pitchRepository).save(any(Pitch.class));
  }

  @Test
  void updatePitch_ToShapedWithoutAppetite_ShouldThrow() {
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));

    // Editing a pitch to SHAPED without an appetite must be rejected so it cannot become a
    // betting candidate with a null appetite (which breaks drag-and-drop and betting decisions).
    testRequest.setStatus(PitchStatus.SHAPED);
    testRequest.setAppetiteDays(null);

    assertThatThrownBy(() -> pitchService.updatePitch(1L, testRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Appetite days is required");
    verify(pitchRepository, never()).save(any(Pitch.class));
  }

  @Test
  void deletePitch_ShouldCallRepository() {
    // Setup security context
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));

    when(pitchRepository.findById(1L)).thenReturn(java.util.Optional.of(testPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    pitchService.deletePitch(1L);

    verify(pitchRepository).save(testPitch);
    assertThat(testPitch.getDeletedAt()).isNotNull();
    assertThat(testPitch.getDeletedBy()).isEqualTo(testUser);
  }

  @Test
  void getPitchesByCycleId_ShouldReturnPitches() {
    when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getPitchesByCycleId(1L);

    assertThat(result).hasSize(1);
    verify(pitchRepository).findByCycleIdNotDeleted(1L);
  }

  @Test
  void getPitchesByTeamId_ShouldReturnPitches() {
    when(pitchRepository.findByTeamIdNotDeleted(1L)).thenReturn(Arrays.asList(testPitch));

    List<PitchDTO> result = pitchService.getPitchesByTeamId(1L);

    assertThat(result).hasSize(1);
    verify(pitchRepository).findByTeamIdNotDeleted(1L);
  }

  @Test
  void updateStatus_ShouldUpdatePitchStatus() {
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    PitchDTO result = pitchService.updateStatus(1L, PitchStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    verify(pitchRepository).save(any(Pitch.class));
  }

  @Test
  void createPitch_WithPriority_ShouldPersistPriority() {
    testRequest.setPriority(BusinessValue.HIGH);
    testRequest.setSortOrder(3);

    Pitch savedPitch = Pitch.builder()
        .id(1L).title("Test Pitch").description("Test Description")
        .appetiteDays(14)
        .cycle(testCycle).team(testTeam)
        .status(PitchStatus.PENDING)
        .priority(BusinessValue.HIGH)
        .sortOrder(3)
        .build();

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(savedPitch);

    PitchDTO result = pitchService.createPitch(testRequest);

    assertThat(result.getPriority()).isEqualTo(BusinessValue.HIGH);
    assertThat(result.getSortOrder()).isEqualTo(3);
    verify(pitchRepository).save(argThat(p -> p.getPriority() == BusinessValue.HIGH && p.getSortOrder() == 3));
  }

  @Test
  void updatePitch_WithPriority_ShouldUpdatePriority() {
    testPitch.setPriority(BusinessValue.MEDIUM);
    testRequest.setPriority(BusinessValue.LOW);

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    pitchService.updatePitch(1L, testRequest);

    verify(pitchRepository).save(argThat(p -> p.getPriority() == BusinessValue.LOW));
  }

  @Test
  void reorder_ShouldUpdateSortOrderForEachItem() {
    Pitch pitch1 = Pitch.builder().id(1L).title("Pitch 1").status(PitchStatus.PENDING).build();
    Pitch pitch2 = Pitch.builder().id(2L).title("Pitch 2").status(PitchStatus.PENDING).build();

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(pitch1));
    when(pitchRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(pitch2));
    when(pitchRepository.saveAll(any())).thenReturn(List.of(pitch1, pitch2));

    ReorderRequest request = ReorderRequest.builder()
        .items(Arrays.asList(
            ReorderRequest.ReorderItem.builder().id(1L).sortOrder(0).build(),
            ReorderRequest.ReorderItem.builder().id(2L).sortOrder(1).build()
        ))
        .build();

    pitchService.reorder(request);

    verify(pitchRepository).saveAll(argThat(iterable -> {
      List<Pitch> saved = new java.util.ArrayList<>();
      iterable.forEach(saved::add);
      return saved.stream().anyMatch(p -> p.getId().equals(1L) && p.getSortOrder() == 0)
          && saved.stream().anyMatch(p -> p.getId().equals(2L) && p.getSortOrder() == 1);
    }));
  }

  @Test
  void reorder_WhenPitchNotFound_ShouldThrowException() {
    when(pitchRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    ReorderRequest request = ReorderRequest.builder()
        .items(List.of(ReorderRequest.ReorderItem.builder().id(999L).sortOrder(0).build()))
        .build();

    assertThatThrownBy(() -> pitchService.reorder(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Pitch not found");
  }

  // ========== Cycle propagation (PitchCycleAssignmentService centralization) ==========

  @Test
  void assignToCycle_ShouldDelegateToPitchCycleAssignmentService() {
    Pitch shapedPitch = Pitch.builder().id(1L).title("Shaped Pitch").appetiteDays(14)
        .status(PitchStatus.SHAPED).build();

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(shapedPitch));
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(shapedPitch);

    PitchDTO result = pitchService.assignToCycle(1L, 1L);

    assertThat(result).isNotNull();
    verify(pitchCycleAssignmentService).applyCycleToPitch(shapedPitch, testCycle);
    assertThat(shapedPitch.getStatus()).isEqualTo(PitchStatus.PENDING);
  }

  @Test
  void unassignFromCycle_ShouldDelegateToPitchCycleAssignmentServiceWithNullCycle() {
    testPitch.setStatus(PitchStatus.PENDING);

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    PitchDTO result = pitchService.unassignFromCycle(1L);

    assertThat(result).isNotNull();
    verify(pitchCycleAssignmentService).applyCycleToPitch(testPitch, null);
    assertThat(testPitch.getStatus()).isEqualTo(PitchStatus.SHAPED);
    assertThat(testPitch.getTeam()).isNull();
  }

  @Test
  void markAsShaped_ShouldDelegateToPitchCycleAssignmentServiceWithNullCycle() {
    Pitch draftPitch = Pitch.builder().id(1L).title("Draft Pitch").appetiteDays(10)
        .problemStatement("Some problem").status(PitchStatus.DRAFT).build();

    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(draftPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(draftPitch);

    PitchDTO result = pitchService.markAsShaped(1L);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(PitchStatus.SHAPED);
    verify(pitchCycleAssignmentService).applyCycleToPitch(draftPitch, null);
  }
}