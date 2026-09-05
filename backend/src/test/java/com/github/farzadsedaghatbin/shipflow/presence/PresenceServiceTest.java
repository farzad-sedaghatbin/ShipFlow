package com.github.farzadsedaghatbin.shipflow.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Unit tests for {@link PresenceService} (v1.13.0 S64), mocking {@link StringRedisTemplate}'s
 * {@code opsForZSet()}/{@code opsForHash()} sub-templates the same way {@code
 * AICacheServiceTest} mocks {@code StringRedisTemplate} in this codebase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceService")
class PresenceServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ZSetOperations<String, String> zSetOperations;
  @Mock private HashOperations<String, String, String> hashOperations;
  @Mock private NotificationSseManager notificationSseManager;

  @InjectMocks private PresenceService presenceService;

  private static final String ZSET_KEY = "presence:PITCH:1:z";
  private static final String NAMES_KEY = "presence:PITCH:1:names";

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    // opsForHash() is a generic method (<HK, HV> HashOperations<K, HK, HV>), so `when(...)`
    // can't infer HK/HV from the stubbed call alone; doReturn() sidesteps that by taking Object.
    lenient().doReturn(hashOperations).when(redisTemplate).opsForHash();
  }

  @Test
  @DisplayName("heartbeat writes to both the ZSET and HASH, sets expiry, and broadcasts")
  void heartbeat_WritesToZsetAndHash_AndBroadcasts() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble()))
        .thenReturn(Set.of("42"));
    when(hashOperations.get(NAMES_KEY, "42")).thenReturn("Alice");

    presenceService.heartbeat(PresenceEntityType.PITCH, 1L, 42L, "Alice");

    verify(zSetOperations).add(eq(ZSET_KEY), eq("42"), anyDouble());
    verify(hashOperations).put(NAMES_KEY, "42", "Alice");
    verify(redisTemplate).expire(eq(ZSET_KEY), any());
    verify(redisTemplate).expire(eq(NAMES_KEY), any());
    verify(notificationSseManager).sendEventToUser(eq(42L), eq("presence-update"), any());
  }

  @Test
  @DisplayName("leave removes from both the ZSET and HASH, and broadcasts")
  void leave_RemovesFromZsetAndHash_AndBroadcasts() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble())).thenReturn(Set.of());

    presenceService.leave(PresenceEntityType.PITCH, 1L, 42L);

    verify(zSetOperations).remove(ZSET_KEY, "42");
    verify(hashOperations).delete(NAMES_KEY, "42");
    // No viewers left, so nothing to notify — but the broadcast path itself must not throw.
    verify(notificationSseManager, never()).sendEventToUser(any(), anyString(), any());
  }

  @Test
  @DisplayName("getViewers returns only non-stale entries, correctly paired with display names")
  void getViewers_ReturnsOnlyNonStaleEntries_PairedWithNames() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble()))
        .thenReturn(Set.of("1", "2"));
    when(hashOperations.get(NAMES_KEY, "1")).thenReturn("Alice");
    // Name entry missing (e.g. pruned separately from the zset entry) — must be skipped, not throw.
    when(hashOperations.get(NAMES_KEY, "2")).thenReturn(null);

    List<PresenceViewerDTO> viewers = presenceService.getViewers(PresenceEntityType.PITCH, 1L);

    assertThat(viewers).hasSize(1);
    assertThat(viewers.get(0).getUserId()).isEqualTo(1L);
    assertThat(viewers.get(0).getDisplayName()).isEqualTo("Alice");
    verify(zSetOperations).removeRangeByScore(eq(ZSET_KEY), eq(0.0), anyDouble());
  }

  @Test
  @DisplayName("getViewers on a never-heartbeated entity returns an empty list without throwing")
  void getViewers_NeverHeartbeaten_ReturnsEmptyList() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble())).thenReturn(Set.of());

    List<PresenceViewerDTO> viewers = presenceService.getViewers(PresenceEntityType.PITCH, 1L);

    assertThat(viewers).isEmpty();
  }

  @Test
  @DisplayName("getViewers handles a null rangeByScore result (e.g. Redis miss) without throwing")
  void getViewers_NullRangeByScoreResult_ReturnsEmptyList() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble())).thenReturn(null);

    List<PresenceViewerDTO> viewers = presenceService.getViewers(PresenceEntityType.PITCH, 1L);

    assertThat(viewers).isEmpty();
  }

  @Test
  @DisplayName("broadcastPresence payload carries entityType, entityId, and the viewer list")
  void heartbeat_BroadcastPayloadShape_IsCorrect() {
    when(zSetOperations.rangeByScore(eq(ZSET_KEY), anyDouble(), anyDouble()))
        .thenReturn(Set.of("42"));
    when(hashOperations.get(NAMES_KEY, "42")).thenReturn("Alice");

    presenceService.heartbeat(PresenceEntityType.PITCH, 1L, 42L, "Alice");

    verify(notificationSseManager, times(1))
        .sendEventToUser(
            eq(42L),
            eq("presence-update"),
            eq(
                Map.of(
                    "entityType",
                    "PITCH",
                    "entityId",
                    1L,
                    "viewers",
                    List.of(new PresenceViewerDTO(42L, "Alice")))));
  }
}
