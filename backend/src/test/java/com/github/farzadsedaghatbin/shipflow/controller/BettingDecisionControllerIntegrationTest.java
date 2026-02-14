package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.betting.RecordBettingDecisionRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
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
@WithMockUser(username = "admin", roles = { "ADMIN" })
class BettingDecisionControllerIntegrationTest {

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
        private BettingDecisionRepository bettingDecisionRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ProjectRepository projectRepository;

        private Cycle testCycle;
        private Team testTeam;
        private Pitch shapedPitch;
        private User testUser;
        private Project testProject;

        @BeforeEach
        void setUp() {
                bettingDecisionRepository.deleteAll();
                pitchRepository.deleteAll();
                teamRepository.deleteAll();
                cycleRepository.deleteAll();

                // Create or get admin user (used by @WithMockUser)
                testUser = userRepository.findByUsername("admin")
                                .orElseGet(() -> userRepository.save(
                                                User.builder()
                                                                .username("admin")
                                                                .email("admin@test.com")
                                                                .password("testpassword")
                                                                .role(UserRole.ADMIN)
                                                                .isActive(true)
                                                                .build()));

                testProject = projectRepository.findAll().stream().findFirst()
                                .orElseGet(() -> projectRepository.save(
                                                Project.builder()
                                                                .name("Test Project")
                                                                .projectKey("BDTEST")
                                                                .description("Test")
                                                                .isActive(true)
                                                                .build()));

                testCycle = Cycle.builder()
                                .name("Test Cycle Q1")
                                .phase(CyclePhase.BETTING)
                                .startDate(LocalDate.now())
                                .endDate(LocalDate.now().plusWeeks(6))
                                .isActive(true)
                                .project(testProject)
                                .build();
                testCycle = cycleRepository.save(testCycle);

                testTeam = Team.builder()
                                .name("Alpha Team")
                                .cycle(testCycle)
                                .build();
                testTeam = teamRepository.save(testTeam);

                shapedPitch = Pitch.builder()
                                .title("User Authentication")
                                .description("Implement OAuth2 login flow")
                                .appetiteDays(14)
                                .status(PitchStatus.SHAPED)
                                .cycle(testCycle)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                shapedPitch = pitchRepository.save(shapedPitch);
        }

        // === Record Decision Tests ===

        @Test
        void recordDecision_WithValidData_ShouldCreateDecision() throws Exception {
                RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
                                .pitchId(shapedPitch.getId())
                                .cycleId(testCycle.getId())
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Strong business case and well-defined scope")
                                .consideredTeamId(testTeam.getId())
                                .availableCapacityDays(20)
                                .priorityScore(85)
                                .build();

                mockMvc.perform(post("/api/betting/decisions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.pitchId", is(shapedPitch.getId().intValue())))
                                .andExpect(jsonPath("$.cycleId", is(testCycle.getId().intValue())))
                                .andExpect(jsonPath("$.decision", is("COMMITTED")))
                                .andExpect(jsonPath("$.reason", is("Strong business case and well-defined scope")))
                                .andExpect(jsonPath("$.priorityScore", is(85)));
        }

        @Test
        void recordDecision_WhenRejected_ShouldRequireReason() throws Exception {
                RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
                                .pitchId(shapedPitch.getId())
                                .cycleId(testCycle.getId())
                                .decision(BettingDecisionType.REJECTED)
                                .reason("Scope too large for current capacity")
                                .appetiteComparisonNotes("Pitch requires 14 days but team has 10 days available")
                                .build();

                mockMvc.perform(post("/api/betting/decisions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.decision", is("REJECTED")))
                                .andExpect(jsonPath("$.reason", is("Scope too large for current capacity")));
        }

        @Test
        void recordDecision_WhenDeferred_ShouldRecordNeedsShapin() throws Exception {
                RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
                                .pitchId(shapedPitch.getId())
                                .cycleId(testCycle.getId())
                                .decision(BettingDecisionType.NEEDS_SHAPING)
                                .reason("Needs more technical investigation before betting")
                                .build();

                mockMvc.perform(post("/api/betting/decisions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.decision", is("NEEDS_SHAPING")));
        }

        @Test
        void recordDecision_WithMissingPitchId_ShouldReturn400() throws Exception {
                RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
                                .cycleId(testCycle.getId())
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Test reason")
                                .build();

                mockMvc.perform(post("/api/betting/decisions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void recordDecision_WithNonExistentPitch_ShouldReturn400() throws Exception {
                RecordBettingDecisionRequest request = RecordBettingDecisionRequest.builder()
                                .pitchId(99999L)
                                .cycleId(testCycle.getId())
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Test reason")
                                .build();

                mockMvc.perform(post("/api/betting/decisions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // === Pitch History Tests ===

        @Test
        void getPitchHistory_ShouldReturnAllDecisions() throws Exception {
                // Create a previous cycle for the old decision
                Cycle previousCycle = Cycle.builder()
                                .name("Previous Cycle")
                                .phase(CyclePhase.COOLDOWN)
                                .startDate(LocalDate.now().minusWeeks(12))
                                .endDate(LocalDate.now().minusWeeks(6))
                                .isActive(false)
                                .project(testProject)
                                .build();
                previousCycle = cycleRepository.save(previousCycle);

                // Create multiple decisions for the same pitch (in different cycles)
                BettingDecision decision1 = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(previousCycle)
                                .decision(BettingDecisionType.DEFERRED)
                                .reason("Not enough capacity this cycle")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now().minusDays(30))
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision1);

                BettingDecision decision2 = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Capacity available now")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision2);

                mockMvc.perform(get("/api/betting/decisions/pitch/{pitchId}/history", shapedPitch.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].decision", is("COMMITTED"))) // Most recent first
                                .andExpect(jsonPath("$[1].decision", is("DEFERRED")));
        }

        @Test
        void getMostRecentDecision_WhenExists_ShouldReturnLatest() throws Exception {
                BettingDecision decision = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Well-defined and ready")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision);

                mockMvc.perform(get("/api/betting/decisions/pitch/{pitchId}/latest", shapedPitch.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pitchId", is(shapedPitch.getId().intValue())))
                                .andExpect(jsonPath("$.decision", is("COMMITTED")));
        }

        @Test
        void getMostRecentDecision_WhenNotExists_ShouldReturn404() throws Exception {
                mockMvc.perform(get("/api/betting/decisions/pitch/{pitchId}/latest", shapedPitch.getId()))
                                .andExpect(status().isNotFound());
        }

        // === Cycle Decision Tests ===

        @Test
        void getCycleDecisions_ShouldReturnAllForCycle() throws Exception {
                Pitch anotherPitch = Pitch.builder()
                                .title("Another Feature")
                                .description("Test")
                                .appetiteDays(7)
                                .status(PitchStatus.SHAPED)
                                .cycle(testCycle)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                anotherPitch = pitchRepository.save(anotherPitch);

                BettingDecision decision1 = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Priority feature")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision1);

                BettingDecision decision2 = BettingDecision.builder()
                                .pitch(anotherPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.REJECTED)
                                .reason("Not aligned with cycle goals")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(anotherPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision2);

                mockMvc.perform(get("/api/betting/decisions/cycle/{cycleId}", testCycle.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void getCycleDecisionHistory_ShouldReturnDetailedStats() throws Exception {
                BettingDecision decision = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Committed pitch")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision);

                mockMvc.perform(get("/api/betting/decisions/cycle/{cycleId}/history", testCycle.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cycleId", is(testCycle.getId().intValue())))
                                .andExpect(jsonPath("$.cycleName", is("Test Cycle Q1")))
                                .andExpect(jsonPath("$.decisions", hasSize(1)))
                                .andExpect(jsonPath("$.committedCount", is(1)))
                                .andExpect(jsonPath("$.rejectedCount", is(0)));
        }

        @Test
        void getCommittedDecisions_ShouldOnlyReturnCommitted() throws Exception {
                BettingDecision committed = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Committed")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(committed);

                Pitch rejectedPitch = Pitch.builder()
                                .title("Rejected Feature")
                                .description("Test")
                                .appetiteDays(7)
                                .status(PitchStatus.SHAPED)
                                .cycle(testCycle)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                rejectedPitch = pitchRepository.save(rejectedPitch);

                BettingDecision rejected = BettingDecision.builder()
                                .pitch(rejectedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.REJECTED)
                                .reason("Rejected")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(rejectedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(rejected);

                mockMvc.perform(get("/api/betting/decisions/cycle/{cycleId}/committed", testCycle.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].decision", is("COMMITTED")));
        }

        @Test
        void getRejectedDecisions_ShouldOnlyReturnRejected() throws Exception {
                BettingDecision rejected = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.REJECTED)
                                .reason("Not a priority")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(rejected);

                mockMvc.perform(get("/api/betting/decisions/cycle/{cycleId}/rejected", testCycle.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].decision", is("REJECTED")))
                                .andExpect(jsonPath("$[0].reason", is("Not a priority")));
        }

        // === Existence Check Tests ===

        @Test
        void hasDecision_WhenExists_ShouldReturnTrue() throws Exception {
                BettingDecision decision = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Test")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision);

                mockMvc.perform(get("/api/betting/decisions/pitch/{pitchId}/exists", shapedPitch.getId())
                                .param("cycleId", testCycle.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));
        }

        @Test
        void hasDecision_WhenNotExists_ShouldReturnFalse() throws Exception {
                mockMvc.perform(get("/api/betting/decisions/pitch/{pitchId}/exists", shapedPitch.getId())
                                .param("cycleId", testCycle.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(content().string("false"));
        }

        // === Appetite Comparison Tests ===

        @Test
        void compareAppetite_ShouldReturnComparison() throws Exception {
                mockMvc.perform(get("/api/betting/decisions/compare/pitch/{pitchId}/team/{teamId}",
                                shapedPitch.getId(), testTeam.getId())
                                .param("cycleId", testCycle.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pitchId", is(shapedPitch.getId().intValue())))
                                .andExpect(jsonPath("$.teamId", is(testTeam.getId().intValue())))
                                .andExpect(jsonPath("$.requestedAppetiteDays", is(14)))
                                .andExpect(jsonPath("$.assessment").exists());
        }

        @Test
        void compareAppetiteAllTeams_ShouldReturnListSortedByFit() throws Exception {
                // Create a second team
                Team secondTeam = Team.builder()
                                .name("Beta Team")
                                .cycle(testCycle)
                                .build();
                teamRepository.save(secondTeam);

                mockMvc.perform(get("/api/betting/decisions/compare/pitch/{pitchId}/all-teams", shapedPitch.getId())
                                .param("cycleId", testCycle.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].pitchId", is(shapedPitch.getId().intValue())));
        }

        // === Project Decision Tests ===

        @Test
        void getProjectDecisions_ShouldReturnPaginatedResults() throws Exception {
                BettingDecision decision = BettingDecision.builder()
                                .pitch(shapedPitch)
                                .cycle(testCycle)
                                .decision(BettingDecisionType.COMMITTED)
                                .reason("Test")
                                .decidedBy(testUser)
                                .decidedAt(LocalDateTime.now())
                                .requestedAppetiteDays(shapedPitch.getAppetiteDays())
                                .build();
                bettingDecisionRepository.save(decision);

                mockMvc.perform(get("/api/betting/decisions/project/{projectId}", testProject.getId())
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.totalElements", is(1)));
        }
}
