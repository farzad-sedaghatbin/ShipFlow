package com.github.farzadsedaghatbin.shipflow.dto.passkey;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyLoginOptionsRequest {

  @NotBlank
  private String username;
}
