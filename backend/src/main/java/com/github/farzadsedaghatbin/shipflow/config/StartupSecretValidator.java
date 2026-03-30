package com.github.farzadsedaghatbin.shipflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validates that critical secrets have been changed from their insecure
 * defaults before the application accepts traffic in the {@code prod} profile.
 *
 * <p>Fails fast with an {@link IllegalStateException} if any default secret
 * is detected so that operators cannot accidentally deploy ShipFlow in
 * production with well-known, publicly documented credentials.
 *
 * <p>Checked secrets:
 * <ul>
 *   <li>JWT signing secret — must not equal the default base64 dev key</li>
 *   <li>Redis password — must not equal {@code "changeme"}</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("prod")
public class StartupSecretValidator implements ApplicationRunner {

  /**
   * Default JWT secret shipped in {@code application.properties} for local
   * development. This value is publicly visible in the repository and MUST
   * NOT be used in production.
   */
  private static final String DEFAULT_JWT_SECRET =
      "c2hhcGV1cC10cmFja2VyLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1zaWduaW5nLTI1Ni1iaXRz";

  /** Default Redis password configured for docker-compose local development. */
  private static final String DEFAULT_REDIS_PASSWORD = "changeme";

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Override
  public void run(ApplicationArguments args) {
    log.info("Running production secret validation...");

    boolean failed = false;

    if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
      log.error(
          "SECURITY VIOLATION: app.jwt.secret is set to the default development value. "
              + "Generate a strong random secret and set the APP_JWT_SECRET environment variable "
              + "before deploying to production.");
      failed = true;
    }

    if (DEFAULT_REDIS_PASSWORD.equals(redisPassword)) {
      log.error(
          "SECURITY VIOLATION: spring.data.redis.password is 'changeme'. "
              + "Set a strong password via the SPRING_DATA_REDIS_PASSWORD environment variable "
              + "before deploying to production.");
      failed = true;
    }

    if (failed) {
      throw new IllegalStateException(
          "ShipFlow refused to start in production because one or more secrets are set to their "
              + "default insecure development values. Check the error messages above for details.");
    }

    log.info("Production secret validation passed.");
  }
}
