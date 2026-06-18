package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.time.OffsetDateTime;

public record WikiSpaceDTO(
    Long id,
    String name,
    String spaceKey,
    Long createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
