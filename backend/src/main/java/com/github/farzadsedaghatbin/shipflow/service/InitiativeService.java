package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateInitiativeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.InitiativeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InitiativeService {

  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public List<InitiativeDTO> getAllInitiatives() {
    return initiativeRepository.findAllNotDeleted().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<InitiativeDTO> getInitiativesByProjectId(Long projectId) {
    return initiativeRepository.findByProjectIdNotDeleted(projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public Page<InitiativeDTO> getInitiativesByProjectId(Long projectId, Pageable pageable) {
    return initiativeRepository.findByProjectIdNotDeleted(projectId, pageable)
        .map(this::toDTO);
  }

  public List<InitiativeDTO> getInitiativesByProjectIdAndStatus(Long projectId, InitiativeStatus status) {
    return initiativeRepository.findByProjectIdAndStatusNotDeleted(projectId, status).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<InitiativeDTO> getActiveInitiatives(Long projectId) {
    return initiativeRepository.findByProjectIdAndStatusInNotDeleted(projectId, 
          java.util.Arrays.asList(InitiativeStatus.PLANNED, InitiativeStatus.IN_PROGRESS)).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public List<InitiativeDTO> getInitiativesByDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
    return initiativeRepository.findByProjectIdAndDateRangeNotDeleted(projectId, startDate, endDate).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  public InitiativeDTO getInitiativeById(Long id) {
    Initiative initiative = initiativeRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + id));
    return toDTO(initiative);
  }

  public InitiativeDTO getInitiativeWithEpics(Long id) {
    Initiative initiative = initiativeRepository.findByIdWithEpicsNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + id));
    return toDTOWithEpics(initiative);
  }

  public InitiativeDTO createInitiative(CreateInitiativeRequest request) {
    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    Initiative initiative = Initiative.builder()
        .name(request.getName())
        .description(request.getDescription())
        .status(request.getStatus() != null ? request.getStatus() : InitiativeStatus.DRAFT)
        .color(request.getColor())
        .targetStartDate(request.getTargetStartDate())
        .targetEndDate(request.getTargetEndDate())
        .project(project)
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .build();

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      initiative.setOwner(owner);
    }

    Initiative saved = initiativeRepository.save(initiative);
    log.info("Created initiative: {} for project: {}", saved.getName(), project.getProjectKey());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.InitiativeKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public InitiativeDTO updateInitiative(Long id, CreateInitiativeRequest request) {
    Initiative initiative = initiativeRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + id));

    initiative.setName(request.getName());
    initiative.setDescription(request.getDescription());
    if (request.getStatus() != null) {
      initiative.setStatus(request.getStatus());
    }
    initiative.setColor(request.getColor());
    initiative.setTargetStartDate(request.getTargetStartDate());
    initiative.setTargetEndDate(request.getTargetEndDate());
    if (request.getSortOrder() != null) {
      initiative.setSortOrder(request.getSortOrder());
    }

    if (request.getOwnerId() != null) {
      User owner = userRepository.findById(request.getOwnerId())
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
      initiative.setOwner(owner);
    } else {
      initiative.setOwner(null);
    }

    Initiative saved = initiativeRepository.save(initiative);
    log.info("Updated initiative: {}", saved.getName());
    
    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.InitiativeKnowledgeEvent(saved.getId()));
    
    return toDTO(saved);
  }

  public void deleteInitiative(Long id) {
    Initiative initiative = initiativeRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + id));

    User currentUser = getCurrentUser();
    initiative.setDeletedAt(LocalDateTime.now());
    initiative.setDeletedBy(currentUser);
    initiativeRepository.save(initiative);
    log.info("Soft deleted initiative: {}", initiative.getName());
  }

  public InitiativeDTO updateStatus(Long id, InitiativeStatus status) {
    Initiative initiative = initiativeRepository.findByIdNotDeleted(id)
        .orElseThrow(() -> new ResourceNotFoundException("Initiative not found with id: " + id));

    initiative.setStatus(status);
    Initiative saved = initiativeRepository.save(initiative);
    log.info("Updated initiative status: {} to {}", saved.getName(), status);
    return toDTO(saved);
  }

  /**
   * Calculate progress based on child epics.
   */
  public double calculateProgress(Long initiativeId) {
    long totalEpics = epicRepository.countByInitiativeIdNotDeleted(initiativeId);
    if (totalEpics == 0) {
      return 0.0;
    }
    long completedEpics = epicRepository.countByInitiativeIdAndStatusNotDeleted(initiativeId, EpicStatus.COMPLETED);
    return (completedEpics * 100.0) / totalEpics;
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

  private InitiativeDTO toDTO(Initiative initiative) {
    long epicCount = epicRepository.countByInitiativeIdNotDeleted(initiative.getId());
    long completedEpicCount = epicRepository.countByInitiativeIdAndStatusNotDeleted(initiative.getId(), EpicStatus.COMPLETED);

    return InitiativeDTO.builder()
        .id(initiative.getId())
        .name(initiative.getName())
        .description(initiative.getDescription())
        .status(initiative.getStatus())
        .color(initiative.getColor())
        .targetStartDate(initiative.getTargetStartDate())
        .targetEndDate(initiative.getTargetEndDate())
        .sortOrder(initiative.getSortOrder())
        .projectId(initiative.getProject().getId())
        .projectName(initiative.getProject().getName())
        .projectKey(initiative.getProject().getProjectKey())
        .ownerId(initiative.getOwner() != null ? initiative.getOwner().getId() : null)
        .ownerName(initiative.getOwner() != null ? initiative.getOwner().getUsername() : null)
        .createdAt(initiative.getCreatedAt())
        .updatedAt(initiative.getUpdatedAt())
        .epicCount((int) epicCount)
        .completedEpicCount((int) completedEpicCount)
        .progressPercentage(epicCount > 0 ? (completedEpicCount * 100.0) / epicCount : 0.0)
        .build();
  }

  private InitiativeDTO toDTOWithEpics(Initiative initiative) {
    InitiativeDTO dto = toDTO(initiative);

    List<InitiativeDTO.EpicSummaryDTO> epicSummaries = initiative.getEpics().stream()
        .filter(epic -> epic.getDeletedAt() == null)
        .map(epic -> InitiativeDTO.EpicSummaryDTO.builder()
            .id(epic.getId())
            .name(epic.getName())
            .status(epic.getStatus().name())
            .targetStartDate(epic.getTargetStartDate())
            .targetEndDate(epic.getTargetEndDate())
            .pitchCount(epic.getPitches() != null ? (int) epic.getPitches().stream()
                .filter(p -> p.getDeletedAt() == null).count() : 0)
            .progressPercentage(0.0) // Will be calculated in EpicService
            .build())
        .collect(Collectors.toList());

    dto.setEpics(epicSummaries);
    return dto;
  }
}
