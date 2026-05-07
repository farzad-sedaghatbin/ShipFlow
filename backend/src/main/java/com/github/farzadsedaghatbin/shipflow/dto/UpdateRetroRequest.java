package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRetroRequest {
  private String title;
  private String notes;
}
