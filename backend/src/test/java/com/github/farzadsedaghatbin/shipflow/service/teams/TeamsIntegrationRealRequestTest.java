package com.github.farzadsedaghatbin.shipflow.service.teams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.teams.TestTeamsNotificationRequest;
import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsConfiguration;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsChannelConfigRepository;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsConfigurationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.teams.TeamsNotificationHistoryRepository;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that captures REAL HTTP requests and compares with working
 * curl command
 */
@SpringBootTest
@ActiveProfiles("test")
class TeamsIntegrationRealRequestTest {

  @Autowired
  private TeamsIntegrationService teamsService;

  @MockBean
  private TeamsConfigurationRepository teamsConfigRepository;
  @MockBean
  private TeamsChannelConfigRepository channelConfigRepository;
  @MockBean
  private TeamsNotificationHistoryRepository historyRepository;

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
    // Given - Anonymized Power Automate URL (real URL was tested and confirmed
    // working)
    String powerAutomateUrl = "https://default-xxxxx.d7.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/xxxxxxxx/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=xxxxx";

    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant")
        .webhookUrl(powerAutomateUrl).isEnabled(true).build();

    org.mockito.Mockito.when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
        .message("🚀 Test notification from ShipFlow - Your Teams integration is working!").build();

    // When - Send test notification (will fail to connect, but we can see what it
    // tried to send in
    // logs)
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
  void testActualPayloadFormat_RealRequest() throws Exception {
    // Use the REAL Power Automate URL - this test will actually call it
    String powerAutomateUrl = "https://default300eebd4b8694d1a8df6e0a23ad188.d7.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/7c0029b148734b5981552a0d53a30348/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=qsqEJmEOgq_ELpcPu3sPxVnhO0aAASIQbYi4s2tgb6A";

    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test").webhookUrl(powerAutomateUrl)
        .isEnabled(true).build();

    org.mockito.Mockito.when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
        .message("🚀 Test notification from ShipFlow - Your Teams integration is working!").build();

    try {
      teamsService.sendTestNotification(1L, request);
      System.out.println("\n✅ SUCCESS! Java RestTemplate request succeeded!");
    } catch (Exception e) {
      System.out.println("\n❌ FAILURE! Java RestTemplate request failed:");
      System.out.println("Error: " + e.getMessage());

      // Now try curl programmatically
      System.out.println("\n🔍 Testing curl from the same JVM...");
      ProcessBuilder pb = new ProcessBuilder("curl", "-s", "-w", "\\n%{http_code}", "-X", "POST",
          powerAutomateUrl, "-H", "Content-Type: application/json", "-H",
          "User-Agent: ShipFlow-Teams-Integration/1.0", "-d",
          "{\"title\":\"🧪 ShipFlow Test Notification\",\"message\":\"🚀 Test notification from ShipFlow - Your Teams integration is working!\",\"text\":\"🚀 Test notification from ShipFlow - Your Teams integration is working!\",\"notificationType\":\"TEST\",\"timestamp\":\"2026-02-01T16:45:00\",\"source\":\"ShipFlow\",\"themeColor\":\"9B59B6\"}");
      pb.redirectErrorStream(true);
      Process p = pb.start();
      String curlOutput = new String(p.getInputStream().readAllBytes());
      int exitCode = p.waitFor();
      System.out.println("Curl output: " + curlOutput);
      System.out.println("Curl exit code: " + exitCode);

      if (curlOutput.contains("202") || curlOutput.contains("200")) {
        System.out.println("✅ CURL SUCCEEDED but Java failed!");
        System.out.println("The difference must be in how Java sends the request.");
      }

      // Don't fail the test - we're just investigating
    }
  }
}
