package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateBettingSlotRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class BettingTableControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private BettingSlotRepository bettingSlotRepository;

  private Cycle testCycle;
  private Team testTeam;
  private Pitch shapedPitch;
  private BettingSlot testSlot;

  @BeforeEach
  void setUp() {
    bettingSlotRepository.deleteAll();
    pitchRepository.deleteAll();
    teamRepository.deleteAll();
    cycleRepository.deleteAll();

    testCycle = Cycle.builder().name("Test Cycle Q1").phase(CyclePhase.SHAPING_BUILDING).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);

    testTeam = Team.builder().name("Alpha Team").build();
    testTeam = teamRepository.save(testTeam);

    // Membership pitch: links testTeam to testCycle for findByCycleId to work
    Pitch membershipPitch = Pitch.builder().title("Alpha Team Membership").description("Team link").appetiteDays(42)
        .cycle(testCycle).team(testTeam).status(PitchStatus.SHAPED).build();
    pitchRepository.save(membershipPitch);

    // Shape Up workflow: shaped pitches ready for betting have NO cycle assigned yet
    shapedPitch = Pitch.builder().title("User Authentication").description("Implement OAuth2 login flow")
        .appetiteDays(14).status(PitchStatus.SHAPED).cycle(null).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    shapedPitch = pitchRepository.save(shapedPitch);

    testSlot = BettingSlot.builder().cycle(testCycle).team(testTeam).position(0).startDate(testCycle.getStartDate())
        .endDate(testCycle.getEndDate()).build();
    testSlot = bettingSlotRepository.save(testSlot);
  }

  @Test
  void getBettingTable_ShouldReturnBettingTableForCycle() throws Exception {
    mockMvc.perform(get("/api/betting/cycle/{cycleId}", testCycle.getId())).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.cycleId", is(testCycle.getId().intValue())))
        .andExpect(jsonPath("$.cycleName", is("Test Cycle Q1")))
        .andExpect(jsonPath("$.shapedPitches", hasSize(1)))
        .andExpect(jsonPath("$.shapedPitches[0].title", is("User Authentication")))
        .andExpect(jsonPath("$.teamTracks", hasSize(1)))
        .andExpect(jsonPath("$.teamTracks[0].teamName", is("Alpha Team")));
  }

  @Test
  void getBettingTable_WhenCycleNotFound_ShouldReturn400() throws Exception {
    mockMvc.perform(get("/api/betting/cycle/{cycleId}", 9999L)).andExpect(status().isBadRequest());
  }

  @Test
  void getShapedPitches_ShouldReturnAvailablePitches() throws Exception {
    mockMvc.perform(get("/api/betting/shaped-pitches")).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[0].status", is("SHAPED")));
  }

  @Test
  void createSlot_WithValidData_ShouldCreateSlot() throws Exception {
    CreateBettingSlotRequest request = CreateBettingSlotRequest.builder().cycleId(testCycle.getId())
        .teamId(testTeam.getId()).position(1).startDate(LocalDate.now().plusWeeks(2))
        .endDate(LocalDate.now().plusWeeks(4)).notes("Second slot for overflow work").build();

    mockMvc.perform(post("/api/betting/slots").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
        .andExpect(jsonPath("$.position", is(1))).andExpect(jsonPath("$.teamName", is("Alpha Team")))
        .andExpect(jsonPath("$.notes", is("Second slot for overflow work")));
  }

  @Test
  void createSlot_WithDuplicatePosition_ShouldReturn400() throws Exception {
    CreateBettingSlotRequest request = CreateBettingSlotRequest.builder().cycleId(testCycle.getId())
        .teamId(testTeam.getId()).position(0) // Same position as testSlot
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(2)).build();

    mockMvc.perform(post("/api/betting/slots").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void generateSlots_ShouldCreateSlotsForAllTeams() throws Exception {
    // Create a second team without slots
    Team secondTeam = Team.builder().name("Beta Team").build();
    teamRepository.save(secondTeam);

    // Link secondTeam to testCycle via a pitch so findByCycleId includes it
    Pitch secondMembershipPitch = Pitch.builder().title("Beta Team Membership").description("Team link").appetiteDays(42)
        .cycle(testCycle).team(secondTeam).status(PitchStatus.SHAPED).build();
    pitchRepository.save(secondMembershipPitch);

    mockMvc.perform(post("/api/betting/cycle/{cycleId}/generate-slots", testCycle.getId()))
        .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1))) // Only Beta Team should get a
        // slot
        .andExpect(jsonPath("$[0].teamName", is("Beta Team")));
  }

  @Test
  void assignPitchToSlot_ShouldAssignSuccessfully() throws Exception {
    mockMvc.perform(patch("/api/betting/slots/{slotId}/assign/{pitchId}", testSlot.getId(), shapedPitch.getId()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.pitchId", is(shapedPitch.getId().intValue())))
        .andExpect(jsonPath("$.pitchTitle", is("User Authentication")));
  }

  @Test
  void assignPitchToSlot_WhenPitchTooLarge_ShouldReturn400() throws Exception {
    // Create a pitch that's too large for a small slot
    Pitch largePitch = Pitch.builder().title("Huge Feature").description("Way too big").appetiteDays(100) // 100
        // days
        // won't
        // fit
        // in 6
        // weeks
        .status(PitchStatus.SHAPED).cycle(testCycle).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
    largePitch = pitchRepository.save(largePitch);

    // Create a small slot
    BettingSlot smallSlot = BettingSlot.builder().cycle(testCycle).team(testTeam).position(2)
        .startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(1)) // Only 7 days
        .build();
    smallSlot = bettingSlotRepository.save(smallSlot);

    mockMvc.perform(patch("/api/betting/slots/{slotId}/assign/{pitchId}", smallSlot.getId(), largePitch.getId()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removePitchFromSlot_ShouldRemoveSuccessfully() throws Exception {
    // First assign a pitch
    testSlot.setPitch(shapedPitch);
    bettingSlotRepository.save(testSlot);

    mockMvc.perform(patch("/api/betting/slots/{slotId}/unassign", testSlot.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.pitchId").doesNotExist());
  }

  @Test
  void updateSlotDates_ShouldUpdateSuccessfully() throws Exception {
    LocalDate newStart = LocalDate.now().plusDays(7);
    LocalDate newEnd = LocalDate.now().plusWeeks(5);

    mockMvc.perform(patch("/api/betting/slots/{slotId}/dates", testSlot.getId())
        .param("startDate", newStart.toString()).param("endDate", newEnd.toString())).andExpect(status().isOk())
        .andExpect(jsonPath("$.startDate", is(newStart.toString())))
        .andExpect(jsonPath("$.endDate", is(newEnd.toString())));
  }

  @Test
  void updateSlotDates_WhenOutsideCycle_ShouldReturn400() throws Exception {
    LocalDate invalidEnd = LocalDate.now().plusWeeks(10); // Beyond cycle end

    mockMvc.perform(patch("/api/betting/slots/{slotId}/dates", testSlot.getId())
        .param("startDate", LocalDate.now().toString()).param("endDate", invalidEnd.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteSlot_ShouldDeleteSuccessfully() throws Exception {
    mockMvc.perform(delete("/api/betting/slots/{slotId}", testSlot.getId())).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/betting/cycle/{cycleId}", testCycle.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.teamTracks[0].slots", hasSize(0)));
  }

  @Test
  void canPitchFitInSlot_WhenFits_ShouldReturnTrue() throws Exception {
    mockMvc.perform(get("/api/betting/slots/{slotId}/can-fit/{pitchId}", testSlot.getId(), shapedPitch.getId()))
        .andExpect(status().isOk()).andExpect(content().string("true"));
  }

  @Test
  void canPitchFitInSlot_WhenDoesNotFit_ShouldReturnFalse() throws Exception {
    // Create a pitch that's too large
    Pitch largePitch = Pitch.builder().title("Large Feature").appetiteDays(100).status(PitchStatus.SHAPED)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    largePitch = pitchRepository.save(largePitch);

    mockMvc.perform(get("/api/betting/slots/{slotId}/can-fit/{pitchId}", testSlot.getId(), largePitch.getId()))
        .andExpect(status().isOk()).andExpect(content().string("false"));
  }

  @Test
  void assignPitchToSlot_ShouldChangeStatusToStarted() throws Exception {
    mockMvc.perform(patch("/api/betting/slots/{slotId}/assign/{pitchId}", testSlot.getId(), shapedPitch.getId()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.pitchStatus", is("STARTED")));
  }
}
