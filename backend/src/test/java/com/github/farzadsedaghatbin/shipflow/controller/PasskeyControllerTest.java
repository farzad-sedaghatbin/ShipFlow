package com.github.farzadsedaghatbin.shipflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.AuthResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyCredentialDTO;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsRequest;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import com.github.farzadsedaghatbin.shipflow.service.PasskeyService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link PasskeyController}. Focuses on the self-service
 * list/delete endpoints (ownership resolution via the current
 * {@code SecurityContextHolder} principal) and the login-verify -> JWT wiring,
 * mirroring the manual-construction + manual-SecurityContext style used by
 * {@code McpStreamableHttpControllerTest} elsewhere in this test suite.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasskeyControllerTest {

  @Mock
  private PasskeyService passkeyService;

  @Mock
  private UserService userService;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  private PasskeyController controller;

  private static Authentication authFor(String username) {
    return new UsernamePasswordAuthenticationToken(username, null, List.of());
  }

  private static void setAuth(Authentication auth) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
  }

  @BeforeEach
  void setUp() {
    controller = new PasskeyController(passkeyService, userService, jwtTokenProvider);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listPasskeys_returnsCurrentUsersCredentials() {
    setAuth(authFor("alice"));
    User alice = User.builder().id(1L).username("alice").role(UserRole.MEMBER).isActive(true).build();
    when(userService.findUserByUsername("alice")).thenReturn(alice);

    List<PasskeyCredentialDTO> creds = List.of(PasskeyCredentialDTO.builder().id(10L).deviceName("Touch ID")
        .createdAt(LocalDateTime.now()).transports(List.of("internal")).build());
    when(passkeyService.listCredentials(1L)).thenReturn(creds);

    ResponseEntity<List<PasskeyCredentialDTO>> response = controller.listPasskeys();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).getDeviceName()).isEqualTo("Touch ID");
  }

  @Test
  void deletePasskey_delegatesToServiceScopedToCurrentUser() {
    setAuth(authFor("alice"));
    User alice = User.builder().id(1L).username("alice").role(UserRole.MEMBER).isActive(true).build();
    when(userService.findUserByUsername("alice")).thenReturn(alice);

    ResponseEntity<Void> response = controller.deletePasskey(10L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(passkeyService).deleteCredential(1L, 10L);
  }

  @Test
  void deletePasskey_ownershipEnforcedByService_propagatesAccessDenied() {
    setAuth(authFor("alice"));
    User alice = User.builder().id(1L).username("alice").role(UserRole.MEMBER).isActive(true).build();
    when(userService.findUserByUsername("alice")).thenReturn(alice);
    org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("not yours"))
        .when(passkeyService).deleteCredential(1L, 99L);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.deletePasskey(99L))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void loginVerify_mintsJwtForVerifiedUsername() {
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().username("alice").credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON("BB").signature("CC").build();
    when(passkeyService.finishLogin(request)).thenReturn("alice");
    when(jwtTokenProvider.generateTokenFromUsername("alice")).thenReturn("jwt-token");
    User alice = User.builder().id(1L).username("alice").role(UserRole.MEMBER).isActive(true).build();
    when(userService.findUserByUsername("alice")).thenReturn(alice);

    ResponseEntity<AuthResponse> response = controller.loginVerify(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
    assertThat(response.getBody().getUsername()).isEqualTo("alice");
    assertThat(response.getBody().getUserId()).isEqualTo(1L);
  }

  @Test
  void loginOptions_delegatesToService() {
    PasskeyLoginOptionsRequest request = PasskeyLoginOptionsRequest.builder().username("alice").build();
    PasskeyLoginOptionsResponse serviceResponse =
        PasskeyLoginOptionsResponse.builder().challenge("chal").rpId("localhost").allowCredentials(List.of()).build();
    when(passkeyService.beginLogin("alice")).thenReturn(serviceResponse);

    ResponseEntity<PasskeyLoginOptionsResponse> response = controller.loginOptions(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getChallenge()).isEqualTo("chal");
  }
}
