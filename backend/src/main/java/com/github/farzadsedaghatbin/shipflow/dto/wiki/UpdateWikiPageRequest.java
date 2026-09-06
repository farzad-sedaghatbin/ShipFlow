package com.github.farzadsedaghatbin.shipflow.dto.wiki;

/**
 * @param expectedVersion Optimistic-lock check (v1.13.0 S64). When present, must match the
 *     page's current {@code version} or the update is rejected with a 409 conflict. {@code null}
 *     skips the check.
 */
public record UpdateWikiPageRequest(String title, String content, Long expectedVersion) {}
