package com.github.farzadsedaghatbin.shipflow.plugin;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SampleRiskCalculatorPluginTest {

  private final SampleRiskCalculatorPlugin plugin = new SampleRiskCalculatorPlugin();

  @Test
  void pluginId_shouldBeCorrect() {
    assertThat(plugin.getPluginId()).isEqualTo("sample-risk-heuristic");
  }

  @Test
  void displayName_shouldContainDevIndicator() {
    assertThat(plugin.getDisplayName()).containsIgnoringCase("dev");
  }

  @Test
  void calculateRisk_shouldReturnHighRisk_whenBehindSchedule() {
    Map<String, Object> context = Map.of(
        "taskCount", 10,
        "completedTaskCount", 2,
        "daysRemaining", 3);

    Double risk = plugin.calculateRisk(context);

    assertThat(risk).isEqualTo(0.85);
  }

  @Test
  void calculateRisk_shouldReturnModerateRisk_whenSlightlyBehind() {
    Map<String, Object> context = Map.of(
        "taskCount", 10,
        "completedTaskCount", 2,
        "daysRemaining", 7);

    Double risk = plugin.calculateRisk(context);

    assertThat(risk).isEqualTo(0.6);
  }

  @Test
  void calculateRisk_shouldReturnLowRisk_whenOnTrack() {
    Map<String, Object> context = Map.of(
        "taskCount", 10,
        "completedTaskCount", 8,
        "daysRemaining", 15);

    Double risk = plugin.calculateRisk(context);

    assertThat(risk).isEqualTo(0.2);
  }

  @Test
  void calculateRisk_shouldReturnNull_whenNoTasks() {
    Map<String, Object> context = Map.of(
        "taskCount", 0,
        "completedTaskCount", 0,
        "daysRemaining", 30);

    Double risk = plugin.calculateRisk(context);

    assertThat(risk).isNull();
  }
}
