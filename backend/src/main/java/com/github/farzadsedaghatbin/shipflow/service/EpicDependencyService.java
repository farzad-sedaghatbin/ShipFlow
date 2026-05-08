package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.EpicDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing epic dependencies. Epic dependencies are informational only
 * (no hard reorder enforcement). Circular detection is still applied for data
 * integrity.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EpicDependencyService {

  private final EpicDependencyRepository epicDependencyRepository;
  private final EpicRepository epicRepository;
  private final LocalizationService localizationService;

  /**
   * Add a dependency between epics. Validates that the dependency doesn't create
   * a circular reference.
   */
  public EpicDependencyDTO addDependency(Long sourceEpicId, CreateEpicDependencyRequest request) {
    // Validate both epics exist
    Epic sourceEpic = epicRepository.findByIdNotDeleted(sourceEpicId)
        .orElseThrow(() -> new IllegalArgumentException("Source epic not found with id: " + sourceEpicId));

    Epic targetEpic = epicRepository.findByIdNotDeleted(request.getTargetEpicId()).orElseThrow(
        () -> new IllegalArgumentException("Target epic not found with id: " + request.getTargetEpicId()));

    // Prevent self-dependencies
    if (sourceEpicId.equals(request.getTargetEpicId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.self.reference"));
    }

    // Check if dependency already exists
    if (epicDependencyRepository.findBySourceEpicIdAndTargetEpicId(sourceEpicId, request.getTargetEpicId())
        .isPresent()) {
      throw new BadRequestException(localizationService.getMessage("dependency.already.exists"));
    }

    // Check for circular dependencies
    if (wouldCreateCircularDependency(sourceEpicId, request.getTargetEpicId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.circular.reference"));
    }

    // Create the dependency
    EpicDependency dependency = EpicDependency.builder()
        .sourceEpic(sourceEpic)
        .targetEpic(targetEpic)
        .dependencyType(
            request.getDependencyType() != null ? request.getDependencyType() : DependencyType.BLOCKS)
        .build();

    EpicDependency saved = epicDependencyRepository.save(dependency);
    return toDTO(saved);
  }

  /** Remove a dependency between epics. */
  public void removeDependency(Long dependencyId) {
    if (!epicDependencyRepository.existsById(dependencyId)) {
      throw new IllegalArgumentException(localizationService.getMessage("dependency.not.found", dependencyId));
    }
    epicDependencyRepository.deleteById(dependencyId);
  }

  /**
   * Get all dependencies for an epic (both outgoing/blocking and incoming/blockedBy).
   */
  public Map<String, List<EpicDependencyDTO>> getDependencies(Long epicId) {
    if (!epicRepository.existsById(epicId)) {
      throw new IllegalArgumentException("Epic not found with id: " + epicId);
    }

    List<EpicDependency> outgoing = epicDependencyRepository.findBySourceEpicId(epicId);
    List<EpicDependency> incoming = epicDependencyRepository.findByTargetEpicId(epicId);

    Map<String, List<EpicDependencyDTO>> result = new HashMap<>();
    result.put("blocking", outgoing.stream().map(this::toDTO).collect(Collectors.toList()));
    result.put("blockedBy", incoming.stream().map(this::toDTO).collect(Collectors.toList()));

    return result;
  }

  /**
   * Check if adding a dependency would create a circular reference. Uses
   * depth-first search to detect cycles.
   */
  private boolean wouldCreateCircularDependency(Long sourceEpicId, Long targetEpicId) {
    return hasPathBetweenEpics(targetEpicId, sourceEpicId, new HashSet<>());
  }

  /** Check if there's a dependency path from start to end epic using DFS. */
  private boolean hasPathBetweenEpics(Long startEpicId, Long endEpicId, Set<Long> visited) {
    if (startEpicId.equals(endEpicId)) {
      return true;
    }

    if (visited.contains(startEpicId)) {
      return false;
    }

    visited.add(startEpicId);

    List<EpicDependency> dependencies = epicDependencyRepository.findBySourceEpicId(startEpicId);
    for (EpicDependency dep : dependencies) {
      if (hasPathBetweenEpics(dep.getTargetEpic().getId(), endEpicId, visited)) {
        return true;
      }
    }

    return false;
  }

  /** Convert entity to DTO. */
  public EpicDependencyDTO toDTO(EpicDependency dependency) {
    return EpicDependencyDTO.builder()
        .id(dependency.getId())
        .sourceEpicId(dependency.getSourceEpic().getId())
        .sourceEpicName(dependency.getSourceEpic().getName())
        .targetEpicId(dependency.getTargetEpic().getId())
        .targetEpicName(dependency.getTargetEpic().getName())
        .dependencyType(dependency.getDependencyType())
        .createdAt(dependency.getCreatedAt())
        .build();
  }
}
