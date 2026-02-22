package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingActionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock
  private MeetingRepository meetingRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private RetrospectiveRepository retrospectiveRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  private MeetingService meetingService;

  private Meeting testMeeting;
  private Pitch testPitch;
  private CreateMeetingRequest testRequest;

  @BeforeEach
  void setUp() {
    testPitch = Pitch.builder().id(1L).title("Test Pitch").build();

    testMeeting = Meeting.builder().id(1L).pitch(testPitch).type("KICKOFF").dateHeld(LocalDate.now())
        .dorReady(true).dodReady(false).notes("Test meeting notes").build();

    testRequest = new CreateMeetingRequest();
    testRequest.setPitchId(1L);
    testRequest.setType("KICKOFF");
    testRequest.setDateHeld(LocalDate.now());
    testRequest.setDorReady(true);
    testRequest.setDodReady(false);
    testRequest.setNotes("Test meeting notes");
  }

  @Test
  void getAllMeetings_ShouldReturnAllMeetings() {
    when(meetingRepository.findAll()).thenReturn(Arrays.asList(testMeeting));

    List<MeetingDTO> result = meetingService.getAllMeetings();

    assertThat(result).hasSize(1);
  }

  @Test
  void getMeetingById_WhenExists_ShouldReturnMeeting() {
    when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

    MeetingDTO result = meetingService.getMeetingById(1L);

    assertThat(result).isNotNull();
  }

  @Test
  void getMeetingById_WhenNotExists_ShouldThrowException() {
    when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meetingService.getMeetingById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Meeting not found");
  }

  @Test
  void createMeeting_ShouldSaveMeeting() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    MeetingDTO result = meetingService.createMeeting(testRequest);

    assertThat(result).isNotNull();
    verify(meetingRepository).save(any(Meeting.class));
  }

  @Test
  void updateMeeting_WhenExists_ShouldUpdateMeeting() {
    when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    testRequest.setNotes("Updated notes");
    MeetingDTO result = meetingService.updateMeeting(1L, testRequest);

    assertThat(result).isNotNull();
    verify(meetingRepository).save(any(Meeting.class));
  }

  @Test
  void deleteMeeting_ShouldCallRepository() {
    doNothing().when(meetingRepository).deleteById(1L);

    meetingService.deleteMeeting(1L);

    verify(meetingRepository).deleteById(1L);
  }

  @Test
  void getMeetingsByPitchId_ShouldReturnMeetings() {
    when(meetingRepository.findByPitchId(1L)).thenReturn(Arrays.asList(testMeeting));

    List<MeetingDTO> result = meetingService.getMeetingsByPitchId(1L);

    assertThat(result).hasSize(1);
    verify(meetingRepository).findByPitchId(1L);
  }

  @Test
  void getMeetingsByType_ShouldReturnMeetings() {
    when(meetingRepository.findByType("KICKOFF")).thenReturn(Arrays.asList(testMeeting));

    List<MeetingDTO> result = meetingService.getMeetingsByType("KICKOFF");

    assertThat(result).hasSize(1);
    verify(meetingRepository).findByType("KICKOFF");
  }

  @Test
  void getAllMeetingsPaginated_ShouldReturnPagedMeetings() {
    Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dateHeld"));
    Page<Meeting> page = new PageImpl<>(Arrays.asList(testMeeting), pageable, 1);

    when(meetingRepository.findAll(any(Pageable.class))).thenReturn(page);

    Page<MeetingDTO> result = meetingService.getAllMeetingsPaginated(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(meetingRepository).findAll(pageable);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getMeetingsWithFilters_ShouldApplyFiltersAndReturnResults() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dateHeld"));
    Page<Meeting> page = new PageImpl<>(Arrays.asList(testMeeting), pageable, 1);

    when(meetingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<MeetingDTO> result = meetingService.getMeetingsWithFilters(null, null, 1L,
        Arrays.asList("KICKOFF"), null, null, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(meetingRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void createMeeting_WithActionItems_ShouldSaveMeetingWithActions() {
    Person testPerson = Person.builder().id(1L).name("Test Person").build();
    List<MeetingActionDTO> actions = new ArrayList<>();
    actions.add(MeetingActionDTO.builder().description("Test action").assignedToId(1L).status(ActionStatus.OPEN)
        .dueDate(LocalDate.now().plusDays(7)).build());

    testRequest.setActions(actions);
    testRequest.setDecisions("Test decisions");
    testRequest.setAttendees("Test attendees");

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    MeetingDTO result = meetingService.createMeeting(testRequest);

    assertThat(result).isNotNull();
    verify(meetingRepository, times(2)).save(any(Meeting.class));
    verify(personRepository).findById(1L);
  }

  @Test
  void createMeeting_WithRetrospective_ShouldLinkRetrospective() {
    Retrospective retro = Retrospective.builder().id(1L).title("Test Retro").build();

    testRequest.setRetrospectiveId(1L);

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(retrospectiveRepository.findById(1L)).thenReturn(Optional.of(retro));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    MeetingDTO result = meetingService.createMeeting(testRequest);

    assertThat(result).isNotNull();
    verify(retrospectiveRepository).findById(1L);
  }

  @Test
  void updateMeeting_WithActionItems_ShouldReplaceActions() {
    Meeting meetingWithActions = Meeting.builder().id(1L).pitch(testPitch).type("KICKOFF")
        .dateHeld(LocalDate.now()).dorReady(true).dodReady(false).notes("Test notes").actions(new ArrayList<>())
        .build();

    Person testPerson = Person.builder().id(1L).name("Test Person").build();
    List<MeetingActionDTO> newActions = new ArrayList<>();
    newActions.add(MeetingActionDTO.builder().description("Updated action").assignedToId(1L)
        .status(ActionStatus.IN_PROGRESS).build());

    testRequest.setActions(newActions);
    testRequest.setDecisions("Updated decisions");

    when(meetingRepository.findById(1L)).thenReturn(Optional.of(meetingWithActions));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(meetingWithActions);

    MeetingDTO result = meetingService.updateMeeting(1L, testRequest);

    assertThat(result).isNotNull();
    verify(meetingRepository).save(any(Meeting.class));
  }

  @Test
  void createMeeting_WithInvalidPitch_ShouldThrowException() {
    testRequest.setPitchId(999L);
    when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Pitch not found");
  }

  @Test
  void createMeeting_WithInvalidRetrospective_ShouldThrowException() {
    testRequest.setRetrospectiveId(999L);
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(retrospectiveRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Retrospective not found");
  }

  @Test
  void createMeeting_WithInvalidAssignee_ShouldThrowException() {
    List<MeetingActionDTO> actions = new ArrayList<>();
    actions.add(MeetingActionDTO.builder().description("Test action").assignedToId(999L).status(ActionStatus.OPEN)
        .build());

    testRequest.setActions(actions);
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(personRepository.findById(999L)).thenReturn(Optional.empty());
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Person not found");
  }

  @Test
  void createMeeting_WithDorDodItems_ShouldSaveWithChecklistItems() {
    // Create DOR/DOD checklist items
    List<MeetingDTO.MeetingChecklistItem> dorItems = List.of(
        MeetingDTO.MeetingChecklistItem.builder()
            .name("Problem Statement Defined")
            .description("Clear problem statement is documented")
            .isRequired(true)
            .isCompleted(true)
            .build(),
        MeetingDTO.MeetingChecklistItem.builder()
            .name("Stakeholders Identified")
            .description("All relevant stakeholders are identified")
            .isRequired(false)
            .isCompleted(false)
            .build()
    );
    
    List<MeetingDTO.MeetingChecklistItem> dodItems = List.of(
        MeetingDTO.MeetingChecklistItem.builder()
            .name("Solution Outlined")
            .description("High-level solution approach documented")
            .isRequired(true)
            .isCompleted(true)
            .build()
    );

    testRequest.setDorItems(dorItems);
    testRequest.setDodItems(dodItems);

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    MeetingDTO result = meetingService.createMeeting(testRequest);

    assertThat(result).isNotNull();
    verify(meetingRepository).save(any(Meeting.class));
  }

  @Test
  void getMeetingById_WithDorDodItems_ShouldReturnChecklistItems() {
    // Create meeting with DOR/DOD JSON
    String dorItemsJson = "[{\"name\":\"Test DOR\",\"description\":\"Test desc\",\"isRequired\":true,\"isCompleted\":true}]";
    String dodItemsJson = "[{\"name\":\"Test DOD\",\"description\":\"Test desc\",\"isRequired\":true,\"isCompleted\":false}]";
    
    Meeting meetingWithChecklist = Meeting.builder()
        .id(1L)
        .pitch(testPitch)
        .type("SHAPING")
        .dateHeld(LocalDate.now())
        .dorReady(true)
        .dodReady(false)
        .dorItemsJson(dorItemsJson)
        .dodItemsJson(dodItemsJson)
        .notes("Test notes")
        .build();

    when(meetingRepository.findById(1L)).thenReturn(Optional.of(meetingWithChecklist));

    MeetingDTO result = meetingService.getMeetingById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getDorItems()).isNotNull();
    assertThat(result.getDodItems()).isNotNull();
    assertThat(result.getDorItems()).hasSize(1);
    assertThat(result.getDodItems()).hasSize(1);
    assertThat(result.getDorItems().get(0).getName()).isEqualTo("Test DOR");
    assertThat(result.getDodItems().get(0).getName()).isEqualTo("Test DOD");
  }

  @Test
  void getMeetingForView_ShouldReturnOnlyCompletedItems() {
    // Given - meeting with mixed completed/uncompleted items
    MeetingDTO.MeetingChecklistItem completedDor = MeetingDTO.MeetingChecklistItem.builder()
        .id(1L).name("Completed DOR").isRequired(true).isCompleted(true).build();
    MeetingDTO.MeetingChecklistItem incompleteDor = MeetingDTO.MeetingChecklistItem.builder()
        .id(2L).name("Incomplete DOR").isRequired(false).isCompleted(false).build();
    MeetingDTO.MeetingChecklistItem completedDod = MeetingDTO.MeetingChecklistItem.builder()
        .id(3L).name("Completed DOD").isRequired(true).isCompleted(true).build();
    MeetingDTO.MeetingChecklistItem incompleteDod = MeetingDTO.MeetingChecklistItem.builder()
        .id(4L).name("Incomplete DOD").isRequired(false).isCompleted(false).build();

    List<MeetingDTO.MeetingChecklistItem> dorItems = Arrays.asList(completedDor, incompleteDor);
    List<MeetingDTO.MeetingChecklistItem> dodItems = Arrays.asList(completedDod, incompleteDod);

    try {
      testMeeting.setDorItemsJson(objectMapper.writeValueAsString(dorItems));
      testMeeting.setDodItemsJson(objectMapper.writeValueAsString(dodItems));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

    // When
    MeetingDTO result = meetingService.getMeetingForView(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getDorItems()).hasSize(1);
    assertThat(result.getDodItems()).hasSize(1);
    assertThat(result.getDorItems().get(0).getName()).isEqualTo("Completed DOR");
    assertThat(result.getDorItems().get(0).getIsCompleted()).isTrue();
    assertThat(result.getDodItems().get(0).getName()).isEqualTo("Completed DOD");
    assertThat(result.getDodItems().get(0).getIsCompleted()).isTrue();
  }

  @Test
  void getMeetingForView_WhenNoCompletedItems_ShouldReturnEmptyLists() {
    // Given - meeting with only incomplete items
    MeetingDTO.MeetingChecklistItem incompleteDor = MeetingDTO.MeetingChecklistItem.builder()
        .id(1L).name("Incomplete DOR").isCompleted(false).build();
    MeetingDTO.MeetingChecklistItem incompleteDod = MeetingDTO.MeetingChecklistItem.builder()
        .id(2L).name("Incomplete DOD").isCompleted(false).build();

    try {
      testMeeting.setDorItemsJson(objectMapper.writeValueAsString(List.of(incompleteDor)));
      testMeeting.setDodItemsJson(objectMapper.writeValueAsString(List.of(incompleteDod)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

    // When
    MeetingDTO result = meetingService.getMeetingForView(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getDorItems()).isEmpty();
    assertThat(result.getDodItems()).isEmpty();
  }

  @Test
  void getMeetingForView_WhenNotExists_ShouldThrowException() {
    // Given
    when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> meetingService.getMeetingForView(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Meeting not found");
  }
}
