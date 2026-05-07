package com.github.farzadsedaghatbin.shipflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a feature is not enabled in organization settings.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String message) {
        super(message);
    }

    public FeatureDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
