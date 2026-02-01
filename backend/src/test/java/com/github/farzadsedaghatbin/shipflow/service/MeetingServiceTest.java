package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingActionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;

  @Mock private PitchRepository pitchRepository;

  @Mock private RetrospectiveRepository retrospectiveRepository;

  @Mock private PersonRepository personRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private MeetingService meetingService;

  private Meeting testMeeting;
  private Pitch testPitch;
  private CreateMeetingRequest testRequest;

  @BeforeEach
  void setUp() {
    testPitch = Pitch.builder().id(1L).title("Test Pitch").build();

    testMeeting =
        Meeting.builder()
            .id(1L)
            .pitch(testPitch)
            .type(MeetingType.KICKOFF)
            .dateHeld(LocalDate.now())
            .dorReady(true)
            .dodReady(false)
            .notes("Test meeting notes")
            .build();

    testRequest = new CreateMeetingRequest();
    testRequest.setPitchId(1L);
    testRequest.setType(MeetingType.KICKOFF);
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

    assertThatThrownBy(() -> meetingService.getMeetingById(999L))
        .isInstanceOf(RuntimeException.class)
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
    when(meetingRepository.findByType(MeetingType.KICKOFF)).thenReturn(Arrays.asList(testMeeting));

    List<MeetingDTO> result = meetingService.getMeetingsByType(MeetingType.KICKOFF);

    assertThat(result).hasSize(1);
    verify(meetingRepository).findByType(MeetingType.KICKOFF);
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
  void getMeetingsWithFilters_ShouldApplyFiltersAndReturnResults() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dateHeld"));
    Page<Meeting> page = new PageImpl<>(Arrays.asList(testMeeting), pageable, 1);

    when(meetingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<MeetingDTO> result =
        meetingService.getMeetingsWithFilters(
            null, null, 1L, Arrays.asList(MeetingType.KICKOFF), null, null, null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(meetingRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void createMeeting_WithActionItems_ShouldSaveMeetingWithActions() {
    Person testPerson = Person.builder().id(1L).name("Test Person").build();
    List<MeetingActionDTO> actions = new ArrayList<>();
    actions.add(
        MeetingActionDTO.builder()
            .description("Test action")
            .assignedToId(1L)
            .status(ActionStatus.OPEN)
            .dueDate(LocalDate.now().plusDays(7))
            .build());

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
    Meeting meetingWithActions =
        Meeting.builder()
            .id(1L)
            .pitch(testPitch)
            .type(MeetingType.KICKOFF)
            .dateHeld(LocalDate.now())
            .dorReady(true)
            .dodReady(false)
            .notes("Test notes")
            .actions(new ArrayList<>())
            .build();

    Person testPerson = Person.builder().id(1L).name("Test Person").build();
    List<MeetingActionDTO> newActions = new ArrayList<>();
    newActions.add(
        MeetingActionDTO.builder()
            .description("Updated action")
            .assignedToId(1L)
            .status(ActionStatus.IN_PROGRESS)
            .build());

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

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Pitch not found");
  }

  @Test
  void createMeeting_WithInvalidRetrospective_ShouldThrowException() {
    testRequest.setRetrospectiveId(999L);
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(retrospectiveRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Retrospective not found");
  }

  @Test
  void createMeeting_WithInvalidAssignee_ShouldThrowException() {
    List<MeetingActionDTO> actions = new ArrayList<>();
    actions.add(
        MeetingActionDTO.builder()
            .description("Test action")
            .assignedToId(999L)
            .status(ActionStatus.OPEN)
            .build());

    testRequest.setActions(actions);
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(personRepository.findById(999L)).thenReturn(Optional.empty());
    when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);

    assertThatThrownBy(() -> meetingService.createMeeting(testRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Person not found");
  }
}
