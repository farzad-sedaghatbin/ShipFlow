package com.github.farzadsedaghatbin.shipflow.dto.push;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Request body for removing a browser's Web Push subscription. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushUnsubscribeRequest {

  @NotBlank private String endpoint;
}
