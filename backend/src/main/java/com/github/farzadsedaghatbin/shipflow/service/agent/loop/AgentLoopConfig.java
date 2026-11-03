package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * How an agent-loop automation delivers: parsed from the automation's action config, e.g.
 *
 * <pre>{"channels":["SLACK","TEAMS","IN_APP"],"channel":"#standup","lookbackHours":24,
 *  "onlyWhenAtRisk":true,"useAi":true}</pre>
 *
 * Missing fields take safe defaults (in-app only, 24 h, AI on); anything present but wrong is
 * rejected with a message fit to show the person configuring it.
 */
public final class AgentLoopConfig {

  /** Where the report goes. */
  public enum Channel {
    SLACK,
    TEAMS,
    IN_APP
  }

  public static final int MIN_LOOKBACK_HOURS = 1;
  public static final int MAX_LOOKBACK_HOURS = 168;

  private static final Pattern CHANNEL_NAME = Pattern.compile("[#@]?[A-Za-z0-9_.-]{1,80}");

  private final Set<Channel> channels;
  private final String channel;
  private final int lookbackHours;
  private final boolean onlyWhenAtRisk;
  private final boolean useAi;

  private AgentLoopConfig(
      Set<Channel> channels, String channel, int lookbackHours, boolean onlyWhenAtRisk, boolean useAi) {
    this.channels = channels;
    this.channel = channel;
    this.lookbackHours = lookbackHours;
    this.onlyWhenAtRisk = onlyWhenAtRisk;
    this.useAi = useAi;
  }

  public static AgentLoopConfig parse(String json, ObjectMapper objectMapper) {
    JsonNode root;
    try {
      root = json == null || json.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(json);
    } catch (Exception e) {
      throw new IllegalArgumentException("The action settings are not valid JSON");
    }
    if (root == null || !root.isObject()) {
      throw new IllegalArgumentException("The action settings must be a JSON object");
    }

    Set<Channel> channels = EnumSet.noneOf(Channel.class);
    if (root.has("channels")) {
      if (!root.get("channels").isArray()) {
        throw new IllegalArgumentException("'channels' must be a list such as [\"SLACK\",\"IN_APP\"]");
      }
      for (JsonNode c : root.get("channels")) {
        try {
          channels.add(Channel.valueOf(c.asText().trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Unknown channel '" + c.asText() + "'. Use SLACK, TEAMS or IN_APP");
        }
      }
      if (channels.isEmpty()) {
        throw new IllegalArgumentException("Pick at least one channel to send the report to");
      }
    } else {
      channels.add(Channel.IN_APP);
    }

    String channel = null;
    if (root.hasNonNull("channel") && !root.get("channel").asText().isBlank()) {
      channel = root.get("channel").asText().trim();
      if (!CHANNEL_NAME.matcher(channel).matches()) {
        throw new IllegalArgumentException("'" + channel + "' is not a valid channel name");
      }
    }

    int lookback = 24;
    if (root.has("lookbackHours")) {
      if (!root.get("lookbackHours").canConvertToInt() || !root.get("lookbackHours").isNumber()) {
        throw new IllegalArgumentException("'lookbackHours' must be a whole number of hours");
      }
      lookback = root.get("lookbackHours").asInt();
      if (lookback < MIN_LOOKBACK_HOURS || lookback > MAX_LOOKBACK_HOURS) {
        throw new IllegalArgumentException(
            "'lookbackHours' must be between " + MIN_LOOKBACK_HOURS + " and " + MAX_LOOKBACK_HOURS);
      }
    }

    boolean onlyWhenAtRisk = !root.has("onlyWhenAtRisk") || root.get("onlyWhenAtRisk").asBoolean(true);
    boolean useAi = !root.has("useAi") || root.get("useAi").asBoolean(true);
    return new AgentLoopConfig(channels, channel, lookback, onlyWhenAtRisk, useAi);
  }

  public Set<Channel> channels() {
    return channels;
  }

  /** Optional channel override passed to Slack/Teams (their configured default when null). */
  public String channel() {
    return channel;
  }

  public int lookbackHours() {
    return lookbackHours;
  }

  /** Cycle health only: stay silent when nothing is wrong. */
  public boolean onlyWhenAtRisk() {
    return onlyWhenAtRisk;
  }

  /** Whether the agent may add its short written summary on top of the facts. */
  public boolean useAi() {
    return useAi;
  }
}
