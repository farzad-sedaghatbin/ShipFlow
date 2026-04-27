package com.github.farzadsedaghatbin.shipflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret:defaultSecretKeyForDevelopmentOnlyPleaseChangeInProduction12345}")
  private String jwtSecret;

  @Value("${app.jwt.expiration-ms:86400000}")
  private long jwtExpirationMs;

  // Cache signing key and parser to avoid ServiceLoader lookup on every request.
  // JJWT 0.12.x uses java.util.ServiceLoader to find its JSON provider, which can
  // fail inside a Spring Boot fat JAR classloader. Explicitly providing
  // JacksonDeserializer/JacksonSerializer bypasses ServiceLoader entirely.
  private SecretKey signingKey;
  private JwtParser jwtParser;

  @PostConstruct
  private void init() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
    jwtParser = Jwts.parser()
        .json(new JacksonDeserializer<>(Map.of()))
        .verifyWith(signingKey)
        .build();
  }

  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateTokenFromUsername(userPrincipal.getUsername());
  }

  public String generateTokenFromUsername(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    return Jwts.builder()
        .json(new JacksonSerializer<>())
        .subject(username)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(signingKey)
        .compact();
  }

  public String getUsernameFromToken(String token) {
    return jwtParser.parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean validateToken(String authToken) {
    try {
      jwtParser.parseSignedClaims(authToken);
      return true;
    } catch (MalformedJwtException e) {
      // Invalid JWT token
    } catch (ExpiredJwtException e) {
      // Expired JWT token
    } catch (UnsupportedJwtException e) {
      // Unsupported JWT token
    } catch (IllegalArgumentException e) {
      // JWT claims string is empty
    }
    return false;
  }
}
