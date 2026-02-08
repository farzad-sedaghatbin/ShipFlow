package com.github.farzadsedaghatbin.shipflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
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

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateTokenFromUsername(userPrincipal.getUsername());
  }

  public String generateTokenFromUsername(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    return Jwts.builder().subject(username).issuedAt(now).expiration(expiryDate).signWith(getSigningKey())
        .compact();
  }

  public String getUsernameFromToken(String token) {
    Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return claims.getSubject();
  }

  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
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
