package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicPitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
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

  @GetMapping
  @Operation(summary = "List all pitches (not deleted)")
  public ResponseEntity<List<PublicPitchDTO>> listPitches(
      @RequestParam(required = false) Long cycleId) {
    List<Pitch> pitches = cycleId != null
        ? pitchRepository.findByCycleIdNotDeleted(cycleId)
        : pitchRepository.findAllNotDeleted();
    return ResponseEntity.ok(pitches.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a pitch by ID")
  public ResponseEntity<PublicPitchDTO> getPitch(@PathVariable Long id) {
    return pitchRepository.findByIdNotDeleted(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
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
