package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicBugDTO;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/bugs")
@RequiredArgsConstructor
@Tag(name = "Public API – Bugs", description = "Public API endpoints for bug report resources")
public class PublicBugController {

  private final BugReportRepository bugReportRepository;

  @GetMapping
  @Operation(summary = "List bug reports with pagination")
  public ResponseEntity<Page<PublicBugDTO>> listBugs(
      @RequestParam(required = false) Long cycleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<BugReport> bugs = cycleId != null
        ? bugReportRepository.findByCycleId(cycleId, pageable)
        : bugReportRepository.findAll(pageable);
    return ResponseEntity.ok(bugs.map(this::toDTO));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a bug report by ID")
  public ResponseEntity<PublicBugDTO> getBug(@PathVariable Long id) {
    return bugReportRepository.findById(id)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/by-key/{bugKey}")
  @Operation(summary = "Get a bug report by its unique key")
  public ResponseEntity<PublicBugDTO> getBugByKey(@PathVariable String bugKey) {
    return bugReportRepository.findByBugKey(bugKey)
        .map(this::toDTO)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private PublicBugDTO toDTO(BugReport b) {
    return PublicBugDTO.builder()
        .id(b.getId())
        .bugKey(b.getBugKey())
        .title(b.getTitle())
        .description(b.getDescription())
        .severity(b.getSeverity() != null ? b.getSeverity().name() : null)
        .status(b.getStatus() != null ? b.getStatus().name() : null)
        .projectId(b.getProject() != null ? b.getProject().getId() : null)
        .pitchId(b.getPitch() != null ? b.getPitch().getId() : null)
        .cycleId(b.getCycle() != null ? b.getCycle().getId() : null)
        .assigneeId(b.getAssignee() != null ? b.getAssignee().getId() : null)
        .assigneeName(b.getAssignee() != null ? b.getAssignee().getName() : null)
        .targetReleaseId(b.getTargetRelease() != null ? b.getTargetRelease().getId() : null)
        .fixedInReleaseId(b.getFixedInRelease() != null ? b.getFixedInRelease().getId() : null)
        .resolution(b.getResolution())
        .resolvedAt(b.getResolvedAt())
        .createdAt(b.getCreatedAt())
        .updatedAt(b.getUpdatedAt())
        .build();
  }
}
