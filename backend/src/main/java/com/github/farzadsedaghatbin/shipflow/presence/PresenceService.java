package com.github.farzadsedaghatbin.shipflow.presence;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tracks "who's currently viewing this" presence for pitches, retrospectives, and wiki pages
 * (v1.13.0 S64), backed by Redis so it works correctly across every backend pod.
 *
 * <p>Storage scheme per {@code (entityType, entityId)}:
 *
 * <ul>
 *   <li>ZSET {@code presence:<type>:<id>:z} — member = userId, score = last-heartbeat epoch
 *       millis. Used to answer "who's active right now" without a separate expiry per member.
 *   <li>HASH {@code presence:<type>:<id>:names} — field = userId, value = display name. Kept
 *       alongside the ZSET so a viewer's name survives without a second round-trip lookup against
 *       the user table.
 * </ul>
 *
 * <p>A viewer is considered active if its ZSET score is within {@link #PRESENCE_TTL_MS} of now.
 * Entries older than {@link #PRESENCE_PRUNE_MS} are opportunistically removed on every read via
 * {@code ZREMRANGEBYSCORE} so an abandoned entity's presence set never grows unbounded. Both Redis
 * keys additionally carry a generous {@code EXPIRE} as a backstop, in case an entity is heartbeaten
 * and then never read/pruned again.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

  /** A heartbeat older than this is considered stale and excluded from {@link #getViewers}. */
  static final long PRESENCE_TTL_MS = 45_000L;

  /** Entries older than this are opportunistically pruned from Redis on every read. */
  static final long PRESENCE_PRUNE_MS = 5 * 60_000L;

  /** Backstop TTL on the Redis keys themselves, in case an entity is never read/pruned again. */
  private static final Duration KEY_EXPIRY = Duration.ofMinutes(10);

  private static final String EVENT_NAME = "presence-update";

  private final StringRedisTemplate redisTemplate;
  private final NotificationSseManager notificationSseManager;

  public void heartbeat(PresenceEntityType type, Long entityId, Long userId, String displayName) {
    String zsetKey = zsetKey(type, entityId);
    String namesKey = namesKey(type, entityId);
    String member = userId.toString();

    redisTemplate.opsForZSet().add(zsetKey, member, System.currentTimeMillis());
    redisTemplate.opsForHash().put(namesKey, member, displayName != null ? displayName : "");
    redisTemplate.expire(zsetKey, KEY_EXPIRY);
    redisTemplate.expire(namesKey, KEY_EXPIRY);

    broadcastPresence(type, entityId);
  }

  public void leave(PresenceEntityType type, Long entityId, Long userId) {
    String zsetKey = zsetKey(type, entityId);
    String namesKey = namesKey(type, entityId);
    String member = userId.toString();

    redisTemplate.opsForZSet().remove(zsetKey, member);
    redisTemplate.opsForHash().delete(namesKey, member);

    broadcastPresence(type, entityId);
  }

  public List<PresenceViewerDTO> getViewers(PresenceEntityType type, Long entityId) {
    String zsetKey = zsetKey(type, entityId);
    String namesKey = namesKey(type, entityId);
    long now = System.currentTimeMillis();

    // Opportunistically drop anything long-abandoned so the set never grows unbounded.
    redisTemplate.opsForZSet().removeRangeByScore(zsetKey, 0, now - PRESENCE_PRUNE_MS);

    Set<String> activeMembers =
        redisTemplate.opsForZSet().rangeByScore(zsetKey, now - PRESENCE_TTL_MS, now);
    if (activeMembers == null || activeMembers.isEmpty()) {
      return List.of();
    }

    List<PresenceViewerDTO> viewers = new ArrayList<>();
    for (String member : activeMembers) {
      Object nameValue = redisTemplate.opsForHash().get(namesKey, member);
      if (nameValue == null) {
        // Name entry missing/expired separately from the zset entry — skip rather than throw.
        continue;
      }
      try {
        viewers.add(new PresenceViewerDTO(Long.valueOf(member), nameValue.toString()));
      } catch (NumberFormatException e) {
        log.warn("Skipping malformed presence member '{}' for {}:{}", member, type, entityId);
      }
    }
    return viewers;
  }

  private void broadcastPresence(PresenceEntityType type, Long entityId) {
    List<PresenceViewerDTO> viewers = getViewers(type, entityId);
    Map<String, Object> payload =
        Map.of("entityType", type.name(), "entityId", entityId, "viewers", viewers);
    for (PresenceViewerDTO viewer : viewers) {
      notificationSseManager.sendEventToUser(viewer.getUserId(), EVENT_NAME, payload);
    }
  }

  private String zsetKey(PresenceEntityType type, Long entityId) {
    return "presence:" + type + ":" + entityId + ":z";
  }

  private String namesKey(PresenceEntityType type, Long entityId) {
    return "presence:" + type + ":" + entityId + ":names";
  }
}
