package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicReleaseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/releases")
@RequiredArgsConstructor
@Tag(name = "Public API – Releases", description = "Public API endpoints for release resources")
public class PublicReleaseController {

  private final ReleaseRepository releaseRepository;

  @GetMapping
  @Operation(summary = "List all releases (not deleted)")
  public ResponseEntity<List<PublicReleaseDTO>> listReleases(
      @RequestParam(required = false) Long projectId) {
    List<Release> releases = projectId != null
        ? releaseRepository.findByProjectIdNotDeleted(projectId)
        : releaseRepository.findAllNotDeleted();
    return ResponseEntity.ok(releases.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a release by ID")
  public ResponseEntity<PublicReleaseDTO> getRelease(@PathVariable Long id) {
    return releaseRepository.findByIdNotDeleted(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private PublicReleaseDTO toDTO(Release r) {
    return PublicReleaseDTO.builder()
        .id(r.getId())
        .name(r.getName())
        .version(r.getVersion())
        .description(r.getDescription())
        .status(r.getStatus() != null ? r.getStatus().name() : null)
        .riskLevel(r.getRiskLevel() != null ? r.getRiskLevel().name() : null)
        .targetDate(r.getTargetDate())
        .releaseDate(r.getReleaseDate())
        .releaseNotes(r.getReleaseNotes())
        .projectId(r.getProject() != null ? r.getProject().getId() : null)
        .createdAt(r.getCreatedAt())
        .updatedAt(r.getUpdatedAt())
        .build();
  }
}
