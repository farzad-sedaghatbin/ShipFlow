package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.time.OffsetDateTime;

public record WikiPageDTO(
    Long id,
    Long spaceId,
    Long parentId,
    String title,
    String slug,
    String content,
    String contentText,
    int position,
    Long createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
