package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachmentDTO {
  private Long id;
  private Long taskId;
  private String fileName;
  private Long fileSize;
  private String contentType;
  private Long uploadedById;
  private String uploadedByUsername;
  private LocalDateTime createdAt;
}
