package com.github.farzadsedaghatbin.shipflow.dto;

/**
 * Aggregated summary statistics for work logs.
 * Returned by {@code GET /api/worklogs/my/summary} to power the summary cards
 * in the Work Logs page without being affected by pagination.
 *
 * @param todayHours  total hours logged by the current user today
 * @param todayCount  number of work log entries logged today
 * @param totalHours  total hours for the current filter (cycle / project / all)
 * @param totalCount  total number of entries for the current filter
 * @since 0.6.0
 */
public record WorkLogSummaryDTO(
    double todayHours,
    long todayCount,
    double totalHours,
    long totalCount) {
}
