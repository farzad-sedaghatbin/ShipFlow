package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who the ShipFlow Agent is, and how a comment addresses it.
 *
 * <p>The agent is a real {@link User} row (so it can author comments like anyone else) but it is
 * created <em>inactive</em> with a random, never-disclosed password. That has two deliberate
 * consequences: it can never sign in ({@code CustomUserDetailsService} rejects inactive accounts),
 * and it does not count against the Community Edition seat cap, which counts active users only.
 * The row is created lazily on first use rather than by an initializer so existing databases and
 * test suites that count users are unaffected.
 */
@Service
@Slf4j
public class AgentIdentityService {

  public static final String AGENT_USERNAME = "shipflow-agent";
  public static final String AGENT_DISPLAY_NAME = "ShipFlow";

  /** {@code @ShipFlow}, case-insensitive, not glued to a preceding word and not a longer name. */
  private static final Pattern AGENT_MENTION =
      Pattern.compile(
          "(?<![\\p{L}\\p{N}._@])@shipflow(?![\\p{L}\\p{N}_]|\\.[\\p{L}\\p{N}])",
          Pattern.CASE_INSENSITIVE);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final boolean enabled;

  public AgentIdentityService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.agent.enabled:true}") boolean enabled) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** True when {@code content} addresses the agent with {@code @ShipFlow}. */
  public boolean mentionsAgent(String content) {
    return enabled && content != null && AGENT_MENTION.matcher(content).find();
  }

  /** The comment text with the agent handle removed, i.e. the actual question. */
  public String stripMention(String content) {
    if (content == null) {
      return "";
    }
    return AGENT_MENTION.matcher(content).replaceAll("").replaceAll("\\s+", " ").trim();
  }

  public boolean isAgent(User user) {
    return user != null && AGENT_USERNAME.equals(user.getUsername());
  }

  @Transactional(readOnly = true)
  public Optional<User> findAgentUser() {
    return userRepository.findByUsername(AGENT_USERNAME);
  }

  /** Returns the agent user, creating it (inactive, unusable password) on first call. */
  @Transactional
  public User ensureAgentUser() {
    return userRepository
        .findByUsername(AGENT_USERNAME)
        .orElseGet(
            () -> {
              User agent =
                  User.builder()
                      .username(AGENT_USERNAME)
                      .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                      .role(UserRole.READONLY)
                      .isActive(false)
                      .build();
              agent = userRepository.save(agent);
              log.info("Created ShipFlow Agent system user (id={})", agent.getId());
              return agent;
            });
  }
}
