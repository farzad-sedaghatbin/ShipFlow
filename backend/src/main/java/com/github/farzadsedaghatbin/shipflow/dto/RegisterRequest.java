package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Password is required")
  private String password;

  // NOTE: Only honoured when POST /api/auth/register is called by an authenticated
  // ADMIN (the "Add User" flow in User Management). For any other caller — i.e. a
  // genuine public self-registration — UserService#createUser ignores this value
  // entirely and assigns app.auth.default-role instead. Still @NotNull so the
  // request shape doesn't change for existing callers; the value just isn't used.
  @NotNull(message = "Role is required")
  private UserRole role;

  private Long personId; // Optional: link to existing person

  private List<Long> projectIds; // Optional: when null/empty, all active projects are assigned
}
