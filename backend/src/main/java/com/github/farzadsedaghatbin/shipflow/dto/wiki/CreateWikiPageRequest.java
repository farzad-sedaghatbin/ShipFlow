package com.github.farzadsedaghatbin.shipflow.dto.wiki;

public record CreateWikiPageRequest(Long spaceId, Long parentId, String title, String content) {}
