package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.audit.AuthAuditLogDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditOutcome;
import com.github.farzadsedaghatbin.shipflow.repository.AuthAuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to the authentication audit trail.
 *
 * <p>Admin-only: the log contains IP addresses and User-Agent strings for
 * every account, which is exactly the kind of data that should not be
 * browsable by ordinary members.
 */
@RestController
@RequestMapping("/api/audit/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Audit", description = "Who signed in, from where, on what device")
public class AuthAuditController {

  private static final int MAX_PAGE_SIZE = 200;

  private final AuthAuditLogRepository repository;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "List authentication events, newest first, with optional filters")
  public ResponseEntity<Page<AuthAuditLogDTO>> list(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String ipAddress,
      @RequestParam(required = false) AuthAuditOutcome outcome,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    int cappedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

    Page<AuthAuditLogDTO> result = repository
        .search(blankToNull(username), blankToNull(ipAddress), outcome, from, to,
            PageRequest.of(Math.max(page, 0), cappedSize))
        .map(AuthAuditLogDTO::from);

    return ResponseEntity.ok(result);
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
