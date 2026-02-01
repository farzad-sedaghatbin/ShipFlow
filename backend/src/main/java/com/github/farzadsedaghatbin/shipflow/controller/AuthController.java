package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserService userService;

  @PostMapping("/login")
  @Operation(summary = "Authenticate user and return JWT token")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = jwtTokenProvider.generateToken(authentication);

    User user = userService.findUserByUsername(request.getUsername());

    AuthResponse response =
        AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .userId(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .personId(user.getPerson() != null ? user.getPerson().getId() : null)
            .personName(user.getPerson() != null ? user.getPerson().getName() : null)
            .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/register")
  @Operation(summary = "Register a new user")
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
    UserDTO user = userService.createUser(request);
    return ResponseEntity.ok(user);
  }

  @GetMapping("/me")
  @Operation(summary = "Get current authenticated user info")
  public ResponseEntity<UserDTO> getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    UserDTO user = userService.findByUsername(username);
    return ResponseEntity.ok(user);
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Reset password using token")
  public ResponseEntity<Map<String, String>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(request);
    return ResponseEntity.ok(Map.of("message", "Password reset successful"));
  }

  @PostMapping("/validate-reset-token")
  @Operation(summary = "Validate if a password reset token is still valid")
  public ResponseEntity<Map<String, Boolean>> validateResetToken(@RequestParam String token) {
    boolean valid = userService.validateResetToken(token);
    return ResponseEntity.ok(Map.of("valid", valid));
  }
}
