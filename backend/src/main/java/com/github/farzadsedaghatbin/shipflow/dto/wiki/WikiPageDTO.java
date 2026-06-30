package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.time.OffsetDateTime;
import java.util.List;

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
    OffsetDateTime updatedAt,
    // Resolved internal [[pageId]] links found in the page body (empty when none).
    List<WikiPageLinkDTO> pageLinks) {}
