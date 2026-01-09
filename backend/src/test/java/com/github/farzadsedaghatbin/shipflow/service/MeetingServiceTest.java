package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingService meetingService;

    private Meeting testMeeting;
    private Pitch testPitch;
    private CreateMeetingRequest testRequest;

    @BeforeEach
    void setUp() {
        testPitch = Pitch.builder()
                .id(1L)
                .title("Test Pitch")
                .build();

        testMeeting = Meeting.builder()
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
}
