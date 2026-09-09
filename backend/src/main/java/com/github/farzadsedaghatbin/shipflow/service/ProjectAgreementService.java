package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementDTO;
import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.ProjectAgreement;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectAgreementRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing per-project agreement/contract-term entries — discrete, dated
 * items logged over time, optionally linked to the Meeting they originated from.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAgreementService {

  private final ProjectAgreementRepository projectAgreementRepository;
  private final ProjectRepository projectRepository;
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<ProjectAgreementDTO> listByProject(Long projectId) {
    return projectAgreementRepository.findByProjectIdNotDeleted(projectId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public ProjectAgreementDTO create(Long projectId, ProjectAgreementRequest request) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

    Meeting meeting = resolveMeeting(projectId, request.getMeetingId());

    ProjectAgreement agreement = ProjectAgreement.builder()
        .project(project)
        .title(request.getTitle())
        .content(request.getContent())
        .meeting(meeting)
        .agreedDate(request.getAgreedDate() != null ? request.getAgreedDate() : LocalDate.now())
        .createdBy(getCurrentUser())
        .build();

    return toDTO(projectAgreementRepository.save(agreement));
  }

  @Transactional
  public ProjectAgreementDTO update(Long projectId, Long agreementId, ProjectAgreementRequest request) {
    ProjectAgreement agreement = findByIdAndProjectOrThrow(projectId, agreementId);

    Meeting meeting = resolveMeeting(agreement.getProject().getId(), request.getMeetingId());

    agreement.setTitle(request.getTitle());
    agreement.setContent(request.getContent());
    agreement.setMeeting(meeting);
    agreement.setAgreedDate(request.getAgreedDate() != null ? request.getAgreedDate() : LocalDate.now());

    return toDTO(projectAgreementRepository.save(agreement));
  }

  @Transactional
  public void delete(Long projectId, Long agreementId) {
    ProjectAgreement agreement = findByIdAndProjectOrThrow(projectId, agreementId);

    agreement.setDeletedAt(LocalDateTime.now());
    agreement.setDeletedBy(getCurrentUser());
    projectAgreementRepository.save(agreement);
  }

  /**
   * Loads an agreement by id and verifies it actually belongs to {@code projectId} — the
   * controller's {@code @PreAuthorize}/{@code requireProjectAccess} only confirm the caller may
   * act within that project, not that this specific {@code agreementId} is one of its rows, so
   * without this check a caller with access to any project could mutate another project's
   * agreement by guessing/incrementing the id.
   */
  private ProjectAgreement findByIdAndProjectOrThrow(Long projectId, Long agreementId) {
    ProjectAgreement agreement = projectAgreementRepository.findByIdNotDeleted(agreementId)
        .orElseThrow(() -> new ResourceNotFoundException("Agreement not found: " + agreementId));
    if (!agreement.getProject().getId().equals(projectId)) {
      throw new ResourceNotFoundException("Agreement not found: " + agreementId);
    }
    return agreement;
  }

  /**
   * Resolve and validate an optional meeting link — a meeting linked to an agreement must
   * belong to the same project the agreement is being created/updated on, whether the meeting's
   * project comes from its own direct reference or (when it has none) from its pitch's cycle.
   */
  private Meeting resolveMeeting(Long projectId, Long meetingId) {
    if (meetingId == null) {
      return null;
    }

    Meeting meeting = meetingRepository.findById(meetingId)
        .orElseThrow(() -> new ResourceNotFoundException("Meeting not found: " + meetingId));

    Long meetingProjectId = resolveMeetingProjectId(meeting);
    if (meetingProjectId == null || !meetingProjectId.equals(projectId)) {
      throw new BadRequestException(
          "Meeting " + meetingId + " does not belong to project " + projectId);
    }

    return meeting;
  }

  private Long resolveMeetingProjectId(Meeting meeting) {
    if (meeting.getPitch() != null
        && meeting.getPitch().getCycle() != null
        && meeting.getPitch().getCycle().getProject() != null) {
      return meeting.getPitch().getCycle().getProject().getId();
    }
    if (meeting.getProject() != null) {
      return meeting.getProject().getId();
    }
    return null;
  }

  private ProjectAgreementDTO toDTO(ProjectAgreement agreement) {
    return ProjectAgreementDTO.builder()
        .id(agreement.getId())
        .projectId(agreement.getProject().getId())
        .title(agreement.getTitle())
        .content(agreement.getContent())
        .meetingId(agreement.getMeeting() != null ? agreement.getMeeting().getId() : null)
        .agreedDate(agreement.getAgreedDate())
        .createdByName(agreement.getCreatedBy() != null ? agreement.getCreatedBy().getUsername() : null)
        .createdAt(agreement.getCreatedAt())
        .build();
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }
}
