package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicCycleDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.security.PublicApiAuthorizationService;
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
  private final PublicApiAuthorizationService publicApiAuthorizationService;

  @GetMapping
  @Operation(summary = "List all cycles, newest first")
  public ResponseEntity<List<PublicCycleDTO>> listCycles(
      @RequestParam(required = false) Long projectId) {
    Long restrictedProjectId = publicApiAuthorizationService.restrictedProjectIdOrNull();
    List<Cycle> cycles;
    if (restrictedProjectId != null) {
      // Project restriction supersedes any caller-supplied projectId, same precedent as
      // PublicRoadmapController.listEpics.
      cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(restrictedProjectId);
    } else if (projectId != null) {
      cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);
    } else {
      cycles = cycleRepository.findAllByOrderByStartDateDesc();
    }
    return ResponseEntity.ok(cycles.stream().map(this::toDTO).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a cycle by ID")
  public ResponseEntity<PublicCycleDTO> getCycle(@PathVariable Long id) {
    Cycle cycle = cycleRepository.findById(id).orElse(null);
    if (cycle == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(),
        cycle.getProject() != null ? cycle.getProject().getId() : null);
    return ResponseEntity.ok(toDTO(cycle));
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
