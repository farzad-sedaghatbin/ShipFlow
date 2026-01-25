package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingActionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
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

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Cycle testCycle;
    private Pitch testPitch;
    private Meeting testMeeting;
    private Person testPerson;
    private Retrospective testRetrospective;
    private Project testProject;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        retrospectiveRepository.deleteAll();
        pitchRepository.deleteAll();
        cycleRepository.deleteAll();
        projectRepository.deleteAll();
        permissionRepository.deleteAll();
        userRepository.deleteAll();
        personRepository.deleteAll();
        
        // Create test project
        testProject = Project.builder()
                .name("Test Project")
                .projectKey("TEST")
                .build();
        testProject = projectRepository.save(testProject);
        
        testCycle = Cycle.builder()
                .name("Test Cycle")
                .phase(CyclePhase.BUILD)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .isActive(true)
                .project(testProject)
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
        
        testPerson = Person.builder()
                .name("Test Person")
                .email("test@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testPerson = personRepository.save(testPerson);

        User testUser = User.builder()
                .username("meeting-test-user")
                .password("password")
                .role(UserRole.MEMBER)
                .person(testPerson)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);
        
        permissionRepository.save(Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.PITCH)
                .permissionType(PermissionType.READ)
                .build());
        permissionRepository.save(Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.PITCH)
                .permissionType(PermissionType.CREATE)
                .build());
        permissionRepository.save(Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.PITCH)
                .permissionType(PermissionType.UPDATE)
                .build());
        permissionRepository.save(Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.PITCH)
                .permissionType(PermissionType.DELETE)
                .build());
        
        testRetrospective = Retrospective.builder()
                .title("Test Retrospective")
                .status(RetroStatus.OPEN)
                .cycle(testCycle)
                .project(testProject)
                .createdAt(LocalDateTime.now())
                .build();
        testRetrospective = retrospectiveRepository.save(testRetrospective);

        testMeeting = Meeting.builder()
                .type(MeetingType.KICKOFF)
                .dateHeld(LocalDate.now())
                .dorReady(false)
                .dodReady(false)
                .pitch(testPitch)
                .notes("Test notes")
                .decisions("Test decisions")
                .attendees("John Doe, Jane Smith")
                .build();
        testMeeting = meetingRepository.save(testMeeting);
    }

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getAllMeetings_ShouldReturnMeetings() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type", is("KICKOFF")));
    }

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingById_WhenExists_ShouldReturnMeeting() throws Exception {
        mockMvc.perform(get("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testMeeting.getId().intValue())))
                .andExpect(jsonPath("$.type", is("KICKOFF")));
    }

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/meetings/{id}", 9999L))
                .andExpect(status().isBadRequest());
    }

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
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

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
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

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void deleteMeeting_WhenExists_ShouldDeleteMeeting() throws Exception {
        mockMvc.perform(delete("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meetings/{id}", testMeeting.getId()))
                .andExpect(status().isBadRequest());
    }

    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingsByPitch_ShouldReturnMeetingsForPitch() throws Exception {
        mockMvc.perform(get("/api/meetings/pitch/{pitchId}", testPitch.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("KICKOFF")));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getAllMeetingsPaginated_ShouldReturnPagedMeetings() throws Exception {
        // Create additional meetings for pagination testing
        for (int i = 0; i < 25; i++) {
            Meeting meeting = Meeting.builder()
                    .type(MeetingType.STANDUP)
                    .dateHeld(LocalDate.now().minusDays(i))
                    .dorReady(false)
                    .dodReady(false)
                    .pitch(testPitch)
                    .notes("Meeting " + i)
                    .build();
            meetingRepository.save(meeting);
        }

        mockMvc.perform(get("/api/meetings/paginated")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "dateHeld")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(25)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingsWithFilters_ByType_ShouldReturnFilteredMeetings() throws Exception {
        // Create meetings of different types
        Meeting demo = Meeting.builder()
                .type(MeetingType.DEMO)
                .dateHeld(LocalDate.now())
                .dorReady(true)
                .dodReady(true)
                .pitch(testPitch)
                .build();
        meetingRepository.save(demo);

        mockMvc.perform(get("/api/meetings/filter")
                        .param("types", "DEMO")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", everyItem(is("DEMO"))));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingsWithFilters_ByDateRange_ShouldReturnFilteredMeetings() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/meetings/filter")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void getMeetingsWithFilters_ByDorReady_ShouldReturnFilteredMeetings() throws Exception {
        // Create meeting with DOR ready
        Meeting dorReadyMeeting = Meeting.builder()
                .type(MeetingType.KICKOFF)
                .dateHeld(LocalDate.now())
                .dorReady(true)
                .dodReady(false)
                .pitch(testPitch)
                .build();
        meetingRepository.save(dorReadyMeeting);

        mockMvc.perform(get("/api/meetings/filter")
                        .param("dorReady", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].dorReady", everyItem(is(true))));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void createMeeting_WithActionItems_ShouldCreateMeetingWithActions() throws Exception {
        List<MeetingActionDTO> actions = new ArrayList<>();
        MeetingActionDTO action1 = MeetingActionDTO.builder()
                .description("Complete documentation")
                .assignedToId(testPerson.getId())
                .status(ActionStatus.OPEN)
                .dueDate(LocalDate.now().plusDays(7))
                .notes("High priority")
                .build();
        actions.add(action1);

        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.RETROSPECTIVE)
                .dateHeld(LocalDate.now())
                .pitchId(testPitch.getId())
                .dorReady(true)
                .dodReady(true)
                .notes("Retro notes")
                .decisions("Decision to improve testing")
                .attendees("Team A, Team B")
                .actions(actions)
                .build();

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("RETROSPECTIVE")))
                .andExpect(jsonPath("$.decisions", is("Decision to improve testing")))
                .andExpect(jsonPath("$.attendees", is("Team A, Team B")))
                .andExpect(jsonPath("$.actions", hasSize(1)))
                .andExpect(jsonPath("$.actions[0].description", is("Complete documentation")))
                .andExpect(jsonPath("$.actions[0].assignedToName", is("Test Person")))
                .andExpect(jsonPath("$.actions[0].status", is("OPEN")));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void createMeeting_WithRetrospective_ShouldLinkRetrospective() throws Exception {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.RETROSPECTIVE)
                .dateHeld(LocalDate.now())
                .pitchId(testPitch.getId())
                .retrospectiveId(testRetrospective.getId())
                .dorReady(true)
                .dodReady(true)
                .notes("Retrospective meeting notes")
                .decisions("Action plan for next cycle")
                .build();

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.retrospectiveId", is(testRetrospective.getId().intValue())))
                .andExpect(jsonPath("$.retrospectiveTitle", is("Test Retrospective")));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void updateMeeting_WithActionItems_ShouldUpdateActions() throws Exception {
        List<MeetingActionDTO> actions = new ArrayList<>();
        MeetingActionDTO action1 = MeetingActionDTO.builder()
                .description("Update tests")
                .assignedToId(testPerson.getId())
                .status(ActionStatus.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(3))
                .build();
        MeetingActionDTO action2 = MeetingActionDTO.builder()
                .description("Review code")
                .status(ActionStatus.OPEN)
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        actions.add(action1);
        actions.add(action2);

        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.DEMO)
                .dateHeld(LocalDate.now())
                .pitchId(testPitch.getId())
                .dorReady(true)
                .dodReady(true)
                .notes("Updated meeting")
                .decisions("New decisions")
                .attendees("Updated attendees")
                .actions(actions)
                .build();

        mockMvc.perform(put("/api/meetings/{id}", testMeeting.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions", is("New decisions")))
                .andExpect(jsonPath("$.attendees", is("Updated attendees")))
                .andExpect(jsonPath("$.actions", hasSize(2)))
                .andExpect(jsonPath("$.actions[0].status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.actions[1].description", is("Review code")));
    }
    
    @WithMockUser(username = "meeting-test-user", roles = "MEMBER")
    @Test
    void createMeeting_WithMultipleActionStatuses_ShouldHandleAllStatuses() throws Exception {
        List<MeetingActionDTO> actions = new ArrayList<>();
        actions.add(MeetingActionDTO.builder()
                .description("Task 1")
                .status(ActionStatus.OPEN)
                .build());
        actions.add(MeetingActionDTO.builder()
                .description("Task 2")
                .status(ActionStatus.IN_PROGRESS)
                .build());
        actions.add(MeetingActionDTO.builder()
                .description("Task 3")
                .status(ActionStatus.COMPLETED)
                .build());
        actions.add(MeetingActionDTO.builder()
                .description("Task 4")
                .status(ActionStatus.CANCELLED)
                .build());

        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .type(MeetingType.STANDUP)
                .dateHeld(LocalDate.now())
                .dorReady(false)
                .dodReady(false)
                .actions(actions)
                .build();

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actions", hasSize(4)))
                .andExpect(jsonPath("$.actions[0].status", is("OPEN")))
                .andExpect(jsonPath("$.actions[1].status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.actions[2].status", is("COMPLETED")))
                .andExpect(jsonPath("$.actions[3].status", is("CANCELLED")));
    }
}
