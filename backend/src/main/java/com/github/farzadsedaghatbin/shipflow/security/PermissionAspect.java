package com.github.farzadsedaghatbin.shipflow.security;

import com.github.farzadsedaghatbin.shipflow.service.PermissionService;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for enforcing @RequirePermission annotations. Intercepts method
 * calls annotated with @RequirePermission and checks if the current user has
 * the required permission before allowing the method to execute.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {

  private final PermissionService permissionService;

  @Value("${app.security.rbac.enabled:true}")
  private boolean rbacEnabled;

  /** Intercept methods annotated with @RequirePermission and check permissions */
  @Before("@annotation(com.github.farzadsedaghatbin.shipflow.security.RequirePermission)")
  public void checkPermission(JoinPoint joinPoint) throws Throwable {
    // Skip RBAC check if disabled (e.g., in test mode)
    if (!rbacEnabled) {
      log.debug("RBAC check skipped (disabled in configuration)");
      return;
    }

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    RequirePermission annotation = method.getAnnotation(RequirePermission.class);
    if (annotation == null) {
      return;
    }

    log.debug("Checking permission for method: {}.{}", joinPoint.getTarget().getClass().getSimpleName(),
        method.getName());

    // Check if user has the required permission
    if (!permissionService.hasPermission(annotation.resource(), annotation.permission())) {
      log.warn("Permission denied for method: {}.{} - Required: {} on {}",
          joinPoint.getTarget().getClass().getSimpleName(), method.getName(), annotation.permission(),
          annotation.resource());

      throw new AccessDeniedException(annotation.message());
    }

    log.debug("Permission granted for method: {}.{}", joinPoint.getTarget().getClass().getSimpleName(),
        method.getName());
  }
}
