package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A Linear team available for import (v1.2.0 S29). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinearTeamDTO {
  private String id;
  private String name;
  private String key;
}
