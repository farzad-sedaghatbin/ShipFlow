package com.github.farzadsedaghatbin.shipflow.service.teams;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.teams.CreateTeamsConfigurationRequest;
import com.github.farzadsedaghatbin.shipflow.dto.teams.TestTeamsNotificationRequest;
import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsConfiguration;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsChannelConfigRepository;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsConfigurationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsNotificationHistoryRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that captures REAL HTTP requests and compares with working curl command
 */
@SpringBootTest
@ActiveProfiles("test")
class TeamsIntegrationRealRequestTest {

  @Autowired private TeamsIntegrationService teamsService;

  @MockBean private TeamsConfigurationRepository teamsConfigRepository;
  @MockBean private TeamsChannelConfigRepository channelConfigRepository;
  @MockBean private TeamsNotificationHistoryRepository historyRepository;

  private MockWebServer mockWebServer;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void testPowerAutomateRequest_CompareToCurl() throws Exception {
    // Given - Anonymized Power Automate URL (real URL was tested and confirmed)
    String powerAutomateUrl =
        "https://default-xxxxx.d7.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/xxxxxxxx/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=xxxxx";

    TeamsConfiguration config =
        TeamsConfiguration.builder()
            .id(1L)
            .tenantName("Test Tenant")
            .webhookUrl(powerAutomateUrl)
            .isEnabled(true)
            .build();

    org.mockito.Mockito.when(teamsConfigRepository.findById(1L))
        .thenReturn(Optional.of(config));

    TestTeamsNotificationRequest request =
        TestTeamsNotificationRequest.builder()
            .message("🚀 Test notification from ShipFlow - Your Teams integration is working!")
            .build();

    // When - Send test notification (will fail to connect, but we can see what it tried to send in logs)
    try {
      teamsService.sendTestNotification(1L, request);
    } catch (Exception e) {
      // Expected to fail - we just want to see what payload it would have sent
      System.out.println("\n========== EXPECTED FAILURE (cannot connect) ==========");
      System.out.println("Error: " + e.getMessage());
    }

    System.out.println("\n========== WHAT THE APP WOULD SEND ==========");
    System.out.println("The application tried to send to: " + powerAutomateUrl);
    System.out.println("Check the debug logs above for the actual payload structure");

    System.out.println("\n========== EXPECTED REQUEST FROM CURL ==========");
    System.out.println("URL: [your Power Automate URL]");
    System.out.println("Method: POST");
    System.out.println("\nExpected Headers:");
    System.out.println("  Content-Type: application/json");
    System.out.println("  User-Agent: ShipFlow-Teams-Integration/1.0");
    
    System.out.println("\nExpected Body (from curl):");
    String expectedCurlBody = """
        {
          "title": "🧪 ShipFlow Test Notification",
          "message": "🚀 Test notification from ShipFlow - Your Teams integration is working!",
          "text": "🚀 Test notification from ShipFlow - Your Teams integration is working!",
          "notificationType": "TEST",
          "timestamp": "2026-02-01T16:45:00",
          "source": "ShipFlow",
          "themeColor": "9B59B6"
        }
        """;
    System.out.println(expectedCurlBody);

    System.out.println("\n========== KEY FINDING ==========");
    System.out.println("The application is sending an Adaptive Card format,");
    System.out.println("but your successful curl uses a simple JSON format.");
    System.out.println("\nTo match the curl, the app needs to send:");
    System.out.println("  - Simple JSON object (not Adaptive Card)");
    System.out.println("  - Fields: title, message, text, notificationType, timestamp, source, themeColor");
  }

  @Test  
  void testActualPayloadFormat() throws Exception {
    // This test shows what the app ACTUALLY sends
    String powerAutomateUrl = mockWebServer.url("/test").toString();
    
    TeamsConfiguration config =
        TeamsConfiguration.builder()
            .id(1L)
            .tenantName("Test")
            .webhookUrl(powerAutomateUrl)
            .build();

    org.mockito.Mockito.when(teamsConfigRepository.findById(1L))
        .thenReturn(Optional.of(config));

    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    teamsService.sendTestNotification(
        1L, 
        TestTeamsNotificationRequest.builder()
            .message("🚀 Test notification from ShipFlow - Your Teams integration is working!")
            .build()
    );

    RecordedRequest request = mockWebServer.takeRequest();
    String body = request.getBody().readUtf8();
    Map<String, Object> payload = objectMapper.readValue(body, Map.class);

    System.out.println("\n========== WHAT THE APP ACTUALLY SENDS ==========");
    System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
    
    // Verify it's Adaptive Card format
    assertThat(payload).containsKey("type");
    assertThat(payload).containsKey("version");
    assertThat(payload).containsKey("body");
    assertThat(payload.get("type")).isEqualTo("AdaptiveCard");
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> bodyElements = (List<Map<String, Object>>) payload.get("body");
    assertThat(bodyElements).isNotEmpty();
    
    // First element should be TextBlock with title
    Map<String, Object> firstBlock = bodyElements.get(0);
    assertThat(firstBlock.get("type")).isEqualTo("TextBlock");
    assertThat(firstBlock.get("text")).asString().contains("ShipFlow Test Notification");
  }
}
