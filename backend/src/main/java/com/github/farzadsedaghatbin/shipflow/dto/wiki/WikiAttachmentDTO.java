package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.time.OffsetDateTime;

public record WikiAttachmentDTO(
    Long id,
    Long pageId,
    String fileName,
    String contentType,
    Long fileSize,
    Long uploadedBy,
    OffsetDateTime createdAt) {}
