package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.scim.ScimListResponse;
import com.github.farzadsedaghatbin.shipflow.dto.scim.ScimUser;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.service.ScimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * SCIM 2.0 Users endpoint.
 *
 * <p>Authentication is bearer-token based — validated by {@link ScimService#validateScimToken}. No
 * {@code @PreAuthorize} is used here because there is no Spring Security principal; the JWT filter
 * does not run for {@code /scim/**} paths (see SecurityConfig).
 *
 * <p>All responses use {@code application/scim+json} as required by RFC 7644.
 */
@RestController
@RequestMapping("/scim/v2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SCIM 2.0", description = "SCIM 2.0 user provisioning endpoints")
public class ScimController {

  static final String SCIM_MEDIA_TYPE = "application/scim+json";

  private final ScimService scimService;

  // ── List Users ────────────────────────────────────────────────────────────

  @GetMapping(value = "/Users", produces = SCIM_MEDIA_TYPE)
  @Operation(summary = "List users (SCIM 2.0)")
  public ResponseEntity<ScimListResponse> listUsers(
      @RequestParam(defaultValue = "1") int startIndex,
      @RequestParam(defaultValue = "100") int count,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    ScimListResponse response = scimService.listUsers(startIndex, count);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(SCIM_MEDIA_TYPE)).body(response);
  }

  // ── Get User ──────────────────────────────────────────────────────────────

  @GetMapping(value = "/Users/{id}", produces = SCIM_MEDIA_TYPE)
  @Operation(summary = "Get a user by id (SCIM 2.0)")
  public ResponseEntity<ScimUser> getUser(
      @PathVariable String id,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    ScimUser user = scimService.getUser(id);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(SCIM_MEDIA_TYPE)).body(user);
  }

  // ── Create User ───────────────────────────────────────────────────────────

  @PostMapping(value = "/Users",
      consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
      produces = SCIM_MEDIA_TYPE)
  @Operation(summary = "Create a user (SCIM 2.0)")
  public ResponseEntity<ScimUser> createUser(
      @RequestBody ScimUser scimUser,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    ScimUser created = scimService.createUser(scimUser);
    return ResponseEntity.status(HttpStatus.CREATED)
        .contentType(MediaType.parseMediaType(SCIM_MEDIA_TYPE))
        .body(created);
  }

  // ── Replace User (PUT) ────────────────────────────────────────────────────

  @PutMapping(value = "/Users/{id}",
      consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
      produces = SCIM_MEDIA_TYPE)
  @Operation(summary = "Replace a user (SCIM 2.0)")
  public ResponseEntity<ScimUser> replaceUser(
      @PathVariable String id,
      @RequestBody ScimUser scimUser,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    ScimUser updated = scimService.replaceUser(id, scimUser);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(SCIM_MEDIA_TYPE)).body(updated);
  }

  // ── Patch User ────────────────────────────────────────────────────────────

  @PatchMapping(value = "/Users/{id}",
      consumes = {SCIM_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE},
      produces = SCIM_MEDIA_TYPE)
  @Operation(summary = "Patch a user (SCIM 2.0)")
  public ResponseEntity<ScimUser> patchUser(
      @PathVariable String id,
      @RequestBody Map<String, Object> patchBody,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    ScimUser updated = scimService.patchUser(id, patchBody);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(SCIM_MEDIA_TYPE)).body(updated);
  }

  // ── Delete User ───────────────────────────────────────────────────────────

  @DeleteMapping("/Users/{id}")
  @Operation(summary = "Delete a user (SCIM 2.0)")
  public ResponseEntity<Void> deleteUser(
      @PathVariable String id,
      HttpServletRequest request) {
    scimService.validateScimToken(request.getHeader("Authorization"));
    scimService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}
