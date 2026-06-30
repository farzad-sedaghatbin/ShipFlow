package com.github.farzadsedaghatbin.shipflow.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionTestResponse {
  private boolean ok;
  private String message;
}
