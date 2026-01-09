package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateEvidenceRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
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
class EvidenceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CycleRepository cycleRepository;

    private Cycle testCycle;
    private Team testTeam;
    private Person testPerson;
    private Pitch testPitch;
    private Evidence testEvidence;

    @BeforeEach
    void setUp() {
        evidenceRepository.deleteAll();
        pitchRepository.deleteAll();
        teamRepository.deleteAll();
        cycleRepository.deleteAll();
        personRepository.deleteAll();
        
        testCycle = Cycle.builder()
                .name("Test Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .isActive(true)
                .build();
        testCycle = cycleRepository.save(testCycle);

        testTeam = Team.builder()
                .name("Test Team")
                .cycle(testCycle)
                .build();
        testTeam = teamRepository.save(testTeam);

        testPerson = Person.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testPerson = personRepository.save(testPerson);

        testPitch = Pitch.builder()
                .title("Test Pitch")
                .description("Test Description")
                .appetiteDays(6)
                .status(PitchStatus.IN_PROGRESS)
                .cycle(testCycle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testPitch = pitchRepository.save(testPitch);

        testEvidence = Evidence.builder()
                .description("Test Evidence Description")
                .date(LocalDate.now())
                .fileUrl("https://example.com/evidence")
                .person(testPerson)
                .pitch(testPitch)
                .build();
        testEvidence = evidenceRepository.save(testEvidence);
    }

    @Test
    void getAllEvidence_ShouldReturnEvidence() throws Exception {
        mockMvc.perform(get("/api/evidences"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].description", is("Test Evidence Description")));
    }

    @Test
    void getEvidenceById_WhenExists_ShouldReturnEvidence() throws Exception {
        mockMvc.perform(get("/api/evidences/{id}", testEvidence.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testEvidence.getId().intValue())))
                .andExpect(jsonPath("$.description", is("Test Evidence Description")));
    }

    @Test
    void getEvidenceById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/evidences/{id}", 9999L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvidence_WithValidData_ShouldCreateEvidence() throws Exception {
        CreateEvidenceRequest request = CreateEvidenceRequest.builder()
                .description("New Evidence Description")
                .date(LocalDate.now())
                .fileUrl("https://example.com/new-evidence")
                .personId(testPerson.getId())
                .pitchId(testPitch.getId())
                .build();

        mockMvc.perform(post("/api/evidences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("New Evidence Description")));
    }

    @Test
    void updateEvidence_WhenExists_ShouldUpdateEvidence() throws Exception {
        CreateEvidenceRequest request = CreateEvidenceRequest.builder()
                .description("Updated Evidence Description")
                .date(LocalDate.now())
                .fileUrl("https://example.com/updated-evidence")
                .personId(testPerson.getId())
                .pitchId(testPitch.getId())
                .build();

        mockMvc.perform(put("/api/evidences/{id}", testEvidence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated Evidence Description")));
    }

    @Test
    void deleteEvidence_WhenExists_ShouldDeleteEvidence() throws Exception {
        mockMvc.perform(delete("/api/evidences/{id}", testEvidence.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/evidences/{id}", testEvidence.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvidenceByPitch_ShouldReturnEvidenceForPitch() throws Exception {
        mockMvc.perform(get("/api/evidences/pitch/{pitchId}", testPitch.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Test Evidence Description")));
    }
}
