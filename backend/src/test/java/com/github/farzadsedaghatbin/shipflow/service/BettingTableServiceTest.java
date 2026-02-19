package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BettingTableServiceTest {

  @Mock
  private BettingSlotRepository bettingSlotRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private TeamRepository teamRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private WorkLogRepository workLogRepository;

  @Mock
  private MessageService messageService;

  @Mock
  private CapacityConfigService capacityConfigService;

  @InjectMocks
  private BettingTableService bettingTableService;

  private Cycle testCycle;
  private Team testTeam;
  private Pitch shapedPitch;
  private Pitch pendingPitch;
  private BettingSlot testSlot;

  @BeforeEach
  void setUp() {
    // Setup messageService with lenient stubs to handle various error messages
    lenient().when(messageService.getMessage(anyString())).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      if (key.contains("no.teams"))
        return "No teams found";
      if (key.contains("already.assigned"))
        return "Pitch is already assigned";
      return key.replace("error.", "").replace(".", " ");
    });
    lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      if (key.contains("position.exists"))
        return "Slot already exists";
      if (key.contains("dates.invalid"))
        return "Slot dates must be within cycle dates";
      if (key.contains("resize.invalid"))
        return "Cannot resize slot: assigned pitch requires more space";
      if (key.contains("doesnt.fit"))
        return "Pitch requires 14 days but slot only has fewer days";
      return key.replace("error.", "").replace(".", " ");
    });

    testCycle = Cycle.builder().id(1L).name("Q1 2024").phase(CyclePhase.SHAPING_BUILDING).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();

    testTeam = Team.builder().id(1L).name("Alpha Team").cycle(testCycle).build();

    shapedPitch = Pitch.builder().id(1L).title("User Auth Feature").description("OAuth2 implementation")
        .appetiteDays(14).status(PitchStatus.SHAPED).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();

    pendingPitch = Pitch.builder().id(2L).title("Pending Feature").appetiteDays(7).status(PitchStatus.PENDING)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    testSlot = BettingSlot.builder().id(1L).cycle(testCycle).team(testTeam).position(0)
        .startDate(testCycle.getStartDate()).endDate(testCycle.getEndDate()).build();

    // Setup capacity config mock - default 8 hours/day
    lenient().when(capacityConfigService.calculatePitchAppetiteHours(any(Pitch.class))).thenReturn(112.0);
    lenient().when(capacityConfigService.getOrganizationDefaultHoursPerDay()).thenReturn(8.0);
  }

  @Test
  void getBettingTable_ShouldReturnFullBettingTableView() {
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
    // Shape Up workflow: shaped pitches have no cycle, so we use findBettingCandidates()
    when(pitchRepository.findBettingCandidates()).thenReturn(Arrays.asList(shapedPitch));
    when(bettingSlotRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testSlot));
    when(teamRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testTeam));
    when(workLogRepository.getTotalHoursByPitchId(any())).thenReturn(0.0);

    BettingTableDTO result = bettingTableService.getBettingTable(1L);

    assertThat(result).isNotNull();
    assertThat(result.getCycleId()).isEqualTo(1L);
    assertThat(result.getCycleName()).isEqualTo("Q1 2024");
    assertThat(result.getShapedPitches()).hasSize(1);
    assertThat(result.getTeamTracks()).hasSize(1);
    assertThat(result.getTeamTracks().get(0).getTeamName()).isEqualTo("Alpha Team");
  }

  @Test
  void getBettingTable_WhenCycleNotFound_ShouldThrowException() {
    when(cycleRepository.findByIdWithProject(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bettingTableService.getBettingTable(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Cycle not found");
  }

  @Test
  void getBettingTable_ShouldExcludeAlreadyAssignedPitches() {
    // Assign the shaped pitch to a slot
    testSlot.setPitch(shapedPitch);

    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
    // Shape Up workflow: shaped pitches have no cycle, so we use findBettingCandidates()
    when(pitchRepository.findBettingCandidates()).thenReturn(Arrays.asList(shapedPitch));
    when(bettingSlotRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testSlot));
    when(teamRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testTeam));
    lenient().when(workLogRepository.getTotalHoursByPitchId(any())).thenReturn(0.0);

    BettingTableDTO result = bettingTableService.getBettingTable(1L);

    assertThat(result.getShapedPitches()).isEmpty();
  }

  @Test
  void createSlot_ShouldCreateNewSlot() {
    CreateBettingSlotRequest request = CreateBettingSlotRequest.builder().cycleId(1L).teamId(1L).position(1)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(3)).notes("Second slot").build();

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(bettingSlotRepository.findByCycleIdAndTeamIdAndPosition(1L, 1L, 1)).thenReturn(Optional.empty());
    when(bettingSlotRepository.save(any(BettingSlot.class))).thenAnswer(invocation -> {
      BettingSlot slot = invocation.getArgument(0);
      slot.setId(2L);
      return slot;
    });

    BettingSlotDTO result = bettingTableService.createSlot(request);

    assertThat(result).isNotNull();
    assertThat(result.getPosition()).isEqualTo(1);
    assertThat(result.getNotes()).isEqualTo("Second slot");
    verify(bettingSlotRepository).save(any(BettingSlot.class));
  }

  @Test
  void createSlot_WithDuplicatePosition_ShouldThrowException() {
    CreateBettingSlotRequest request = CreateBettingSlotRequest.builder().cycleId(1L).teamId(1L).position(0) // Same
        // as
        // testSlot
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(3)).build();

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(testTeam));
    when(bettingSlotRepository.findByCycleIdAndTeamIdAndPosition(1L, 1L, 0)).thenReturn(Optional.of(testSlot));

    assertThatThrownBy(() -> bettingTableService.createSlot(request)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void assignPitchToSlot_ShouldAssignPitchAndUpdateStatus() {
    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(shapedPitch));
    when(bettingSlotRepository.findByPitchId(1L)).thenReturn(Optional.empty());
    when(pitchRepository.save(any(Pitch.class))).thenReturn(shapedPitch);
    when(bettingSlotRepository.save(any(BettingSlot.class))).thenReturn(testSlot);

    BettingSlotDTO result = bettingTableService.assignPitchToSlot(1L, 1L);

    assertThat(result).isNotNull();
    verify(pitchRepository).save(argThat(pitch ->
        pitch.getStatus() == PitchStatus.STARTED && pitch.getCycle() != null && pitch.getCycle().getId().equals(1L)));
  }

  @Test
  void assignPitchToSlot_WhenPitchTooLarge_ShouldThrowException() {
    // Create a slot that's too small
    BettingSlot smallSlot = BettingSlot.builder().id(2L).cycle(testCycle).team(testTeam).position(1)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5)) // Only 5 days
        .build();

    when(bettingSlotRepository.findById(2L)).thenReturn(Optional.of(smallSlot));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(shapedPitch)); // 14 days
    when(bettingSlotRepository.findByPitchId(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> bettingTableService.assignPitchToSlot(2L, 1L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("requires 14 days");
  }

  @Test
  void assignPitchToSlot_WhenPitchAlreadyAssigned_ShouldThrowException() {
    BettingSlot anotherSlot = BettingSlot.builder().id(2L).pitch(shapedPitch).build();

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(shapedPitch));
    when(bettingSlotRepository.findByPitchId(1L)).thenReturn(Optional.of(anotherSlot));

    assertThatThrownBy(() -> bettingTableService.assignPitchToSlot(1L, 1L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("already assigned");
  }

  @Test
  void removePitchFromSlot_ShouldClearCycleButPreserveStatus() {
    testSlot.setPitch(shapedPitch);
    shapedPitch.setStatus(PitchStatus.STARTED);

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(shapedPitch);
    when(bettingSlotRepository.save(any(BettingSlot.class))).thenAnswer(invocation -> {
      BettingSlot slot = invocation.getArgument(0);
      return slot;
    });

    BettingSlotDTO result = bettingTableService.removePitchFromSlot(1L);

    assertThat(result.getPitchId()).isNull();
    // Status must NOT be reverted — pitch keeps whatever status it had (STARTED, IN_PROGRESS, etc.)
    // Only the cycle assignment is cleared.
    verify(pitchRepository).save(argThat(pitch ->
        pitch.getStatus() == PitchStatus.STARTED && pitch.getCycle() == null));
  }

  @Test
  void updateSlotDates_ShouldUpdateDates() {
    LocalDate newStart = LocalDate.now().plusDays(7);
    LocalDate newEnd = LocalDate.now().plusWeeks(4);

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
    when(bettingSlotRepository.save(any(BettingSlot.class))).thenAnswer(invocation -> {
      BettingSlot slot = invocation.getArgument(0);
      return slot;
    });

    BettingSlotDTO result = bettingTableService.updateSlotDates(1L, newStart, newEnd);

    assertThat(result.getStartDate()).isEqualTo(newStart);
    assertThat(result.getEndDate()).isEqualTo(newEnd);
  }

  @Test
  void updateSlotDates_WhenOutsideCycle_ShouldThrowException() {
    LocalDate invalidEnd = LocalDate.now().plusWeeks(10); // Beyond cycle end

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));

    assertThatThrownBy(() -> bettingTableService.updateSlotDates(1L, LocalDate.now(), invalidEnd))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("within cycle dates");
  }

  @Test
  void updateSlotDates_WhenAssignedPitchWontFit_ShouldThrowException() {
    testSlot.setPitch(shapedPitch); // 14 days appetite
    LocalDate newEnd = LocalDate.now().plusDays(5); // Only 5 days

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));

    assertThatThrownBy(() -> bettingTableService.updateSlotDates(1L, LocalDate.now(), newEnd))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("Cannot resize slot");
  }

  @Test
  void deleteSlot_ShouldDeleteAndRevertPitchStatus() {
    testSlot.setPitch(shapedPitch);
    shapedPitch.setStatus(PitchStatus.STARTED);

    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(shapedPitch);

    bettingTableService.deleteSlot(1L);

    verify(bettingSlotRepository).delete(testSlot);
    verify(pitchRepository).save(argThat(pitch -> pitch.getStatus() == PitchStatus.SHAPED));
  }

  @Test
  void canPitchFitInSlot_WhenFits_ShouldReturnTrue() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(shapedPitch)); // 14 days
    when(bettingSlotRepository.findById(1L)).thenReturn(Optional.of(testSlot)); // 42 days

    boolean result = bettingTableService.canPitchFitInSlot(1L, 1L);

    assertThat(result).isTrue();
  }

  @Test
  void canPitchFitInSlot_WhenDoesNotFit_ShouldReturnFalse() {
    BettingSlot smallSlot = BettingSlot.builder().id(2L).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(5)).build();

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(shapedPitch)); // 14 days
    when(bettingSlotRepository.findById(2L)).thenReturn(Optional.of(smallSlot)); // 5 days

    boolean result = bettingTableService.canPitchFitInSlot(1L, 2L);

    assertThat(result).isFalse();
  }

  @Test
  void generateSlotsForCycle_ShouldCreateSlotsForTeamsWithoutSlots() {
    Team team2 = Team.builder().id(2L).name("Beta Team").cycle(testCycle).build();

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findByCycleId(1L)).thenReturn(Arrays.asList(testTeam, team2));
    when(bettingSlotRepository.findByCycleIdAndTeamId(1L, 1L)).thenReturn(Arrays.asList(testSlot)); // Alpha has
    // slots
    when(bettingSlotRepository.findByCycleIdAndTeamId(1L, 2L)).thenReturn(Arrays.asList()); // Beta has no slots
    when(bettingSlotRepository.save(any(BettingSlot.class))).thenAnswer(invocation -> {
      BettingSlot slot = invocation.getArgument(0);
      slot.setId(3L);
      return slot;
    });

    List<BettingSlotDTO> result = bettingTableService.generateSlotsForCycle(1L);

    assertThat(result).hasSize(1);
    verify(bettingSlotRepository, times(1)).save(any(BettingSlot.class));
  }

  @Test
  void generateSlotsForCycle_WhenNoTeams_ShouldThrowException() {
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(teamRepository.findByCycleId(1L)).thenReturn(Arrays.asList());

    assertThatThrownBy(() -> bettingTableService.generateSlotsForCycle(1L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No teams found");
  }
}
