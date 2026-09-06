package com.github.farzadsedaghatbin.shipflow.controller.publicapi;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.PublicBugDTO;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.security.PublicApiAuthorizationService;
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
  private final PublicApiAuthorizationService publicApiAuthorizationService;

  @GetMapping
  @Operation(summary = "List bug reports with pagination")
  public ResponseEntity<Page<PublicBugDTO>> listBugs(
      @RequestParam(required = false) Long cycleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
    Long restrictedProjectId = publicApiAuthorizationService.restrictedProjectIdOrNull();
    Page<BugReport> bugs;
    if (restrictedProjectId != null) {
      // Project restriction supersedes any caller-supplied cycleId, same precedent as
      // PublicTaskController.listTasks.
      bugs = bugReportRepository.findByProjectId(restrictedProjectId, pageable);
    } else if (cycleId != null) {
      bugs = bugReportRepository.findByCycleId(cycleId, pageable);
    } else {
      bugs = bugReportRepository.findAll(pageable);
    }
    return ResponseEntity.ok(bugs.map(this::toDTO));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a bug report by ID")
  public ResponseEntity<PublicBugDTO> getBug(@PathVariable Long id) {
    BugReport bug = bugReportRepository.findById(id).orElse(null);
    if (bug == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(),
        bug.getProject() != null ? bug.getProject().getId() : null);
    return ResponseEntity.ok(toDTO(bug));
  }

  @GetMapping("/by-key/{bugKey}")
  @Operation(summary = "Get a bug report by its unique key")
  public ResponseEntity<PublicBugDTO> getBugByKey(@PathVariable String bugKey) {
    BugReport bug = bugReportRepository.findByBugKey(bugKey).orElse(null);
    if (bug == null) {
      return ResponseEntity.notFound().build();
    }
    publicApiAuthorizationService.requireProjectAccess(
        publicApiAuthorizationService.currentApiKey(),
        bug.getProject() != null ? bug.getProject().getId() : null);
    return ResponseEntity.ok(toDTO(bug));
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
