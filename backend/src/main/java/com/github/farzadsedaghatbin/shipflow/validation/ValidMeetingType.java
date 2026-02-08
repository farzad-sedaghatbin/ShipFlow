package com.github.farzadsedaghatbin.shipflow.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a meeting type is valid according to the organization settings.
 * This annotation ensures that only configured meeting types (including custom types)
 * are accepted, preventing typos and invalid values.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMeetingTypeValidator.class)
@Documented
public @interface ValidMeetingType {
  String message() default "Invalid meeting type. Must match a configured meeting type.";
  
  Class<?>[] groups() default {};
  
  Class<? extends Payload>[] payload() default {};
  
  /**
   * Whether to allow null values. Default is false.
   */
  boolean allowNull() default false;
}
