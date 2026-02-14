package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EpicService {

  private final EpicRepository epicRepository;
  private final InitiativeRepository initiativeRepository;
  private final PitchRepository pitchRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public List<EpicDTO> getAllEpics() {
    return epicRepository.findAllNotDeleted().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<EpicDTO> getEpicsByProjectId(Long projectId) {
    return epicRepository.findByProjectIdNotDeleted(projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public Page<EpicDTO> getEpicsByProjectId(Long projectId, Pageable pageable) {
    return epicRepository.findByProjectIdNotDeleted(projectId, pageable)
        .map(this::toDTO);
  }

  public List<EpicDTO> getEpicsByInitiativeId(Long initiativeId) {
    return epicRepository.findByInitiativeIdNotDeleted(initiativeId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<EpicDTO> getOrphanEpics(Long projectId) {
    return epicRepository.findOrphanEpicsByProjectIdNotDeleted(projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<EpicDTO> getEpicsByProjectIdAndStatus(Long projectId, EpicStatus status) {
    return epicRepository.findByProjectIdAndStatusNotDeleted(projectId, status).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<EpicDTO> getActiveEpics(Long projectId) {
    return epicRepository.findByProjectIdAndStatusInNotDeleted(projectId, 
          java.util.Arrays.asList(EpicStatus.PLANNED, EpicStatus.IN_PROGRESS)).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<EpicDTO> getEpicsByDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
    return epicRepository.findByProjectIdAndDateRangeNotDeleted(projectId, startDate, endDate).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public EpicDTO getEpicById(Long id) {
    Epic epic = epicRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + id));
    return toDTO(epic);
  }

  public EpicDTO getEpicWithPitches(Long id) {
    Epic epic = epicRepository.findByIdWithPitchesNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + id));
    return toDTOWithPitches(epic);
  }

  public EpicDTO createEpic(CreateEpicRequest request) {
    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    Epic epic = Epic.builder()
        .name(request.getName())
        .description(request.getDescription())
        .status(request.getStatus() != null ? request.getStatus() : EpicStatus.DRAFT)
        .color(request.getColor())
        .targetStartDate(request.getTargetStartDate())
        .targetEndDate(request.getTargetEndDate())
        .project(project)
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .build();

    if (request.getInitiativeId() != null) {
      Initiative initiative = initiativeRepository.findByIdNotDeleted(request.getInitiativeId())
          .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + request.getInitiativeId()));
      epic.setInitiative(initiative);
    }

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      epic.setOwner(owner);
    }

    Epic saved = epicRepository.save(epic);
    log.info("Created epic: {} for project: {}", saved.getName(), project.getProjectKey());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.EpicKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public EpicDTO updateEpic(Long id, CreateEpicRequest request) {
    Epic epic = epicRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + id));

    epic.setName(request.getName());
    epic.setDescription(request.getDescription());
    if (request.getStatus() != null) {
      epic.setStatus(request.getStatus());
    }
    epic.setColor(request.getColor());
    epic.setTargetStartDate(request.getTargetStartDate());
    epic.setTargetEndDate(request.getTargetEndDate());
    if (request.getSortOrder() != null) {
      epic.setSortOrder(request.getSortOrder());
    }

    if (request.getInitiativeId() != null) {
      Initiative initiative = initiativeRepository.findByIdNotDeleted(request.getInitiativeId())
          .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + request.getInitiativeId()));
      epic.setInitiative(initiative);
    } else {
      epic.setInitiative(null);
    }

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      epic.setOwner(owner);
    } else {
      epic.setOwner(null);
    }

    Epic saved = epicRepository.save(epic);
    log.info("Updated epic: {}", saved.getName());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.EpicKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public void deleteEpic(Long id) {
    Epic epic = epicRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + id));

    User currentUser = getCurrentUser();
    epic.setDeletedAt(LocalDateTime.now());
    epic.setDeletedBy(currentUser);
    epicRepository.save(epic);
    log.info("Soft deleted epic: {}", epic.getName());
  }

  public EpicDTO updateStatus(Long id, EpicStatus status) {
    Epic epic = epicRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + id));

    epic.setStatus(status);
    Epic saved = epicRepository.save(epic);
    log.info("Updated epic status: {} to {}", saved.getName(), status);
    return toDTO(saved);
  }

  public EpicDTO linkToInitiative(Long epicId, Long initiativeId) {
    Epic epic = epicRepository.findByIdNotDeleted(epicId)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + epicId));
    Initiative initiative = initiativeRepository.findByIdNotDeleted(initiativeId)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + initiativeId));

    epic.setInitiative(initiative);
    Epic saved = epicRepository.save(epic);
    log.info("Linked epic {} to initiative {}", epic.getName(), initiative.getName());
    return toDTO(saved);
  }

  public EpicDTO unlinkFromInitiative(Long epicId) {
    Epic epic = epicRepository.findByIdNotDeleted(epicId)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + epicId));

    epic.setInitiative(null);
    Epic saved = epicRepository.save(epic);
    log.info("Unlinked epic {} from initiative", epic.getName());
    return toDTO(saved);
  }

  /**
   * Calculate progress based on child pitches.
   */
  public double calculateProgress(Long epicId) {
    Epic epic = epicRepository.findByIdWithPitchesNotDeleted(epicId)
        .orElseThrow(() -> new ResourceNotFoundException("Epic not found with id: " + epicId));

    if (epic.getPitches() == null || epic.getPitches().isEmpty()) {
      return 0.0;
    }

    long totalPitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .count();
    if (totalPitches == 0) {
      return 0.0;
    }

    long completedPitches = epic.getPitches().stream()
        .filter(p -> p.getDeletedAt() == null)
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .count();

    return (completedPitches * 100.0) / totalPitches;
  }

  private User getCurrentUser() {
    try {
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      return userRepository.findByUsername(username).orElse(null);
    } catch (Exception e) {
      log.warn("Failed to get current user: {}", e.getMessage());
      return null;
    }
  }

  private EpicDTO toDTO(Epic epic) {
    int pitchCount = 0;
    int completedPitchCount = 0;

    if (epic.getPitches() != null) {
      pitchCount = (int) epic.getPitches().stream()
          .filter(p -> p.getDeletedAt() == null)
          .count();
      completedPitchCount = (int) epic.getPitches().stream()
          .filter(p -> p.getDeletedAt() == null)
          .filter(p -> p.getStatus() == PitchStatus.DONE)
          .count();
    }

    return EpicDTO.builder()
        .id(epic.getId())
        .name(epic.getName())
        .description(epic.getDescription())
        .status(epic.getStatus())
        .color(epic.getColor())
        .targetStartDate(epic.getTargetStartDate())
        .targetEndDate(epic.getTargetEndDate())
        .sortOrder(epic.getSortOrder())
        .projectId(epic.getProject().getId())
        .projectName(epic.getProject().getName())
        .projectKey(epic.getProject().getProjectKey())
        .initiativeId(epic.getInitiative() != null ? epic.getInitiative().getId() : null)
        .initiativeName(epic.getInitiative() != null ? epic.getInitiative().getName() : null)
        .ownerId(epic.getOwner() != null ? epic.getOwner().getId() : null)
        .ownerName(epic.getOwner() != null ? epic.getOwner().getUsername() : null)
        .createdAt(epic.getCreatedAt())
        .updatedAt(epic.getUpdatedAt())
        .pitchCount(pitchCount)
        .completedPitchCount(completedPitchCount)
        .progressPercentage(pitchCount > 0 ? (completedPitchCount * 100.0) / pitchCount : 0.0)
        .build();
  }

  private EpicDTO toDTOWithPitches(Epic epic) {
    EpicDTO dto = toDTO(epic);

    List<EpicDTO.PitchSummaryDTO> pitchSummaries = epic.getPitches().stream()
        .filter(pitch -> pitch.getDeletedAt() == null)
        .map(pitch -> EpicDTO.PitchSummaryDTO.builder()
            .id(pitch.getId())
            .title(pitch.getTitle())
            .status(pitch.getStatus().name())
            .cycleName(pitch.getCycle() != null ? pitch.getCycle().getName() : null)
            .appetiteDays(pitch.getAppetiteDays())
            .progressPercentage(0.0) // Will be calculated elsewhere
            .build())
        .collect(Collectors.toList());

    dto.setPitches(pitchSummaries);
    return dto;
  }
}
