package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

/**
 * Cleans text that came from people (task titles, names) or from a model before it is posted to a
 * shared channel on the agent's behalf. A title like {@code <!channel> click <https://evil|here>}
 * must not become a channel-wide ping or a disguised link just because the agent quoted it.
 */
public final class OutboundText {

  private OutboundText() {}

  /** Neutralises Slack/Teams control syntax and broadcast mentions; collapses whitespace. */
  public static String clean(String text) {
    if (text == null) {
      return "";
    }
    String t = text
        // broadcast mentions in every common spelling
        .replaceAll("(?i)<!(channel|here|everyone)[^>]*>", "")
        .replaceAll("(?i)(?<![\\p{L}\\p{N}])@(channel|here|everyone)(?![\\p{L}\\p{N}])", "")
        // link / user / channel syntax: <...> becomes plain angle quotes
        .replace('<', '‹')
        .replace('>', '›')
        // control characters
        .replaceAll("[\\p{Cntrl}&&[^\\n]]", " ");
    return t.replaceAll("[ \\t]+", " ").replaceAll(" ?\\n ?", "\n").trim();
  }

  /** {@link #clean} for a single line (titles, names): newlines become spaces, length is capped. */
  public static String line(String text, int max) {
    String t = clean(text).replace('\n', ' ');
    return t.length() <= max ? t : t.substring(0, max - 1) + "…";
  }
}
