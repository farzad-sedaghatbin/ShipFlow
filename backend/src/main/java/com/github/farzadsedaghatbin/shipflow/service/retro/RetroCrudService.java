package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.dto.RetroDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Retrospective CRUD operations and lifecycle management.
 * Handles create, read, update, delete, open, and close operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RetroCrudService {

  private final RetroRepository retroRepository;
  private final RetroItemRepository retroItemRepository;
  private final CycleRepository cycleRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final LocalizationService localizationService;
  private final MessageService messageService;
  private final RetroMapper retroMapper;

  // ==================== READ OPERATIONS ====================

  public List<RetroDTO> getAllRetrosByProject(Long projectId) {
    validateRetrospectivesEnabled(projectId);
    List<Retrospective> retros = retroRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    return retroMapper.toDTOBatch(retros);
  }

  public List<RetroDTO> getRetrosByCycle(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    if (cycle.getProject() != null) {
      validateRetrospectivesEnabled(cycle.getProject().getId());
    }

    List<Retrospective> retros = retroRepository.findByCycleIdOrderByCreatedAtDesc(cycleId);
    return retroMapper.toDTOBatch(retros);
  }

  public RetroDTO getRetroById(Long id) {
    Retrospective retro = retroRepository.findByIdWithDetails(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));

    if (retro.getProject() != null) {
      validateRetrospectivesEnabled(retro.getProject().getId());
    }

    long itemCount = retroItemRepository.countByRetrospectiveId(id);
    return retroMapper.toDTO(retro, itemCount);
  }

  public RetroDTO getRetroWithItems(Long id) {
    Retrospective retro = retroRepository.findByIdWithItems(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));

    if (retro.getProject() != null) {
      validateRetrospectivesEnabled(retro.getProject().getId());
    }

    return retroMapper.toDTOWithItems(retro, getCurrentUser());
  }

  // ==================== CREATE/UPDATE/DELETE ====================

  public RetroDTO createRetro(CreateRetroRequest request) {
    validateRetrospectivesEnabled(request.getProjectId());

    Cycle cycle = cycleRepository.findById(request.getCycleId())
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + request.getCycleId()));

    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));

    User currentUser = getCurrentUser();

    Retrospective retro = Retrospective.builder()
        .title(request.getTitle())
        .notes(request.getNotes())
        .status(RetroStatus.DRAFT)
        .cycle(cycle)
        .project(project)
        .createdBy(currentUser)
        .build();

    Retrospective saved = retroRepository.save(retro);
    return retroMapper.toDTO(saved, 0L);
  }

  public RetroDTO updateRetro(Long id, UpdateRetroRequest request) {
    Retrospective retro = retroRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));

    if (retro.getProject() != null) {
      validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.update.closed"));
    }

    if (request.getTitle() != null) {
      retro.setTitle(request.getTitle());
    }
    if (request.getNotes() != null) {
      retro.setNotes(request.getNotes());
    }

    Retrospective saved = retroRepository.save(retro);
    long itemCount = retroItemRepository.countByRetrospectiveId(id);
    return retroMapper.toDTO(saved, itemCount);
  }

  public void deleteRetro(Long id) {
    if (!retroRepository.existsById(id)) {
      throw new ResourceNotFoundException("Retrospective not found with id: " + id);
    }
    retroRepository.deleteById(id);
  }

  // ==================== LIFECYCLE OPERATIONS ====================

  public RetroDTO openRetro(Long id) {
    Retrospective retro = retroRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));

    if (retro.getProject() != null) {
      validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.open.closed"));
    }

    retro.setStatus(RetroStatus.OPEN);
    Retrospective saved = retroRepository.save(retro);
    long itemCount = retroItemRepository.countByRetrospectiveId(id);
    return retroMapper.toDTO(saved, itemCount);
  }

  public RetroDTO closeRetro(Long id) {
    Retrospective retro = retroRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));

    if (retro.getProject() != null) {
      validateRetrospectivesEnabled(retro.getProject().getId());
    }

    retro.setStatus(RetroStatus.CLOSED);
    retro.setClosedAt(LocalDateTime.now());
    Retrospective saved = retroRepository.save(retro);
    long itemCount = retroItemRepository.countByRetrospectiveId(id);
    return retroMapper.toDTO(saved, itemCount);
  }

  // ==================== PROJECT SETTINGS ====================

  public boolean isRetrospectivesEnabled(Long projectId) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    return Boolean.TRUE.equals(project.getEnableRetrospectives());
  }

  public void setRetrospectivesEnabled(Long projectId, boolean enabled) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    project.setEnableRetrospectives(enabled);
    projectRepository.save(project);
  }

  // ==================== HELPERS ====================

  public void validateRetrospectivesEnabled(Long projectId) {
    if (!isRetrospectivesEnabled(projectId)) {
      throw new IllegalStateException(messageService.getMessage("error.retro.feature.disabled"));
    }
  }

  public User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }

  /**
   * Get retrospective entity by ID (for internal use by other retro services).
   */
  public Retrospective getRetroEntity(Long id) {
    return retroRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
  }

  /**
   * Get retrospective entity with items (for internal use by other retro services).
   */
  public Retrospective getRetroEntityWithItems(Long id) {
    return retroRepository.findByIdWithItems(id)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
  }
}
