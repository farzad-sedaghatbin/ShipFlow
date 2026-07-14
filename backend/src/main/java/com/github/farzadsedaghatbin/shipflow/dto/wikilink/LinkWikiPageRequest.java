package com.github.farzadsedaghatbin.shipflow.dto.wikilink;

import jakarta.validation.constraints.NotNull;

/** Request body to link an existing wiki page to a Pitch or Task. */
public record LinkWikiPageRequest(@NotNull Long wikiPageId) {}
