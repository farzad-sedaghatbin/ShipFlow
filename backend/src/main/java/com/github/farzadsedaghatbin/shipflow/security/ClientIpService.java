package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring-managed entry point for resolving the true client IP of a request.
 *
 * <p>Exists so the trusted-proxy list is configured in exactly one place. It
 * was previously duplicated as a {@code @Value} field in each filter that
 * needed it; a third copy (for auth auditing) would have made a silent
 * disagreement between them a matter of time, and two components disagreeing
 * about who a request came from is precisely the class of bug
 * {@link ClientIpResolver} was extracted to fix.
 *
 * @see ClientIpResolver for the resolution order and why it matters
 */
@Slf4j
@Component
public class ClientIpService {

  @Value("${app.rate-limit.trusted-proxies:127.0.0.1,::1}")
  private String trustedProxiesRaw;

  private List<String> trustedProxies;

  @PostConstruct
  void initTrustedProxies() {
    trustedProxies = Arrays.stream(trustedProxiesRaw.split(","))
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .toList();
    log.info("Trusted proxies for client-IP resolution: {}", trustedProxies);
  }

  /** Resolves the originating client address, honouring proxy headers only from trusted peers. */
  public String resolve(HttpServletRequest request) {
    return ClientIpResolver.resolve(request, trustedProxies);
  }
}
