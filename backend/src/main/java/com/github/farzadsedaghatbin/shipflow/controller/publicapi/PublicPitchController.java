package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicPitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.security.PublicApiAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/pitches")
@RequiredArgsConstructor
@Tag(name = "Public API – Pitches", description = "Public API endpoints for pitch resources")
public class PublicPitchController {

  private final PitchRepository pitchRepository;
  private final PublicApiAuthorizationService publicApiAuthorizationService;

  @GetMapping
  @Operation(summary = "List all pitches (not deleted)")
  public ResponseEntity<List<PublicPitchDTO>> listPitches(
      @RequestParam(required = false) Long cycleId) {
    Long restrictedProjectId = publicApiAuthorizationService.restrictedProjectIdOrNull();
    List<Pitch> pitches;
    if (restrictedProjectId != null) {
      // Scope at the query level to the restricted project first; cycleId (if also supplied) is
      // then a small in-memory refinement over that already-project-scoped result set, not a
      // second unrestricted query.
      pitches = pitchRepository.findByProjectIdNotDeleted(restrictedProjectId);
      if (cycleId != null) {
        pitches = pitches.stream()
            .filter(p -> p.getCycle() != null && cycleId.equals(p.getCycle().getId()))
            .toList();
      }
    } else if (cycleId != null) {
      pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    } else {
      pitches = pitchRepository.findAllNotDeleted();
    }
    return ResponseEntity.ok(pitches.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a pitch by ID")
  public ResponseEntity<PublicPitchDTO> getPitch(@PathVariable Long id) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(id).orElse(null);
    if (pitch == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(), resolveProjectId(pitch));
    return ResponseEntity.ok(toDTO(pitch));
  }

  /**
   * A pitch's project can come from either its own direct {@code project} field (e.g. a
   * pre-cycle pitch) or its cycle's project — mirrors {@link PitchRepository#findByProjectIdNotDeleted}'s
   * resolution so the list and single-item paths never disagree about which project a pitch
   * belongs to.
   */
  private Long resolveProjectId(Pitch p) {
    if (p.getProject() != null) {
      return p.getProject().getId();
    }
    if (p.getCycle() != null && p.getCycle().getProject() != null) {
      return p.getCycle().getProject().getId();
    }
    return null;
  }

  private PublicPitchDTO toDTO(Pitch p) {
    return PublicPitchDTO.builder()
        .id(p.getId())
        .title(p.getTitle())
        .description(p.getDescription())
        .appetiteDays(p.getAppetiteDays())
        .problemStatement(p.getProblemStatement())
        .solution(p.getSolution())
        .status(p.getStatus() != null ? p.getStatus().name() : null)
        .cycleId(p.getCycle() != null ? p.getCycle().getId() : null)
        .epicId(p.getEpic() != null ? p.getEpic().getId() : null)
        .targetReleaseId(p.getTargetRelease() != null ? p.getTargetRelease().getId() : null)
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .build();
  }
}
