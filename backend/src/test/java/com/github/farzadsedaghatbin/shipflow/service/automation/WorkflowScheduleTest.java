package com.github.farzadsedaghatbin.shipflow.service.automation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowSchedule")
class WorkflowScheduleTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private WorkflowSchedule parse(String json) {
    return WorkflowSchedule.parse(json, mapper);
  }

  @Test
  @DisplayName("A six-field cron fires at the next matching moment, in UTC by default")
  void sixFieldDefaultsToUtc() {
    WorkflowSchedule s = parse("{\"cron\":\"0 0 9 * * MON-FRI\"}");

    // Friday 2026-11-06 08:00 UTC → same day 09:00
    assertThat(s.next(Instant.parse("2026-11-06T08:00:00Z"))).contains(Instant.parse("2026-11-06T09:00:00Z"));
    // Friday 10:00 UTC → skips the weekend to Monday 09:00
    assertThat(s.next(Instant.parse("2026-11-06T10:00:00Z"))).contains(Instant.parse("2026-11-09T09:00:00Z"));
    assertThat(s.getZone()).isEqualTo(ZoneId.of("UTC"));
  }

  @Test
  @DisplayName("The timezone decides what '9 o'clock' means")
  void timezone() {
    WorkflowSchedule s = parse("{\"cron\":\"0 0 9 * * *\",\"timezone\":\"Asia/Tehran\"}");

    // 09:00 in Tehran (UTC+3:30, no DST since 2022) is 05:30 UTC
    assertThat(s.next(Instant.parse("2026-11-06T00:00:00Z"))).contains(Instant.parse("2026-11-06T05:30:00Z"));
  }

  @Test
  @DisplayName("The familiar five-field cron is accepted")
  void fiveField() {
    WorkflowSchedule s = parse("{\"cron\":\"30 8 * * MON\"}");

    assertThat(s.getCron()).isEqualTo("0 30 8 * * MON");
    assertThat(s.next(Instant.parse("2026-11-02T00:00:00Z"))).contains(Instant.parse("2026-11-02T08:30:00Z"));
  }

  @Test
  @DisplayName("A schedule must be present, JSON, with a cron")
  void missingPieces() {
    assertThatThrownBy(() -> parse(null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cron");
    assertThatThrownBy(() -> parse("  ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parse("{}")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cron");
    assertThatThrownBy(() -> parse("not json")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JSON");
  }

  @Test
  @DisplayName("A bad cron or timezone is explained, not swallowed")
  void invalid() {
    assertThatThrownBy(() -> parse("{\"cron\":\"every day\"}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not a valid cron");
    assertThatThrownBy(() -> parse("{\"cron\":\"0 0 9 * * *\",\"timezone\":\"Mars/Olympus\"}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timezone");
  }

  @Test
  @DisplayName("Schedules that fire more than once an hour are refused")
  void tooFrequent() {
    assertThatThrownBy(() -> parse("{\"cron\":\"0 * * * * *\"}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once an hour");
    assertThatThrownBy(() -> parse("{\"cron\":\"0 */15 * * * *\"}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once an hour");
    assertThatThrownBy(() -> parse("{\"cron\":\"0 0,30 9 * * *\"}"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Hourly is the fastest allowed")
  void hourlyAllowed() {
    assertThat(parse("{\"cron\":\"0 0 * * * *\"}").next(Instant.parse("2026-11-06T08:10:00Z")))
        .contains(Instant.parse("2026-11-06T09:00:00Z"));
  }

  @Test
  @DisplayName("A schedule that can never fire is refused")
  void neverFires() {
    assertThatThrownBy(() -> parse("{\"cron\":\"0 0 9 31 2 *\"}")).isInstanceOf(IllegalArgumentException.class);
  }
}
