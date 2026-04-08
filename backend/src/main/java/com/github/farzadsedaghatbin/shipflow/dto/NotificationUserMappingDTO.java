package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUserMappingDTO {

  private Long id;
  private Long personId;
  private String providerName;
  private String externalUserId;
}
