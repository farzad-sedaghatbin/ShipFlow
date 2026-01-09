package com.github.farzadsedaghatbin.shipflow.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatLanguageModel implementation for RunPod Serverless GPU endpoints.
 * Handles the async job submission and polling pattern required by RunPod API.
 * 
 * API Flow:
 * 1. POST /run - Submit job, returns {id: "..."}
 * 2. GET /status/{id} - Poll until status == "COMPLETED"
 * 3. Extract output[0].choices[0].text from completed response
 */
@Slf4j
public class RunPodChatModel implements ChatLanguageModel {

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final Duration timeout;
    private final Duration pollInterval;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private RunPodChatModel(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.modelName = builder.modelName;
        this.timeout = builder.timeout;
        this.pollInterval = builder.pollInterval;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // Convert chat messages to a single prompt string
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
            
            // Step 1: Submit job to /run
            String jobId = submitJob(prompt);
            log.info("RunPod job submitted with ID: {}", jobId);
            
            // Step 2: Poll /status/{id} until completed
            String result = pollForCompletion(jobId);
            log.debug("RunPod job completed, response: {} chars", result.length());
            
            return result;
            
        } catch (Exception e) {
            log.error("RunPod API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get response from RunPod: " + e.getMessage(), e);
        }
    }

    /**
     * Submit a job to RunPod /run endpoint.
     * @return The job ID
     */
    private String submitJob(String prompt) throws Exception {
        String runUrl = baseUrl + "/run";
        
        // Build the request payload matching RunPod's expected format
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        input.put("max_tokens", 2048);
        input.put("temperature", 0.3);
        if (modelName != null && !modelName.isEmpty()) {
            input.put("model", modelName);
        }
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("input", input);
        
        String jsonPayload = objectMapper.writeValueAsString(payload);
        log.debug("RunPod request payload: {}", jsonPayload);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(runUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("RunPod /run failed with status " + response.statusCode() + ": " + response.body());
        }
        
        JsonNode responseJson = objectMapper.readTree(response.body());
        String jobId = responseJson.get("id").asText();
        
        if (jobId == null || jobId.isEmpty()) {
            throw new RuntimeException("RunPod /run did not return a job ID: " + response.body());
        }
        
        return jobId;
    }

    /**
     * Poll the /status/{id} endpoint until the job is completed.
     * @return The extracted text from the response
     */
    private String pollForCompletion(String jobId) throws Exception {
        String statusUrl = baseUrl + "/status/" + jobId;
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();
        
        while (true) {
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                throw new RuntimeException("RunPod job timed out after " + timeout.getSeconds() + " seconds");
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("RunPod /status failed with status " + response.statusCode() + ": " + response.body());
            }
            
            JsonNode responseJson = objectMapper.readTree(response.body());
            String status = responseJson.get("status").asText();
            
            log.debug("RunPod job {} status: {}", jobId, status);
            
            switch (status) {
                case "COMPLETED":
                    return extractTextFromResponse(responseJson);
                    
                case "FAILED":
                    String error = responseJson.has("error") ? responseJson.get("error").asText() : "Unknown error";
                    throw new RuntimeException("RunPod job failed: " + error);
                    
                case "CANCELLED":
                    throw new RuntimeException("RunPod job was cancelled");
                    
                case "IN_QUEUE":
                case "IN_PROGRESS":
                    // Continue polling
                    Thread.sleep(pollInterval.toMillis());
                    break;
                    
                default:
                    log.warn("Unknown RunPod status: {}, continuing to poll", status);
                    Thread.sleep(pollInterval.toMillis());
            }
        }
    }

    /**
     * Extract the generated text from the RunPod response.
     * Expected format: output[0].choices[0].text or output.text or output directly as string
     */
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
            // Try output[0].text directly
            if (firstOutput.has("text")) {
                return firstOutput.get("text").asText();
            }
            // If first element is just a string
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
        
        // Try output.text directly
        if (output.has("text")) {
            return output.get("text").asText();
        }
        
        // Try output as direct string
        if (output.isTextual()) {
            return output.asText();
        }
        
        // Last resort: return the entire output as string
        log.warn("Could not parse RunPod output format, returning raw: {}", output);
        return output.toString();
    }

    public static class Builder {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Duration timeout = Duration.ofSeconds(180);
        private Duration pollInterval = Duration.ofSeconds(2);

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public RunPodChatModel build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey is required");
            }
            return new RunPodChatModel(this);
        }
    }
}
