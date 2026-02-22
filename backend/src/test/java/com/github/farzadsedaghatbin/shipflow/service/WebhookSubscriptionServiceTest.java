package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateWebhookRequest;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.WebhookSubscriptionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WebhookSubscription;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WebhookSubscriptionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookSubscriptionServiceTest {

  @Mock
  private WebhookSubscriptionRepository repository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private WebhookSubscriptionService service;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder().id(1L).username("testuser").build();
  }

  @Test
  void create_shouldReturnDtoWithSecret() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(repository.save(any(WebhookSubscription.class))).thenAnswer(inv -> {
      WebhookSubscription sub = inv.getArgument(0);
      sub.setId(10L);
      return sub;
    });

    CreateWebhookRequest request = CreateWebhookRequest.builder()
        .name("Slack Webhook")
        .targetUrl("https://hooks.slack.com/test")
        .eventTypes("task.status_changed,task.completed")
        .build();

    WebhookSubscriptionDTO dto = service.create(1L, request);

    assertThat(dto.getName()).isEqualTo("Slack Webhook");
    assertThat(dto.getTargetUrl()).isEqualTo("https://hooks.slack.com/test");
    assertThat(dto.getSecret()).isNotNull().startsWith("whsec_");
    assertThat(dto.getIsActive()).isTrue();
    assertThat(dto.getEventTypes()).contains("task.status_changed");
  }

  @Test
  void delete_shouldRejectDifferentUser() {
    WebhookSubscription sub = WebhookSubscription.builder()
        .id(10L).name("test").user(testUser).build();
    when(repository.findById(10L)).thenReturn(Optional.of(sub));

    assertThatThrownBy(() -> service.delete(10L, 999L))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void toggleActive_shouldFlipState() {
    WebhookSubscription sub = WebhookSubscription.builder()
        .id(10L).name("test").user(testUser).isActive(true).failureCount(3).build();
    when(repository.findById(10L)).thenReturn(Optional.of(sub));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    WebhookSubscriptionDTO dto = service.toggleActive(10L, 1L);

    assertThat(dto.getIsActive()).isFalse();
    assertThat(dto.getFailureCount()).isEqualTo(0); // reset on toggle
  }
}
