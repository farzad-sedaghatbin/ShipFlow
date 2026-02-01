package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM Provider implementation for RunPod Serverless GPU.
 *
 * <p>RunPod is ideal for:
 *
 * <ul>
 *   <li>Production deployments without local GPU
 *   <li>Pay-per-use GPU compute
 *   <li>Scaling AI workloads
 * </ul>
 *
 * <p>API Flow:
 *
 * <ol>
 *   <li>POST /run - Submit job, returns {id: "..."}
 *   <li>GET /status/{id} - Poll until status == "COMPLETED"
 *   <li>Extract output from completed response
 * </ol>
 */
@Component
@Slf4j
public class RunPodLLMProvider implements LLMProvider {

  @Override
  public LLMProviderType getProviderType() {
    return LLMProviderType.RUNPOD;
  }

  @Override
  public ChatLanguageModel createModel(LLMProviderConfig config) {
    validateConfig(config);

    log.info(
        "Creating RunPod ChatLanguageModel - URL: {}, Model: {}",
        config.getBaseUrl(),
        config.getModelName());

    Duration pollInterval = config.getExtraParam("pollInterval", Duration.ofSeconds(2));

    return new RunPodChatModel(
        config.getBaseUrl(),
        config.getApiKey(),
        config.getModelName(),
        config.getTimeout(),
        pollInterval,
        config.getMaxTokens(),
        config.getTemperature());
  }

  @Override
  public void validateConfig(LLMProviderConfig config) {
    if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
      throw new IllegalArgumentException("RunPod requires a base URL (app.ai.runpod.base-url)");
    }
    if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
      throw new IllegalArgumentException("RunPod requires an API key (app.ai.runpod.api-key)");
    }
  }

  @Override
  public boolean requiresApiKey() {
    return true;
  }

  @Override
  public boolean requiresBaseUrl() {
    return true;
  }

  /** Internal ChatLanguageModel implementation for RunPod. */
  @Slf4j
  static class RunPodChatModel implements ChatLanguageModel {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final Duration timeout;
    private final Duration pollInterval;
    private final Integer maxTokens;
    private final Double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    RunPodChatModel(
        String baseUrl,
        String apiKey,
        String modelName,
        Duration timeout,
        Duration pollInterval,
        Integer maxTokens,
        Double temperature) {
      this.baseUrl = baseUrl;
      this.apiKey = apiKey;
      this.modelName = modelName;
      this.timeout = timeout != null ? timeout : Duration.ofSeconds(180);
      this.pollInterval = pollInterval != null ? pollInterval : Duration.ofSeconds(2);
      this.maxTokens = maxTokens != null ? maxTokens : 2048;
      this.temperature = temperature != null ? temperature : 0.3;
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
      this.objectMapper = new ObjectMapper();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
      StringBuilder promptBuilder = new StringBuilder();
      for (ChatMessage message : messages) {
        if (message instanceof UserMessage) {
          promptBuilder.append(((UserMessage) message).singleText());
        } else if (message instanceof AiMessage) {
          promptBuilder.append(((AiMessage) message).text());
        }
        promptBuilder.append("\n");
      }
      String prompt = promptBuilder.toString().trim();
      String responseText = generate(prompt);
      return Response.from(AiMessage.from(responseText));
    }

    @Override
    public String generate(String prompt) {
      try {
        log.debug("Submitting prompt to RunPod: {} chars", prompt.length());

        String jobId = submitJob(prompt);
        log.info("RunPod job submitted with ID: {}", jobId);

        String result = pollForCompletion(jobId);
        log.debug("RunPod job completed, response: {} chars", result.length());

        return result;

      } catch (Exception e) {
        log.error("RunPod API call failed: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to get response from RunPod: " + e.getMessage(), e);
      }
    }

    private String submitJob(String prompt) throws Exception {
      String runUrl = baseUrl + "/run";

      Map<String, Object> input = new HashMap<>();
      input.put("prompt", prompt);
      input.put("max_tokens", maxTokens);
      input.put("temperature", temperature);
      if (modelName != null && !modelName.isEmpty()) {
        input.put("model", modelName);
      }

      Map<String, Object> payload = new HashMap<>();
      payload.put("input", input);

      String jsonPayload = objectMapper.writeValueAsString(payload);
      log.debug("RunPod request payload: {}", jsonPayload);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(runUrl))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200 && response.statusCode() != 201) {
        throw new RuntimeException(
            "RunPod /run failed with status " + response.statusCode() + ": " + response.body());
      }

      JsonNode responseJson = objectMapper.readTree(response.body());
      String jobId = responseJson.get("id").asText();

      if (jobId == null || jobId.isEmpty()) {
        throw new RuntimeException("RunPod /run did not return a job ID: " + response.body());
      }

      return jobId;
    }

    private String pollForCompletion(String jobId) throws Exception {
      String statusUrl = baseUrl + "/status/" + jobId;
      long startTime = System.currentTimeMillis();
      long timeoutMillis = timeout.toMillis();

      while (true) {
        if (System.currentTimeMillis() - startTime > timeoutMillis) {
          throw new RuntimeException(
              "RunPod job timed out after " + timeout.getSeconds() + " seconds");
        }

        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(statusUrl))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
          throw new RuntimeException(
              "RunPod /status failed with status "
                  + response.statusCode()
                  + ": "
                  + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String status = responseJson.get("status").asText();

        log.debug("RunPod job {} status: {}", jobId, status);

        switch (status) {
          case "COMPLETED":
            return extractTextFromResponse(responseJson);

          case "FAILED":
            String error =
                responseJson.has("error") ? responseJson.get("error").asText() : "Unknown error";
            throw new RuntimeException("RunPod job failed: " + error);

          case "CANCELLED":
            throw new RuntimeException("RunPod job was cancelled");

          case "IN_QUEUE":
          case "IN_PROGRESS":
            Thread.sleep(pollInterval.toMillis());
            break;

          default:
            log.warn("Unknown RunPod status: {}, continuing to poll", status);
            Thread.sleep(pollInterval.toMillis());
        }
      }
    }

    private String extractTextFromResponse(JsonNode responseJson) {
      JsonNode output = responseJson.get("output");

      if (output == null) {
        throw new RuntimeException("RunPod response missing 'output' field: " + responseJson);
      }

      // Try output[0].choices[0].text (OpenAI-compatible format)
      if (output.isArray() && output.size() > 0) {
        JsonNode firstOutput = output.get(0);
        if (firstOutput.has("choices")) {
          JsonNode choices = firstOutput.get("choices");
          if (choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            if (firstChoice.has("text")) {
              return firstChoice.get("text").asText();
            }
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
              return firstChoice.get("message").get("content").asText();
            }
          }
        }
        if (firstOutput.has("text")) {
          return firstOutput.get("text").asText();
        }
        if (firstOutput.isTextual()) {
          return firstOutput.asText();
        }
      }

      // Try output.choices[0].text (non-array format)
      if (output.has("choices")) {
        JsonNode choices = output.get("choices");
        if (choices.isArray() && choices.size() > 0) {
          JsonNode firstChoice = choices.get(0);
          if (firstChoice.has("text")) {
            return firstChoice.get("text").asText();
          }
          if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
            return firstChoice.get("message").get("content").asText();
          }
        }
      }

      if (output.has("text")) {
        return output.get("text").asText();
      }

      if (output.isTextual()) {
        return output.asText();
      }

      log.warn("Could not parse RunPod output format, returning raw: {}", output);
      return output.toString();
    }
  }
}
