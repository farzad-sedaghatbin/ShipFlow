package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final PitchRepository pitchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<MeetingDTO> getAllMeetings() {
        return meetingRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<MeetingDTO> getMeetingsByPitchId(Long pitchId) {
        return meetingRepository.findByPitchId(pitchId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<MeetingDTO> getMeetingsByType(MeetingType type) {
        return meetingRepository.findByType(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public MeetingDTO getMeetingById(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found with id: " + id));
        return toDTO(meeting);
    }

    public MeetingDTO createMeeting(CreateMeetingRequest request) {
        Meeting meeting = Meeting.builder()
                .type(request.getType())
                .dateHeld(request.getDateHeld())
                .dorReady(request.getDorReady() != null ? request.getDorReady() : false)
                .dodReady(request.getDodReady() != null ? request.getDodReady() : false)
                .notes(request.getNotes())
                .build();
        
        if (request.getPitchId() != null) {
            Pitch pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
            meeting.setPitch(pitch);
        }
        
        Meeting saved = meetingRepository.save(meeting);
        
        // Publish event for knowledge ingestion
        eventPublisher.publishEvent(new KnowledgeEventListener.MeetingKnowledgeEvent(saved.getId()));
        
        return toDTO(saved);
    }

    public MeetingDTO updateMeeting(Long id, CreateMeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found with id: " + id));
        
        meeting.setType(request.getType());
        meeting.setDateHeld(request.getDateHeld());
        meeting.setDorReady(request.getDorReady());
        meeting.setDodReady(request.getDodReady());
        meeting.setNotes(request.getNotes());
        
        if (request.getPitchId() != null) {
            Pitch pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
            meeting.setPitch(pitch);
        }
        
        Meeting saved = meetingRepository.save(meeting);
        
        // Publish event for knowledge ingestion
        eventPublisher.publishEvent(new KnowledgeEventListener.MeetingKnowledgeEvent(saved.getId()));
        
        return toDTO(saved);
    }

    public void deleteMeeting(Long id) {
        meetingRepository.deleteById(id);
    }

    private MeetingDTO toDTO(Meeting meeting) {
        return MeetingDTO.builder()
                .id(meeting.getId())
                .pitchId(meeting.getPitch() != null ? meeting.getPitch().getId() : null)
                .pitchTitle(meeting.getPitch() != null ? meeting.getPitch().getTitle() : null)
                .cycleId(meeting.getPitch() != null && meeting.getPitch().getCycle() != null ? meeting.getPitch().getCycle().getId() : null)
                .cycleName(meeting.getPitch() != null && meeting.getPitch().getCycle() != null ? meeting.getPitch().getCycle().getName() : null)
                .projectId(meeting.getPitch() != null && meeting.getPitch().getCycle() != null && meeting.getPitch().getCycle().getProject() != null ? meeting.getPitch().getCycle().getProject().getId() : null)
                .projectName(meeting.getPitch() != null && meeting.getPitch().getCycle() != null && meeting.getPitch().getCycle().getProject() != null ? meeting.getPitch().getCycle().getProject().getName() : null)
                .projectKey(meeting.getPitch() != null && meeting.getPitch().getCycle() != null && meeting.getPitch().getCycle().getProject() != null ? meeting.getPitch().getCycle().getProject().getProjectKey() : null)
                .type(meeting.getType())
                .dateHeld(meeting.getDateHeld())
                .dorReady(meeting.getDorReady())
                .dodReady(meeting.getDodReady())
                .notes(meeting.getNotes())
                .build();
    }
}
