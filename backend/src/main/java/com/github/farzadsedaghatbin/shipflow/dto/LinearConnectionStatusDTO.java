package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Status of the Linear OAuth connection (v1.2.0 S29). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinearConnectionStatusDTO {
  /** True if a Linear access token has been stored. */
  private boolean connected;
  /** True if client-id and client-secret are present in environment. */
  private boolean configured;
  /** Selected team ID, null if no team selected yet. */
  private String teamId;
  /** Selected team display name, null if no team selected yet. */
  private String teamName;
}
