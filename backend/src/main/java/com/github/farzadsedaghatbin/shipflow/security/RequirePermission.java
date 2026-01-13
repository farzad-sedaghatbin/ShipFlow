package com.github.farzadsedaghatbin.shipflow.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;

/**
 * Annotation for declarative permission checking on controller methods.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.CREATE)
 * public ResponseEntity<CycleDTO> createCycle(@RequestBody CreateCycleRequest request) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * The resource type this permission applies to
     */
    ResourceType resource();
    
    /**
     * The permission type required
     */
    PermissionType permission();
    
    /**
     * Optional error message when permission is denied
     */
    String message() default "Access denied: insufficient permissions";
}
