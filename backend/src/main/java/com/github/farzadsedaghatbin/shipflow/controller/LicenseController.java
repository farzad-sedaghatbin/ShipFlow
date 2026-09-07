package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.license.LicenseStatusResponse;
import com.github.farzadsedaghatbin.shipflow.dto.license.UploadLicenseRequest;
import com.github.farzadsedaghatbin.shipflow.license.LicenseLimits;
import com.github.farzadsedaghatbin.shipflow.license.LicensePayload;
import com.github.farzadsedaghatbin.shipflow.license.LicenseService;
import com.github.farzadsedaghatbin.shipflow.license.LicenseStatus;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.WorkflowAutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Open-core licensing (v1.14.0): read the current licence status, and upload/remove the licence
 * file. See {@code LicenseService} for the verification contract.
 */
@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
@Tag(name = "Licensing", description = "Open-core licence status, upload and removal (v1.14.0)")
public class LicenseController {

  private final LicenseService licenseService;
  private final UserService userService;
  private final WorkflowAutomationService workflowAutomationService;

  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get the current licence status, edition, seats, and features")
  public ResponseEntity<LicenseStatusResponse> getStatus() {
    return ResponseEntity.ok(buildStatusResponse());
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Upload (or replace) the licence file",
      description = "Returns 400 license.invalid if the content is malformed or fails signature verification.")
  public ResponseEntity<LicenseStatusResponse> uploadLicense(@Valid @RequestBody UploadLicenseRequest request) {
    licenseService.uploadLicense(request.getContent());
    return ResponseEntity.ok(buildStatusResponse());
  }

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Remove the uploaded licence file",
      description = "Reverts to app.license.file (if configured) or Community Edition.")
  public ResponseEntity<LicenseStatusResponse> removeLicense() {
    licenseService.removeLicense();
    return ResponseEntity.ok(buildStatusResponse());
  }

  private LicenseStatusResponse buildStatusResponse() {
    LicenseStatus status = licenseService.getStatus();
    LicensePayload payload = licenseService.currentPayload().orElse(null);
    LocalDate expiresAt = payload != null ? payload.getExpiresAt() : null;

    return LicenseStatusResponse.builder()
        .status(status)
        .edition(payload != null ? payload.getEdition() : null)
        .licensee(payload != null ? payload.getLicensee() : null)
        .seats(payload != null ? payload.getSeats() : null)
        .seatsUsed(userService.countActiveUsers())
        .features(payload != null && payload.getFeatures() != null ? payload.getFeatures() : List.of())
        .issuedAt(payload != null ? payload.getIssuedAt() : null)
        .expiresAt(expiresAt)
        .supportUntil(payload != null ? payload.getSupportUntil() : null)
        .graceEndsAt(expiresAt != null ? expiresAt.plusDays(LicenseService.GRACE_PERIOD_DAYS) : null)
        .communityCap(LicenseLimits.COMMUNITY_USER_SEAT_CAP)
        .automationCap(LicenseLimits.COMMUNITY_AUTOMATION_CAP)
        .automationsUsed(workflowAutomationService.countEnabledAutomations())
        .build();
  }
}
