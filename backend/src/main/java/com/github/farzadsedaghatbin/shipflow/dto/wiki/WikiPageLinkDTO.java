package com.github.farzadsedaghatbin.shipflow.dto.wiki;

/**
 * A resolved internal wiki link parsed from a page's {@code [[pageId]]} tokens. {@code exists} is
 * false when the referenced page is missing or soft-deleted, so the UI can render a broken-link
 * affordance instead of a dead navigation. {@code url} is the canonical in-app route.
 */
public record WikiPageLinkDTO(Long pageId, String title, Long spaceId, boolean exists, String url) {}
