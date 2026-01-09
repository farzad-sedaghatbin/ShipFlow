package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for document uploads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String extractedText;
    private String storagePath;
    private boolean textExtracted;
    private String errorMessage;
}
