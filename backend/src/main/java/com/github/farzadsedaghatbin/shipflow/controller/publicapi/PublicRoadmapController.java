package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicEpicDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/roadmap")
@RequiredArgsConstructor
@Tag(name = "Public API – Roadmap", description = "Public API endpoints for roadmap items (epics)")
public class PublicRoadmapController {

  private final EpicRepository epicRepository;

  @GetMapping
  @Operation(summary = "List all roadmap items (epics, not deleted)")
  public ResponseEntity<List<PublicEpicDTO>> listEpics(
      @RequestParam(required = false) Long projectId) {
    List<Epic> epics = projectId != null
        ? epicRepository.findByProjectIdNotDeleted(projectId)
        : epicRepository.findAllNotDeleted();
    return ResponseEntity.ok(epics.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a roadmap item (epic) by ID")
  public ResponseEntity<PublicEpicDTO> getEpic(@PathVariable Long id) {
    return epicRepository.findByIdNotDeleted(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private PublicEpicDTO toDTO(Epic e) {
    return PublicEpicDTO.builder()
        .id(e.getId())
        .name(e.getName())
        .description(e.getDescription())
        .status(e.getStatus() != null ? e.getStatus().name() : null)
        .color(e.getColor())
        .targetStartDate(e.getTargetStartDate())
        .targetEndDate(e.getTargetEndDate())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .initiativeId(e.getInitiative() != null ? e.getInitiative().getId() : null)
        .ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
        .sortOrder(e.getSortOrder())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }
}
