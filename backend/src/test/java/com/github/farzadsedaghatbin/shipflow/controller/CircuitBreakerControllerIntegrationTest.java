package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN"})
class CircuitBreakerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CycleRepository cycleRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

    @Autowired
    private PersonRepository personRepository;

    private Cycle cycle;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        // Clean up
        workLogRepository.deleteAll();
        pitchRepository.deleteAll();
        cycleRepository.deleteAll();

        // Create test person for work logs
        testPerson = personRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> personRepository.save(Person.builder()
                        .name("Test Person")
                        .email("test@example.com")
                        .build()));

        // Create test cycle
        cycle = Cycle.builder()
                .name("Test Cycle 1")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(32))
                .isActive(true)
                .build();
        cycle = cycleRepository.save(cycle);
    }

    @Nested
    @DisplayName("Detect Overflow Pitches")
    class DetectOverflowTests {

        @Test
        @DisplayName("Should detect pitch at 100% budget utilization")
        void shouldDetectPitchAt100Percent() throws Exception {
            // Create pitch with 5 days appetite
            Pitch pitch = createPitch("Overbudget Pitch", 5);

            // Log 40 hours (5 days * 8 hours)
            createWorkLog(pitch, new BigDecimal("40.0"));

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/overflow", cycle.getId())
                            .param("threshold", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].pitchId", is(pitch.getId().intValue())))
                    .andExpect(jsonPath("$[0].pitchTitle", is("Overbudget Pitch")))
                    .andExpect(jsonPath("$[0].appetiteDays", is(5.0)))
                    .andExpect(jsonPath("$[0].hoursSpent", is(40.0)))
                    .andExpect(jsonPath("$[0].utilizationPercentage", is(100.0)))
                    .andExpect(jsonPath("$[0].isOverflowing", is(true)));
        }

        @Test
        @DisplayName("Should detect pitch at 80% threshold with custom threshold")
        void shouldDetectPitchAt80PercentThreshold() throws Exception {
            Pitch pitch = createPitch("Warning Zone Pitch", 5);
            createWorkLog(pitch, new BigDecimal("32.0")); // 80% of 40 hours

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/overflow", cycle.getId())
                            .param("threshold", "80"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].utilizationPercentage", is(80.0)))
                    .andExpect(jsonPath("$[0].isOverflowing", is(true)));
        }

        @Test
        @DisplayName("Should not detect pitch below threshold")
        void shouldNotDetectPitchBelowThreshold() throws Exception {
            Pitch pitch = createPitch("Healthy Pitch", 5);
            createWorkLog(pitch, new BigDecimal("20.0")); // 50% utilization

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/overflow", cycle.getId())
                            .param("threshold", "80"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should detect pitch severely over budget (150%)")
        void shouldDetectPitchSeverelyOverBudget() throws Exception {
            Pitch pitch = createPitch("Severely Over Pitch", 5);
            createWorkLog(pitch, new BigDecimal("60.0")); // 150% of 40 hours

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/overflow", cycle.getId())
                            .param("threshold", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].utilizationPercentage", is(150.0)))
                    .andExpect(jsonPath("$[0].overflowPercentage", is(50.0)));
        }

        @Test
        @DisplayName("Should use default threshold of 100 when not provided")
        void shouldUseDefaultThreshold() throws Exception {
            Pitch pitch = createPitch("Default Threshold Test", 5);
            createWorkLog(pitch, new BigDecimal("40.0"));

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/overflow", cycle.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Get Triggered Circuit Breakers")
    class GetTriggeredTests {

        @Test
        @DisplayName("Should return all triggered circuit breakers for cycle")
        void shouldReturnTriggeredCircuitBreakers() throws Exception {
            Pitch pitch1 = createPitch("Triggered Pitch 1", 5);
            pitch1.setIsCircuitBreakerTriggered(true);
            pitch1.setCircuitBreakerReason("Scope expansion");
            pitch1.setCircuitBreakerDate(LocalDateTime.now());
            pitchRepository.save(pitch1);

            Pitch pitch2 = createPitch("Triggered Pitch 2", 3);
            pitch2.setIsCircuitBreakerTriggered(true);
            pitch2.setCircuitBreakerReason("Unexpected complexity");
            pitchRepository.save(pitch2);

            // Create non-triggered pitch
            createPitch("Normal Pitch", 2);

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/triggered", cycle.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].isCircuitBreakerTriggered", is(true)))
                    .andExpect(jsonPath("$[1].isCircuitBreakerTriggered", is(true)));

        }

        @Test
        @DisplayName("Should return empty list when no triggered circuit breakers")
        void shouldReturnEmptyListWhenNoTriggered() throws Exception {
            createPitch("Normal Pitch 1", 5);
            createPitch("Normal Pitch 2", 3);

            mockMvc.perform(get("/api/circuit-breaker/cycle/{cycleId}/triggered", cycle.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Trigger Circuit Breaker")
    class TriggerCircuitBreakerTests {

        @Test
        @DisplayName("Should trigger circuit breaker on pitch")
        void shouldTriggerCircuitBreaker() throws Exception {
            Pitch pitch = createPitch("Overflowing Pitch", 5);
            createWorkLog(pitch, new BigDecimal("45.0"));

            String reason = "Pitch has exceeded appetite by 12.5% with unexpected API integration complexity";

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/trigger", pitch.getId())
                            .param("reason", reason))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isCircuitBreakerTriggered", is(true)))
                    .andExpect(jsonPath("$.circuitBreakerReason", is(reason)))
                    .andExpect(jsonPath("$.circuitBreakerDate", notNullValue()));
        }

        @Test
        @DisplayName("Should fail to trigger with empty reason")
        void shouldFailToTriggerWithEmptyReason() throws Exception {
            Pitch pitch = createPitch("Test Pitch", 5);

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/trigger", pitch.getId())
                            .param("reason", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to trigger non-existent pitch")
        void shouldFailToTriggerNonExistentPitch() throws Exception {
            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/trigger", 99999L)
                            .param("reason", "Test reason"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Kill Pitch")
    class KillPitchTests {

        @Test
        @DisplayName("Should kill pitch and set status to CANCELLED")
        void shouldKillPitch() throws Exception {
            Pitch pitch = createPitch("Doomed Pitch", 5);
            pitch.setIsCircuitBreakerTriggered(true);
            pitch.setCircuitBreakerReason("Over budget");
            pitchRepository.save(pitch);

            String reason = "Unable to cut scope enough to meet appetite. Core problem different than shaped.";

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/kill", pitch.getId())
                            .param("reason", reason))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")))
                    .andExpect(jsonPath("$.circuitBreakerReason", containsString("KILLED")))
                    .andExpect(jsonPath("$.circuitBreakerReason", containsString(reason)));
        }

        @Test
        @DisplayName("Should fail to kill with empty reason")
        void shouldFailToKillWithEmptyReason() throws Exception {
            Pitch pitch = createPitch("Test Pitch", 5);

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/kill", pitch.getId())
                            .param("reason", ""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Resolve Circuit Breaker")
    class ResolveCircuitBreakerTests {

        @Test
        @DisplayName("Should resolve circuit breaker and update status")
        void shouldResolveCircuitBreaker() throws Exception {
            Pitch pitch = createPitch("Recovering Pitch", 5);
            pitch.setIsCircuitBreakerTriggered(true);
            pitch.setCircuitBreakerReason("Was over budget");
            pitch.setCircuitBreakerDate(LocalDateTime.now());
            pitchRepository.save(pitch);

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/resolve", pitch.getId())
                            .param("newStatus", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isCircuitBreakerTriggered", is(false)))
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
        }

        @Test
        @DisplayName("Should fail to resolve with invalid status")
        void shouldFailToResolveWithInvalidStatus() throws Exception {
            Pitch pitch = createPitch("Test Pitch", 5);
            pitch.setIsCircuitBreakerTriggered(true);
            pitchRepository.save(pitch);

            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/resolve", pitch.getId())
                            .param("newStatus", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to resolve non-existent pitch")
        void shouldFailToResolveNonExistentPitch() throws Exception {
            mockMvc.perform(post("/api/circuit-breaker/pitch/{pitchId}/resolve", 99999L)
                            .param("newStatus", "IN_PROGRESS"))
                    .andExpect(status().isNotFound());
        }
    }

    // Helper methods
    private Pitch createPitch(String title, int appetiteDays) {
        Pitch pitch = new Pitch();
        pitch.setTitle(title);
        pitch.setDescription("Test pitch for circuit breaker");
        pitch.setCycle(cycle);
        pitch.setAppetiteDays(appetiteDays);
        pitch.setStatus(PitchStatus.IN_PROGRESS);
        return pitchRepository.save(pitch);
    }

    private void createWorkLog(Pitch pitch, BigDecimal hours) {
        WorkLog workLog = WorkLog.builder()
                .pitch(pitch)
                .person(testPerson) // Required field
                .hoursSpent(hours)
                .date(LocalDate.now())
                .build();
        workLogRepository.save(workLog);
    }
}
