package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicCycleDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/cycles")
@RequiredArgsConstructor
@Tag(name = "Public API – Cycles", description = "Public API endpoints for cycle resources")
public class PublicCycleController {

  private final CycleRepository cycleRepository;

  @GetMapping
  @Operation(summary = "List all cycles, newest first")
  public ResponseEntity<List<PublicCycleDTO>> listCycles(
      @RequestParam(required = false) Long projectId) {
    List<Cycle> cycles = projectId != null
        ? cycleRepository.findByProjectIdOrderByStartDateDesc(projectId)
        : cycleRepository.findAllByOrderByStartDateDesc();
    return ResponseEntity.ok(cycles.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a cycle by ID")
  public ResponseEntity<PublicCycleDTO> getCycle(@PathVariable Long id) {
    return cycleRepository.findById(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private PublicCycleDTO toDTO(Cycle c) {
    return PublicCycleDTO.builder()
        .id(c.getId())
        .projectId(c.getProject() != null ? c.getProject().getId() : null)
        .name(c.getName())
        .startDate(c.getStartDate())
        .endDate(c.getEndDate())
        .phase(c.getPhase() != null ? c.getPhase().name() : null)
        .isActive(c.getIsActive())
        .build();
  }
}
