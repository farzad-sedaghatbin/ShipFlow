package com.github.farzadsedaghatbin.shipflow.service.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentIdentityService")
class AgentIdentityServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private AgentIdentityService service(boolean enabled) {
    return new AgentIdentityService(userRepository, passwordEncoder, enabled);
  }

  @Test
  @DisplayName("Detects @ShipFlow in the ways people actually type it")
  void detectsMentions() {
    AgentIdentityService s = service(true);
    assertThat(s.mentionsAgent("@ShipFlow what is blocking this?")).isTrue();
    assertThat(s.mentionsAgent("hey @shipflow, summarise")).isTrue();
    assertThat(s.mentionsAgent("thanks @SHIPFLOW.")).isTrue();
    assertThat(s.mentionsAgent("(@ShipFlow)")).isTrue();
    assertThat(s.mentionsAgent("ping\n@ShipFlow\nplease")).isTrue();
  }

  @Test
  @DisplayName("Does not fire on lookalikes: emails, longer names, bare word")
  void ignoresLookalikes() {
    AgentIdentityService s = service(true);
    assertThat(s.mentionsAgent("mail me at bob@shipflow.dev")).isFalse();
    assertThat(s.mentionsAgent("@ShipFlowBot do it")).isFalse();
    assertThat(s.mentionsAgent("@shipflow_admin")).isFalse();
    assertThat(s.mentionsAgent("@shipflow.dev is down")).isFalse();
    assertThat(s.mentionsAgent("ShipFlow is great")).isFalse();
    assertThat(s.mentionsAgent(null)).isFalse();
  }

  @Test
  @DisplayName("Never detects a mention while the agent is disabled")
  void disabledNeverMatches() {
    assertThat(service(false).mentionsAgent("@ShipFlow hi")).isFalse();
  }

  @Test
  @DisplayName("stripMention leaves just the question")
  void stripsMention() {
    AgentIdentityService s = service(true);
    assertThat(s.stripMention("@ShipFlow   what is blocking   this?")).isEqualTo("what is blocking this?");
    assertThat(s.stripMention("why is it late @shipflow?")).isEqualTo("why is it late ?");
    assertThat(s.stripMention(null)).isEmpty();
  }

  @Test
  @DisplayName("ensureAgentUser creates an INACTIVE read-only user with an unusable password")
  void createsInactiveUser() {
    when(userRepository.findByUsername(AgentIdentityService.AGENT_USERNAME)).thenReturn(Optional.empty());
    when(passwordEncoder.encode(any())).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User agent = service(true).ensureAgentUser();

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getUsername()).isEqualTo("shipflow-agent");
    assertThat(saved.getValue().getIsActive()).isFalse(); // cannot log in, not a licensed seat
    assertThat(saved.getValue().getRole()).isEqualTo(UserRole.READONLY);
    assertThat(agent.getPassword()).isEqualTo("hashed");
  }

  @Test
  @DisplayName("ensureAgentUser reuses the existing row")
  void reusesExistingUser() {
    User existing = User.builder().id(9L).username(AgentIdentityService.AGENT_USERNAME).build();
    when(userRepository.findByUsername(AgentIdentityService.AGENT_USERNAME)).thenReturn(Optional.of(existing));

    assertThat(service(true).ensureAgentUser()).isSameAs(existing);
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("isAgent matches only the agent username")
  void isAgent() {
    AgentIdentityService s = service(true);
    assertThat(s.isAgent(User.builder().username("shipflow-agent").build())).isTrue();
    assertThat(s.isAgent(User.builder().username("admin").build())).isFalse();
    assertThat(s.isAgent(null)).isFalse();
  }
}
