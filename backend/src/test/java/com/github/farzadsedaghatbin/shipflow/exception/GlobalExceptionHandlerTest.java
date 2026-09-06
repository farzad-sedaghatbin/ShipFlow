package com.github.farzadsedaghatbin.shipflow.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link GlobalExceptionHandler}'s {@link OptimisticLockConflictException} 409
 * mapping (v1.13.0 S64).
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    MessageSource messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(any(String.class), any(), any(String.class), any()))
        .thenAnswer(invocation -> invocation.getArgument(2));
    ReflectionTestUtils.setField(handler, "messageSource", messageSource);
  }

  @Test
  void handleOptimisticLockConflictException_returns409WithExpectedShape() {
    PitchDTO currentState = PitchDTO.builder().id(42L).title("Current Title").version(7L).build();
    OptimisticLockConflictException ex =
        new OptimisticLockConflictException("PITCH", 42L, 7L, currentState);

    ResponseEntity<Map<String, Object>> response =
        handler.handleOptimisticLockConflictException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("entityType")).isEqualTo("PITCH");
    assertThat(body.get("entityId")).isEqualTo(42L);
    assertThat(body.get("currentVersion")).isEqualTo(7L);
    assertThat(body.get("current")).isEqualTo(currentState);
    assertThat(body.get("status")).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(body).containsKeys("timestamp", "message", "messageKey");
  }

  @Test
  void handleOptimisticLockConflictException_withNullCurrentVersion_stillMapsCorrectly() {
    OptimisticLockConflictException ex =
        new OptimisticLockConflictException("WIKI_PAGE", 5L, null, null);

    ResponseEntity<Map<String, Object>> response =
        handler.handleOptimisticLockConflictException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("entityType")).isEqualTo("WIKI_PAGE");
    assertThat(body.get("entityId")).isEqualTo(5L);
    assertThat(body.get("currentVersion")).isNull();
    assertThat(body.get("current")).isNull();
  }
}
