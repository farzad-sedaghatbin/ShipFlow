package com.github.farzadsedaghatbin.shipflow.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Plain Mockito service-layer test for {@link PresenceController} — no {@code @WebMvcTest}
 * convention exists yet for the closest analog, {@code NotificationSseController} (untested), so
 * this follows the same direct-invocation style used across this codebase's other
 * {@code @Mock}/{@code @InjectMocks} service tests (e.g. {@code PitchServiceTest}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceController")
class PresenceControllerTest {

  @Mock private PresenceService presenceService;
  @Mock private UserService userService;
  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;

  @InjectMocks private PresenceController presenceController;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("alice");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("heartbeat resolves the current user and delegates to PresenceService, returning 204")
  void heartbeat_DelegatesToPresenceService_Returns204() {
    UserDTO user = UserDTO.builder().id(7L).username("alice").personName("Alice A.").build();
    when(userService.findByUsername("alice")).thenReturn(user);

    ResponseEntity<Void> response =
        presenceController.heartbeat(PresenceEntityType.PITCH, 1L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(presenceService).heartbeat(PresenceEntityType.PITCH, 1L, 7L, "Alice A.");
  }

  @Test
  @DisplayName("heartbeat falls back to username when the user has no personName")
  void heartbeat_FallsBackToUsername_WhenNoPersonName() {
    UserDTO user = UserDTO.builder().id(7L).username("alice").personName(null).build();
    when(userService.findByUsername("alice")).thenReturn(user);

    presenceController.heartbeat(PresenceEntityType.WIKI_PAGE, 5L);

    verify(presenceService).heartbeat(PresenceEntityType.WIKI_PAGE, 5L, 7L, "alice");
  }

  @Test
  @DisplayName("leave resolves the current user and delegates to PresenceService, returning 204")
  void leave_DelegatesToPresenceService_Returns204() {
    UserDTO user = UserDTO.builder().id(7L).username("alice").personName("Alice A.").build();
    when(userService.findByUsername("alice")).thenReturn(user);

    ResponseEntity<Void> response =
        presenceController.leave(PresenceEntityType.RETROSPECTIVE, 3L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(presenceService).leave(PresenceEntityType.RETROSPECTIVE, 3L, 7L);
  }
}
