package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertNotificationMappingRequest {

  @NotBlank
  private String providerName;

  @NotBlank
  private String externalUserId;
}
