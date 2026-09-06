package com.github.farzadsedaghatbin.shipflow.security;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces the per-key project restriction (see {@link ApiKey#getRestrictedToProjectId()}) for
 * every controller under {@code /api/v1/public/**}, including the data export/import endpoints
 * in {@code DataManagementController}.
 *
 * <p>{@link ApiKeyAuthenticationFilter} stores the authenticated {@link ApiKey} as the
 * {@code Authentication}'s {@code details} — the same mechanism {@code McpAuthFilter} already
 * uses, read via {@code auth.getDetails() instanceof ApiKey} in {@code McpToolDispatcher}. This
 * class is the single place that reads it back for public-API authorization, so controllers
 * never need a second, parallel way to find the calling key.
 */
@Component
public class PublicApiAuthorizationService {

  /**
   * Returns the {@link ApiKey} that authenticated the current request, or {@code null} if the
   * request wasn't authenticated via {@link ApiKeyAuthenticationFilter} (e.g. an internal
   * JWT-authenticated call reusing one of these controllers, or no authentication at all).
   */
  public ApiKey currentApiKey() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getDetails() instanceof ApiKey apiKey ? apiKey : null;
  }

  /**
   * Convenience accessor: the project id the current request's key is restricted to, or
   * {@code null} if the key is unrestricted (or there is no API-key-authenticated request at
   * all).
   */
  public Long restrictedProjectIdOrNull() {
    ApiKey apiKey = currentApiKey();
    return apiKey != null ? apiKey.getRestrictedToProjectId() : null;
  }

  /**
   * Throws if {@code apiKey} is restricted to a project other than {@code resourceProjectId}.
   * A {@code null} {@code apiKey} or an unrestricted key (null {@code restrictedToProjectId})
   * always allows — this method is a no-op for the pre-existing, unrestricted-key behavior.
   *
   * <p>A restricted key checked against a resource with no resolvable project id
   * ({@code resourceProjectId == null}) is denied, not allowed — an ownerless resource is never
   * safely "in scope" for a project-restricted key.
   *
   * <p>Denial surfaces as {@link ResourceNotFoundException} (404), not 403. This API is reachable
   * by any holder of a valid key rather than only by an internally-authenticated, role-scoped
   * user, so revealing "this resource exists but you can't see it" via 403 would let a
   * project-restricted key enumerate resource ids across the whole deployment. 404 gives no
   * signal either way. There's no existing internal precedent for "resource outside a caller's
   * permitted projects" to follow instead (the app's own Project-Level Permissions checks don't
   * have an equivalent public/key-authenticated code path), so this is a deliberate new choice,
   * not an existing convention.
   */
  public void requireProjectAccess(ApiKey apiKey, Long resourceProjectId) {
    if (apiKey == null) {
      return;
    }
    Long restrictedToProjectId = apiKey.getRestrictedToProjectId();
    if (restrictedToProjectId == null) {
      return;
    }
    if (resourceProjectId == null || !restrictedToProjectId.equals(resourceProjectId)) {
      throw new ResourceNotFoundException("Resource not found");
    }
  }
}
