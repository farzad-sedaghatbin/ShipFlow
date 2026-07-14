package com.github.farzadsedaghatbin.shipflow.dto.wikilink;

import java.time.LocalDateTime;

/** A wiki page linked to a Pitch or Task for reference (research/docs). */
public record LinkedWikiPageDTO(
    Long linkId,
    Long wikiPageId,
    String title,
    String slug,
    Long spaceId,
    String spaceName,
    LocalDateTime linkedAt,
    String linkedByName) {}
