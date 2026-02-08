package com.github.farzadsedaghatbin.shipflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException, ServletException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpStatus.FORBIDDEN.value());

    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", LocalDateTime.now().toString());
    error.put("status", HttpStatus.FORBIDDEN.value());
    error.put("error", "Forbidden");
    error.put("message", "You don't have permission to access this resource");
    error.put("path", request.getServletPath());

    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(response.getOutputStream(), error);
  }
}
