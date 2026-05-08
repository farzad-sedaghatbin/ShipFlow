package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreatePitchDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.PitchDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing pitch dependencies. Handles creating, deleting, and
 * querying pitch dependencies with circular dependency detection.
 * Pitch dependencies are enforced during reorder operations (unlike epic deps).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PitchDependencyService {

  private final PitchDependencyRepository pitchDependencyRepository;
  private final PitchRepository pitchRepository;
  private final LocalizationService localizationService;

  /**
   * Add a dependency between pitches. Validates that the dependency doesn't create
   * a circular reference.
   */
  public PitchDependencyDTO addDependency(Long sourcePitchId, CreatePitchDependencyRequest request) {
    // Validate both pitches exist
    Pitch sourcePitch = pitchRepository.findByIdNotDeleted(sourcePitchId)
        .orElseThrow(() -> new IllegalArgumentException("Source pitch not found with id: " + sourcePitchId));

    Pitch targetPitch = pitchRepository.findByIdNotDeleted(request.getTargetPitchId()).orElseThrow(
        () -> new IllegalArgumentException("Target pitch not found with id: " + request.getTargetPitchId()));

    // Prevent self-dependencies
    if (sourcePitchId.equals(request.getTargetPitchId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.self.reference"));
    }

    // Check if dependency already exists
    if (pitchDependencyRepository.findBySourcePitchIdAndTargetPitchId(sourcePitchId, request.getTargetPitchId())
        .isPresent()) {
      throw new BadRequestException(localizationService.getMessage("dependency.already.exists"));
    }

    // Check for circular dependencies
    if (wouldCreateCircularDependency(sourcePitchId, request.getTargetPitchId())) {
      throw new BadRequestException(localizationService.getMessage("dependency.circular.reference"));
    }

    // Create the dependency
    PitchDependency dependency = PitchDependency.builder()
        .sourcePitch(sourcePitch)
        .targetPitch(targetPitch)
        .dependencyType(
            request.getDependencyType() != null ? request.getDependencyType() : DependencyType.BLOCKS)
        .build();

    PitchDependency saved = pitchDependencyRepository.save(dependency);
    return toDTO(saved);
  }

  /** Remove a dependency between pitches. */
  public void removeDependency(Long dependencyId) {
    if (!pitchDependencyRepository.existsById(dependencyId)) {
      throw new IllegalArgumentException(localizationService.getMessage("dependency.not.found", dependencyId));
    }
    pitchDependencyRepository.deleteById(dependencyId);
  }

  /**
   * Get all dependencies for a pitch (both outgoing/blocking and incoming/blockedBy).
   */
  public Map<String, List<PitchDependencyDTO>> getDependencies(Long pitchId) {
    if (!pitchRepository.existsById(pitchId)) {
      throw new IllegalArgumentException("Pitch not found with id: " + pitchId);
    }

    List<PitchDependency> outgoing = pitchDependencyRepository.findBySourcePitchId(pitchId);
    List<PitchDependency> incoming = pitchDependencyRepository.findByTargetPitchId(pitchId);

    Map<String, List<PitchDependencyDTO>> result = new HashMap<>();
    result.put("blocking", outgoing.stream().map(this::toDTO).collect(Collectors.toList()));
    result.put("blockedBy", incoming.stream().map(this::toDTO).collect(Collectors.toList()));

    return result;
  }

  /**
   * Check if adding a dependency would create a circular reference. Uses
   * depth-first search to detect cycles.
   */
  private boolean wouldCreateCircularDependency(Long sourcePitchId, Long targetPitchId) {
    // If target already depends on source (directly or indirectly), adding
    // source->target would create a cycle
    return hasPathBetweenPitches(targetPitchId, sourcePitchId, new HashSet<>());
  }

  /** Check if there's a dependency path from start to end pitch using DFS. */
  private boolean hasPathBetweenPitches(Long startPitchId, Long endPitchId, Set<Long> visited) {
    if (startPitchId.equals(endPitchId)) {
      return true;
    }

    if (visited.contains(startPitchId)) {
      return false;
    }

    visited.add(startPitchId);

    // Get all pitches that startPitch depends on (outgoing dependencies)
    List<PitchDependency> dependencies = pitchDependencyRepository.findBySourcePitchId(startPitchId);
    for (PitchDependency dep : dependencies) {
      if (hasPathBetweenPitches(dep.getTargetPitch().getId(), endPitchId, visited)) {
        return true;
      }
    }

    return false;
  }

  /** Convert entity to DTO. */
  public PitchDependencyDTO toDTO(PitchDependency dependency) {
    return PitchDependencyDTO.builder()
        .id(dependency.getId())
        .sourcePitchId(dependency.getSourcePitch().getId())
        .sourcePitchTitle(dependency.getSourcePitch().getTitle())
        .targetPitchId(dependency.getTargetPitch().getId())
        .targetPitchTitle(dependency.getTargetPitch().getTitle())
        .dependencyType(dependency.getDependencyType())
        .createdAt(dependency.getCreatedAt())
        .build();
  }
}
