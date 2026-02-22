package com.github.farzadsedaghatbin.shipflow.service.vcs;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.service.github.GitHubIntegrationService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the {@link VCSProvider} interface.
 *
 * <p>Verifies that known implementations correctly implement the interface
 * and that the interface surface is stable.</p>
 *
 * @since 0.6.0
 */
class VCSProviderContractTest {

  @Test
  void gitHubIntegrationService_implementsVCSProvider() {
    assertThat(VCSProvider.class).isAssignableFrom(GitHubIntegrationService.class);
  }

  @Test
  void interfaceDeclares_expectedMethods() {
    Set<String> methods = Arrays.stream(VCSProvider.class.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());

    assertThat(methods).containsExactlyInAnyOrder(
        "getProviderName",
        "processCommit",
        "processPullRequest",
        "getTaskLinks",
        "getPitchLinks"
    );
  }

  @Test
  void processCommit_hasCorrectParameterCount() throws NoSuchMethodException {
    Method method = VCSProvider.class.getMethod("processCommit",
        String.class, String.class, String.class,
        String.class, String.class, String.class,
        java.time.LocalDateTime.class, String.class, String.class);
    assertThat(method.getParameterCount()).isEqualTo(9);
    assertThat(method.getReturnType()).isEqualTo(void.class);
  }

  @Test
  void processPullRequest_hasCorrectParameterCount() throws NoSuchMethodException {
    Method method = VCSProvider.class.getMethod("processPullRequest",
        String.class, Integer.class,
        String.class, String.class, String.class,
        String.class, String.class, String.class, String.class,
        java.time.LocalDateTime.class, java.time.LocalDateTime.class,
        java.time.LocalDateTime.class, String.class);
    assertThat(method.getParameterCount()).isEqualTo(13);
    assertThat(method.getReturnType()).isEqualTo(void.class);
  }

  @Test
  void getTaskLinks_returnsList() throws NoSuchMethodException {
    Method method = VCSProvider.class.getMethod("getTaskLinks", Long.class);
    assertThat(method.getReturnType()).isEqualTo(java.util.List.class);
  }

  @Test
  void getPitchLinks_returnsList() throws NoSuchMethodException {
    Method method = VCSProvider.class.getMethod("getPitchLinks", Long.class);
    assertThat(method.getReturnType()).isEqualTo(java.util.List.class);
  }
}
