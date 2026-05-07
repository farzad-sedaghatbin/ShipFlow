package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordRequest {

  @NotBlank(message = "New password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  private String newPassword;
}
