package com.github.farzadsedaghatbin.shipflow.service.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.scheduling.support.CronExpression;

/**
 * A SCHEDULE automation's timing, parsed from its trigger config:
 *
 * <pre>{"cron":"0 0 9 * * MON-FRI","timezone":"Europe/Berlin"}</pre>
 *
 * <p>{@code cron} is Spring's six-field form (second minute hour day-of-month month weekday); the
 * familiar five-field form is accepted and given a leading {@code 0} second. {@code timezone}
 * defaults to UTC. Schedules that would fire more often than {@link #MIN_GAP} are rejected: these
 * loops post messages to people, so "every minute" is a mistake, not a feature.
 */
public final class WorkflowSchedule {

  /** The closest two consecutive runs may be. */
  public static final Duration MIN_GAP = Duration.ofMinutes(60);

  private final CronExpression cron;
  private final ZoneId zone;
  private final String cronText;

  private WorkflowSchedule(CronExpression cron, ZoneId zone, String cronText) {
    this.cron = cron;
    this.zone = zone;
    this.cronText = cronText;
  }

  /**
   * @throws IllegalArgumentException with a message fit to show a user when the config is missing,
   *     unreadable, has a bad cron/timezone, or fires too often
   */
  public static WorkflowSchedule parse(String triggerConfigJson, ObjectMapper objectMapper) {
    if (triggerConfigJson == null || triggerConfigJson.isBlank()) {
      throw new IllegalArgumentException("A schedule needs a cron expression, e.g. {\"cron\":\"0 0 9 * * MON-FRI\"}");
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(triggerConfigJson);
    } catch (Exception e) {
      throw new IllegalArgumentException("The schedule is not valid JSON");
    }
    String cronRaw = root != null && root.hasNonNull("cron") ? root.get("cron").asText().trim() : "";
    if (cronRaw.isEmpty()) {
      throw new IllegalArgumentException("A schedule needs a cron expression, e.g. {\"cron\":\"0 0 9 * * MON-FRI\"}");
    }
    String normalized = cronRaw.replaceAll("\\s+", " ");
    if (normalized.split(" ").length == 5) {
      normalized = "0 " + normalized; // standard 5-field cron: minute hour day month weekday
    }
    CronExpression cron;
    try {
      cron = CronExpression.parse(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "'" + cronRaw + "' is not a valid cron expression (expected: second minute hour day-of-month month weekday)");
    }
    String zoneText = root.hasNonNull("timezone") ? root.get("timezone").asText().trim() : "UTC";
    ZoneId zone;
    try {
      zone = ZoneId.of(zoneText.isEmpty() ? "UTC" : zoneText);
    } catch (Exception e) {
      throw new IllegalArgumentException("'" + zoneText + "' is not a known timezone (e.g. UTC, Europe/Berlin, Asia/Tehran)");
    }
    WorkflowSchedule schedule = new WorkflowSchedule(cron, zone, normalized);
    schedule.requireNotTooFrequent();
    return schedule;
  }

  /** The first run strictly after {@code after}, or empty if the expression never fires again. */
  public Optional<Instant> next(Instant after) {
    ZonedDateTime n = cron.next(ZonedDateTime.ofInstant(after, zone));
    return Optional.ofNullable(n).map(ZonedDateTime::toInstant);
  }

  public ZoneId getZone() {
    return zone;
  }

  public String getCron() {
    return cronText;
  }

  private void requireNotTooFrequent() {
    // Look at the next few runs from a fixed reference so the answer doesn't depend on "now".
    Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
    Instant a = next(t0).orElse(null);
    if (a == null) {
      throw new IllegalArgumentException("This schedule never fires");
    }
    Instant prev = a;
    for (int i = 0; i < 6; i++) {
      Instant n = next(prev).orElse(null);
      if (n == null) {
        return;
      }
      if (Duration.between(prev, n).compareTo(MIN_GAP) < 0) {
        throw new IllegalArgumentException(
            "This schedule fires more than once an hour. Agent loops post messages to people, so the"
                + " minimum gap between runs is " + MIN_GAP.toMinutes() + " minutes");
      }
      prev = n;
    }
  }
}
