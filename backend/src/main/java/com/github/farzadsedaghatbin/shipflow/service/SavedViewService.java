package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.savedview.CreateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.SavedViewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.UpdateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.entity.SavedView;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.SavedViewRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing per-user, per-project saved filter views (backlog presets). */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SavedViewService {

  private final SavedViewRepository savedViewRepository;

  // ── Read ────────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<SavedViewDTO> getSavedViews(Long userId, Long projectId) {
    return savedViewRepository.findByUserIdAndProjectId(userId, projectId).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // ── Create ──────────────────────────────────────────────────────────────────

  public SavedViewDTO createSavedView(
      Long userId, Long projectId, CreateSavedViewRequest request) {
    if (savedViewRepository.existsByUserIdAndProjectIdAndName(userId, projectId, request.getName())) {
      throw new BadRequestException(
          "A saved view named '" + request.getName() + "' already exists for this project");
    }

    SavedView view =
        SavedView.builder()
            .userId(userId)
            .projectId(projectId)
            .name(request.getName())
            .filters(request.getFilters())
            .isDefault(false)
            .build();

    SavedView saved = savedViewRepository.save(view);
    log.info("Created saved view '{}' (id={}) for user {} in project {}", saved.getName(),
        saved.getId(), userId, projectId);
    return toDTO(saved);
  }

  // ── Update ──────────────────────────────────────────────────────────────────

  public SavedViewDTO updateSavedView(Long id, Long userId, UpdateSavedViewRequest request) {
    SavedView view = savedViewRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Saved view not found with id: " + id + " for current user"));

    if (request.getName() != null && !request.getName().isBlank()) {
      // Only check for duplicates when the name actually changes
      if (!request.getName().equals(view.getName())
          && savedViewRepository.existsByUserIdAndProjectIdAndName(
              userId, view.getProjectId(), request.getName())) {
        throw new BadRequestException(
            "A saved view named '" + request.getName() + "' already exists for this project");
      }
      view.setName(request.getName());
    }

    if (request.getFilters() != null) {
      view.setFilters(request.getFilters());
    }

    SavedView updated = savedViewRepository.save(view);
    log.info("Updated saved view id={} for user {}", id, userId);
    return toDTO(updated);
  }

  // ── Delete ──────────────────────────────────────────────────────────────────

  public void deleteSavedView(Long id, Long userId) {
    SavedView view = savedViewRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Saved view not found with id: " + id + " for current user"));
    savedViewRepository.delete(view);
    log.info("Deleted saved view id={} for user {}", id, userId);
  }

  // ── Set Default ─────────────────────────────────────────────────────────────

  public SavedViewDTO setDefault(Long id, Long userId, Long projectId) {
    SavedView view = savedViewRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Saved view not found with id: " + id + " for current user"));

    // Unset previous default for this user+project (if any)
    savedViewRepository.findByUserIdAndProjectIdAndIsDefaultTrue(userId, projectId)
        .ifPresent(prev -> {
          if (!prev.getId().equals(id)) {
            prev.setDefault(false);
            savedViewRepository.save(prev);
          }
        });

    view.setDefault(true);
    SavedView updated = savedViewRepository.save(view);
    log.info("Set saved view id={} as default for user {} in project {}", id, userId, projectId);
    return toDTO(updated);
  }

  // ── Mapping ─────────────────────────────────────────────────────────────────

  private SavedViewDTO toDTO(SavedView view) {
    return SavedViewDTO.builder()
        .id(view.getId())
        .userId(view.getUserId())
        .projectId(view.getProjectId())
        .name(view.getName())
        .filters(view.getFilters())
        .isDefault(view.isDefault())
        .createdAt(view.getCreatedAt())
        .updatedAt(view.getUpdatedAt())
        .build();
  }
}
