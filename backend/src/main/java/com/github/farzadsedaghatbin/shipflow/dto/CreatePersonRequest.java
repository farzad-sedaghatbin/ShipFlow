package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePersonRequest {
  @NotBlank(message = "Name is required")
  private String name;

  @Email(message = "Invalid email format")
  private String email;

  private String avatarUrl;
  private String department;
  private String skills;
  private String bio;
  private Boolean isActive = true;
}
