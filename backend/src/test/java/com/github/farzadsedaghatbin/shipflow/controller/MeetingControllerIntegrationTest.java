package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
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
class MeetingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private CycleRepository cycleRepository;

    private Cycle testCycle;
    private Pitch testPitch;
    private Meeting testMeeting;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        pitchRepository.deleteAll();
        cycleRepository.deleteAll();
        
        testCycle = Cycle.builder()
                .name("Test Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .isActive(true)
                .build();
        testCycle = cycleRepository.save(testCycle);

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

        testMeeting = Meeting.builder()
                .type(MeetingType.KICKOFF)
                .dateHeld(LocalDate.now())
                .dorReady(false)
                .dodReady(false)
                .pitch(testPitch)
                .notes("Test notes")
                .build();
        testMeeting = meetingRepository.save(testMeeting);
    }

    @Test
    void getAllMeetings_ShouldReturnMeetings() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type", is("KICKOFF")));
    }

    @Test
    void getMeetingById_WhenExists_ShouldReturnMeeting() throws Exception {
        mockMvc.perform(get("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testMeeting.getId().intValue())))
                .andExpect(jsonPath("$.type", is("KICKOFF")));
    }

    @Test
    void getMeetingById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/meetings/{id}", 9999L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMeeting_WithValidData_ShouldCreateMeeting() throws Exception {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.STANDUP)
                .dateHeld(LocalDate.now().plusDays(1))
                .pitchId(testPitch.getId())
                .dorReady(true)
                .dodReady(false)
                .notes("New meeting notes")
                .build();

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("STANDUP")));
    }

    @Test
    void updateMeeting_WhenExists_ShouldUpdateMeeting() throws Exception {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.DEMO)
                .dateHeld(LocalDate.now().plusDays(2))
                .pitchId(testPitch.getId())
                .dorReady(true)
                .dodReady(true)
                .notes("Updated notes")
                .build();

        mockMvc.perform(put("/api/meetings/{id}", testMeeting.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("DEMO")));
    }

    @Test
    void deleteMeeting_WhenExists_ShouldDeleteMeeting() throws Exception {
        mockMvc.perform(delete("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMeetingsByPitch_ShouldReturnMeetingsForPitch() throws Exception {
        mockMvc.perform(get("/api/meetings/pitch/{pitchId}", testPitch.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("KICKOFF")));
    }
}
