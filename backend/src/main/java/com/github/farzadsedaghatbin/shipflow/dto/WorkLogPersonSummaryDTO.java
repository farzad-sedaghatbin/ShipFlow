package com.github.farzadsedaghatbin.shipflow.dto;

/**
 * Aggregated work-log summary for a single person on a pitch.
 * Returned by {@code GET /api/worklogs/pitch/{pitchId}/by-person}.
 */
public record WorkLogPersonSummaryDTO(
    Long personId,
    String personName,
    double totalHours,
    long entryCount) {}
