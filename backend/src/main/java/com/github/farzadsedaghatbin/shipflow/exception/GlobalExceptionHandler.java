package com.github.farzadsedaghatbin.shipflow.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Autowired
  private MessageSource messageSource;

  /** Get a localized message for the given key. */
  private String getMessage(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("auth.access.denied"));
    error.put("messageKey", "auth.access.denied");
    error.put("status", HttpStatus.FORBIDDEN.value());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("auth.login.failed"));
    error.put("messageKey", "auth.login.failed");
    error.put("status", HttpStatus.UNAUTHORIZED.value());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("auth.login.failed"));
    error.put("messageKey", "auth.login.failed");
    error.put("status", HttpStatus.UNAUTHORIZED.value());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("error.resource.not.found"));
    error.put("messageKey", "error.resource.not.found");
    error.put("detail", ex.getMessage());
    error.put("status", HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", ex.getMessage());
    error.put("status", HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage());
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());

    String message = getMessage("error.data.integrity");
    String messageKey = "error.data.integrity";
    String rootCause = ex.getMostSpecificCause().getMessage();

    if (rootCause != null) {
      if (rootCause.contains("Unique index or primary key violation") || rootCause.contains("duplicate key")) {
        if (rootCause.contains("PROJECT_KEY") || rootCause.contains("project_key")) {
          message = getMessage("project.key.duplicate");
          messageKey = "project.key.duplicate";
        } else if (rootCause.contains("USERNAME") || rootCause.contains("username")) {
          message = getMessage("user.username.duplicate");
          messageKey = "user.username.duplicate";
        } else if (rootCause.contains("EMAIL") || rootCause.contains("email")) {
          message = getMessage("user.email.duplicate");
          messageKey = "user.email.duplicate";
        } else {
          message = getMessage("error.resource.already.exists");
          messageKey = "error.resource.already.exists";
        }
      } else if (rootCause.contains("foreign key constraint")) {
        message = getMessage("error.resource.in.use");
        messageKey = "error.resource.in.use";
      }
    }

    error.put("message", message);
    error.put("messageKey", messageKey);
    error.put("status", HttpStatus.CONFLICT.value());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", ex.getMessage());
    error.put("status", HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handle client disconnection exceptions (ClientAbortException, IOException:
   * Connection reset by peer, etc.) These are normal network conditions, not
   * server errors, so we log them at DEBUG level instead of ERROR.
   */
  @ExceptionHandler(ClientAbortException.class)
  public void handleClientAbortException(ClientAbortException ex) {
    log.debug("Client disconnected during response: {}", ex.getMessage());
    // Don't return a response - the client has already disconnected
  }

  /**
   * Handle general IO exceptions, particularly connection reset errors. Log at
   * DEBUG level if it's a connection issue, ERROR level otherwise.
   */
  @ExceptionHandler(IOException.class)
  public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
    String message = ex.getMessage();

    // Check if this is a client disconnection issue
    if (message != null && (message.contains("Connection reset by peer") || message.contains("Broken pipe")
        || message.contains("An established connection was aborted"))) {
      log.debug("Client connection issue: {}", message);
      return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }

    // For other IO exceptions, treat as server error
    log.error("IO Exception: {}", ex.getMessage(), ex);
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("error.generic"));
    error.put("messageKey", "error.generic");
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    log.error("Unexpected runtime exception: {}", ex.getMessage(), ex);
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("error.generic"));
    error.put("messageKey", "error.generic");
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    String requestUrl = request.getRequestURI();
    String method = request.getMethod();
    String queryString = request.getQueryString();
    String fullUrl = queryString != null ? requestUrl + "?" + queryString : requestUrl;
    
    log.error("HTTP Method Not Supported - Method: {}, URL: {}, Supported Methods: {}", 
        method, fullUrl, String.join(", ", ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[]{"UNKNOWN"}));
    log.error("Request Headers: {}", request.getHeaderNames());
    
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", String.format("Request method '%s' is not supported for URL '%s'", method, requestUrl));
    error.put("requestedUrl", requestUrl);
    error.put("method", method);
    error.put("supportedMethods", ex.getSupportedMethods());
    error.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    // Check if this is a wrapped client disconnection issue
    if (isClientDisconnectionException(ex)) {
      log.debug("Client disconnection detected in generic exception: {}", ex.getMessage());
      return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }

    log.error("Unexpected exception: {}", ex.getMessage(), ex);
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("message", getMessage("error.generic"));
    error.put("messageKey", "error.generic");
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Check if an exception is related to client disconnection by examining the
   * exception chain and error messages for common client disconnection
   * indicators.
   */
  private boolean isClientDisconnectionException(Exception ex) {
    // Check the exception chain for client disconnection indicators
    Throwable cause = ex;
    while (cause != null) {
      // Check exception type
      if (cause instanceof ClientAbortException || cause instanceof java.net.SocketException
          || (cause instanceof IOException && isConnectionResetMessage(cause.getMessage()))) {
        return true;
      }

      // Check for common client disconnection messages
      String message = cause.getMessage();
      if (message != null && isConnectionResetMessage(message)) {
        return true;
      }

      cause = cause.getCause();
    }
    return false;
  }

  /** Check if a message indicates a client disconnection. */
  private boolean isConnectionResetMessage(String message) {
    if (message == null) {
      return false;
    }

    String lowerMessage = message.toLowerCase();
    return lowerMessage.contains("connection reset by peer") || lowerMessage.contains("broken pipe")
        || lowerMessage.contains("an established connection was aborted")
        || lowerMessage.contains("connection was forcibly closed") || lowerMessage.contains("socket closed")
        || lowerMessage.contains("client abort");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.BAD_REQUEST.value());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((err) -> {
      String fieldName = ((FieldError) err).getField();
      String errorMessage = err.getDefaultMessage();
      fieldErrors.put(fieldName, errorMessage);
    });
    error.put("errors", fieldErrors);

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.BAD_REQUEST.value());

    String message = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));

    error.put("message", message);
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    log.warn("Type mismatch for parameter '{}': received '{}', expected type '{}'", ex.getName(), ex.getValue(),
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.BAD_REQUEST.value());

    String parameterName = ex.getName();
    String receivedValue = String.valueOf(ex.getValue());
    String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

    error.put("message", String.format("Invalid value '%s' for parameter '%s'. Expected a valid %s.", receivedValue,
        parameterName, expectedType));
    error.put("parameter", parameterName);
    error.put("invalidValue", receivedValue);
    error.put("expectedType", expectedType);

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Map<String, Object>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
    log.warn("Missing required header: {}", ex.getHeaderName());

    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("message", String.format("Missing required header: %s", ex.getHeaderName()));
    error.put("header", ex.getHeaderName());

    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
    log.warn("File upload size exceeded: {}", ex.getMessage());

    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
    error.put("message", "File size exceeds maximum allowed limit (10MB)");
    error.put("messageKey", "error.file.size.exceeded");

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
    // Check if client disconnected during upload
    if (isClientDisconnectionException(ex)) {
      log.debug("Client disconnected during file upload: {}", ex.getMessage());
      return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }

    // Check for malformed stream (incomplete upload)
    String message = ex.getMessage();
    if (message != null && (message.contains("Stream ended unexpectedly") 
        || message.contains("MalformedStreamException"))) {
      log.warn("Incomplete file upload detected: {}", message);
      
      Map<String, Object> error = new HashMap<>();
      error.put("timestamp", LocalDateTime.now());
      error.put("status", HttpStatus.BAD_REQUEST.value());
      error.put("message", "File upload was incomplete. Please try again.");
      error.put("messageKey", "error.file.upload.incomplete");
      
      return ResponseEntity.badRequest().body(error);
    }

    log.error("Multipart request parsing failed: {}", message, ex);
    
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now());
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("message", "Failed to process file upload. Please check the file and try again.");
    error.put("messageKey", "error.file.upload.failed");

    return ResponseEntity.badRequest().body(error);
  }
}
