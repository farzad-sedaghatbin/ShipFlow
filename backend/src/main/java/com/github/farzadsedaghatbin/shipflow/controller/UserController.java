package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.ChangePasswordRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateProfileRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UserProfileDTO;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

  private final UserService userService;

  @GetMapping
  @Operation(summary = "Get all users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserDTO>> getAllUsers() {
    return ResponseEntity.ok(userService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID")
  @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
  public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  @PutMapping("/{id}/role")
  @Operation(summary = "Update user role")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDTO> updateRole(@PathVariable Long id, @RequestParam UserRole role) {
    return ResponseEntity.ok(userService.updateRole(id, role));
  }

  @PutMapping("/{id}/link-person/{personId}")
  @Operation(summary = "Link user to a person")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDTO> linkToPerson(@PathVariable Long id, @PathVariable Long personId) {
    return ResponseEntity.ok(userService.linkToPerson(id, personId));
  }

  @PutMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate user")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDTO> deactivateUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.deactivate(id));
  }

  @PutMapping("/{id}/activate")
  @Operation(summary = "Activate user")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserDTO> activateUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.activate(id));
  }

  @PutMapping("/{id}/password")
  @Operation(summary = "Change user password (admin can change any, user can change own)")
  @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
  public ResponseEntity<Void> changePassword(
      @PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(id, request);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/profile")
  @Operation(summary = "Get user profile with avatar and person details")
  @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
  public ResponseEntity<UserProfileDTO> getProfile(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getProfile(id));
  }

  @GetMapping("/me/profile")
  @Operation(summary = "Get current user's profile")
  public ResponseEntity<UserProfileDTO> getCurrentUserProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return ResponseEntity.ok(userService.getProfileByUsername(username));
  }

  @PutMapping("/{id}/profile")
  @Operation(summary = "Update user profile (avatar, bio, skills, etc.)")
  @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
  public ResponseEntity<UserProfileDTO> updateProfile(
      @PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(userService.updateProfile(id, request));
  }

  @PutMapping("/me/profile")
  @Operation(summary = "Update current user's profile")
  public ResponseEntity<UserProfileDTO> updateCurrentUserProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    UserProfileDTO profile = userService.getProfileByUsername(username);
    return ResponseEntity.ok(userService.updateProfile(profile.getId(), request));
  }
}
