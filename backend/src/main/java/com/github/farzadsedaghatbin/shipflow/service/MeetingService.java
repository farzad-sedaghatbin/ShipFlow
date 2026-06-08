package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingActionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.MeetingDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionStatus;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetrospectiveRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MeetingService {

  private final MeetingRepository meetingRepository;
  private final PitchRepository pitchRepository;
  private final ProjectRepository projectRepository;
  private final RetrospectiveRepository retrospectiveRepository;
  private final PersonRepository personRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  public Page<MeetingDTO> getAllMeetingsPaginated(Pageable pageable) {
    return meetingRepository.findAll(pageable).map(this::toDTO);
  }

  public Page<MeetingDTO> getMeetingsWithFilters(Long cycleId, Long projectId, Long pitchId, List<String> types,
      LocalDate startDate, LocalDate endDate, Boolean dorReady, Boolean dodReady, Pageable pageable) {

    Specification<Meeting> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (cycleId != null) {
        var pitchJoin = root.join("pitch", JoinType.LEFT);
        var cycleJoin = pitchJoin.join("cycle", JoinType.LEFT);
        predicates.add(cb.equal(cycleJoin.get("id"), cycleId));
      }

      if (projectId != null) {
        var directProjectJoin = root.join("project", JoinType.LEFT);
        var pitchJoin = root.join("pitch", JoinType.LEFT);
        var cycleJoin = pitchJoin.join("cycle", JoinType.LEFT);
        var cycleProjectJoin = cycleJoin.join("project", JoinType.LEFT);
        predicates.add(cb.or(
            cb.equal(directProjectJoin.get("id"), projectId),
            cb.equal(cycleProjectJoin.get("id"), projectId)
        ));
      }

      if (pitchId != null) {
        predicates.add(cb.equal(root.get("pitch").get("id"), pitchId));
      }

      if (types != null && !types.isEmpty()) {
        predicates.add(root.get("type").in(types));
      }

      if (startDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("dateHeld"), startDate));
      }

      if (endDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("dateHeld"), endDate));
      }

      if (dorReady != null) {
        predicates.add(cb.equal(root.get("dorReady"), dorReady));
      }

      if (dodReady != null) {
        predicates.add(cb.equal(root.get("dodReady"), dodReady));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };

    return meetingRepository.findAll(spec, pageable).map(this::toDTO);
  }

  public List<MeetingDTO> getMeetingsByPitchId(Long pitchId) {
    return meetingRepository.findByPitchId(pitchId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<MeetingDTO> getMeetingsByType(String type) {
    return meetingRepository.findByType(type).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public MeetingDTO getMeetingById(Long id) {
    Meeting meeting = meetingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Meeting not found with id: " + id));
    return toDTO(meeting);
  }

  public MeetingDTO getMeetingForView(Long id) {
    Meeting meeting = meetingRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Meeting not found with id: " + id));
    return toDTO(meeting);
  }

  public MeetingDTO createMeeting(CreateMeetingRequest request) {
    Meeting meeting = Meeting.builder().type(request.getType()).dateHeld(request.getDateHeld())
        .dorReady(request.getDorReady() != null ? request.getDorReady() : false)
        .dodReady(request.getDodReady() != null ? request.getDodReady() : false)
        .dorItemsJson(toJson(request.getDorItems()))
        .dodItemsJson(toJson(request.getDodItems()))
        .notes(request.getNotes())
        .decisions(request.getDecisions()).attendees(request.getAttendees()).build();

    if (request.getPitchId() != null) {
      Pitch pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
      meeting.setPitch(pitch);
      // Also set project from pitch's cycle
      if (pitch.getCycle() != null && pitch.getCycle().getProject() != null) {
        meeting.setProject(pitch.getCycle().getProject());
      }
    } else if (request.getProjectId() != null) {
      // For meetings without a pitch, use direct project association
      Project project = projectRepository.findById(request.getProjectId())
          .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + request.getProjectId()));
      meeting.setProject(project);
    }

    if (request.getRetrospectiveId() != null) {
      Retrospective retrospective = retrospectiveRepository.findById(request.getRetrospectiveId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Retrospective not found with id: " + request.getRetrospectiveId()));
      meeting.setRetrospective(retrospective);
    }

    Meeting saved = meetingRepository.save(meeting);

    // Handle action items
    if (request.getActions() != null && !request.getActions().isEmpty()) {
      for (MeetingActionDTO actionDTO : request.getActions()) {
        MeetingAction action = MeetingAction.builder().meeting(saved).description(actionDTO.getDescription())
            .status(actionDTO.getStatus() != null ? actionDTO.getStatus() : ActionStatus.OPEN)
            .dueDate(actionDTO.getDueDate()).notes(actionDTO.getNotes()).build();

        if (actionDTO.getAssignedToId() != null) {
          Person assignee = personRepository.findById(actionDTO.getAssignedToId())
              .orElseThrow(() -> new IllegalArgumentException(
                  "Person not found with id: " + actionDTO.getAssignedToId()));
          action.setAssignedTo(assignee);
        }

        saved.getActions().add(action);
      }
      saved = meetingRepository.save(saved);
    }

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
    meeting.setDorItemsJson(toJson(request.getDorItems()));
    meeting.setDodItemsJson(toJson(request.getDodItems()));
    meeting.setNotes(request.getNotes());
    meeting.setDecisions(request.getDecisions());
    meeting.setAttendees(request.getAttendees());

    if (request.getPitchId() != null) {
      Pitch pitch = pitchRepository.findById(request.getPitchId()).orElseThrow(
          () -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
      meeting.setPitch(pitch);
      // Also update project from pitch's cycle
      if (pitch.getCycle() != null && pitch.getCycle().getProject() != null) {
        meeting.setProject(pitch.getCycle().getProject());
      }
    } else {
      meeting.setPitch(null);
      // For meetings without a pitch, use direct project association
      if (request.getProjectId() != null) {
        Project project = projectRepository.findById(request.getProjectId())
            .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + request.getProjectId()));
        meeting.setProject(project);
      } else {
        meeting.setProject(null);
      }
    }

    if (request.getRetrospectiveId() != null) {
      Retrospective retrospective = retrospectiveRepository.findById(request.getRetrospectiveId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Retrospective not found with id: " + request.getRetrospectiveId()));
      meeting.setRetrospective(retrospective);
    } else {
      meeting.setRetrospective(null);
    }

    // Clear existing actions and add new ones
    meeting.getActions().clear();
    if (request.getActions() != null && !request.getActions().isEmpty()) {
      for (MeetingActionDTO actionDTO : request.getActions()) {
        MeetingAction action = MeetingAction.builder().meeting(meeting).description(actionDTO.getDescription())
            .status(actionDTO.getStatus() != null ? actionDTO.getStatus() : ActionStatus.OPEN)
            .dueDate(actionDTO.getDueDate()).notes(actionDTO.getNotes()).build();

        if (actionDTO.getAssignedToId() != null) {
          Person assignee = personRepository.findById(actionDTO.getAssignedToId())
              .orElseThrow(() -> new IllegalArgumentException(
                  "Person not found with id: " + actionDTO.getAssignedToId()));
          action.setAssignedTo(assignee);
        }

        meeting.getActions().add(action);
      }
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
    List<MeetingActionDTO> actionDTOs = meeting.getActions() != null
        ? meeting.getActions().stream().map(action -> MeetingActionDTO.builder().id(action.getId())
            .description(action.getDescription())
            .assignedToId(action.getAssignedTo() != null ? action.getAssignedTo().getId() : null)
            .assignedToName(action.getAssignedTo() != null ? action.getAssignedTo().getName() : null)
            .status(action.getStatus()).dueDate(action.getDueDate()).notes(action.getNotes()).build())
            .collect(Collectors.toList())
        : new ArrayList<>();

    // Derive project/cycle from pitch if available, otherwise use direct project reference
    Long projectId = null;
    String projectName = null;
    String projectKey = null;
    Long cycleId = null;
    String cycleName = null;

    if (meeting.getPitch() != null) {
      if (meeting.getPitch().getCycle() != null) {
        cycleId = meeting.getPitch().getCycle().getId();
        cycleName = meeting.getPitch().getCycle().getName();
        if (meeting.getPitch().getCycle().getProject() != null) {
          projectId = meeting.getPitch().getCycle().getProject().getId();
          projectName = meeting.getPitch().getCycle().getProject().getName();
          projectKey = meeting.getPitch().getCycle().getProject().getProjectKey();
        }
      }
    } else if (meeting.getProject() != null) {
      // Use direct project reference when no pitch
      projectId = meeting.getProject().getId();
      projectName = meeting.getProject().getName();
      projectKey = meeting.getProject().getProjectKey();
    }

    return MeetingDTO.builder().id(meeting.getId())
        .pitchId(meeting.getPitch() != null ? meeting.getPitch().getId() : null)
        .pitchTitle(meeting.getPitch() != null ? meeting.getPitch().getTitle() : null)
        .cycleId(cycleId)
        .cycleName(cycleName)
        .projectId(projectId)
        .projectName(projectName)
        .projectKey(projectKey)
        .type(meeting.getType()).dateHeld(meeting.getDateHeld()).dorReady(meeting.getDorReady())
        .dodReady(meeting.getDodReady())
        .dorItems(fromJson(meeting.getDorItemsJson(), new TypeReference<List<MeetingDTO.MeetingChecklistItem>>() {}))
        .dodItems(fromJson(meeting.getDodItemsJson(), new TypeReference<List<MeetingDTO.MeetingChecklistItem>>() {}))
        .notes(meeting.getNotes())
        .retrospectiveId(meeting.getRetrospective() != null ? meeting.getRetrospective().getId() : null)
        .retrospectiveTitle(meeting.getRetrospective() != null ? meeting.getRetrospective().getTitle() : null)
        .decisions(meeting.getDecisions()).attendees(meeting.getAttendees()).actions(actionDTOs).build();
  }

  /** Convert object to JSON string. */
  private String toJson(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize object to JSON", e);
      return null;
    }
  }

  /** Convert JSON string to object. */
  private <T> T fromJson(String json, TypeReference<T> typeRef) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize JSON", e);
      return null;
    }
  }
}
