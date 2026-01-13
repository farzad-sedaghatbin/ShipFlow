package com.github.farzadsedaghatbin.shipflow.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for AI-powered features using LangChain4j.
 * Supports two AI providers:
 * - RunPod Serverless GPU (default) - cloud-based, pay-per-use
 * - Ollama (local) - self-hosted, free, requires local GPU/CPU
 * 
 * Switch between providers using: app.ai.provider=runpod or app.ai.provider=ollama
 */
@Configuration
@Slf4j
public class AIConfig {

    @Value("${app.ai.risk-analysis.enabled:true}")
    private boolean aiRiskAnalysisEnabled;

    @Value("${app.ai.provider:runpod}")
    private String aiProvider;

    // RunPod Configuration
    @Value("${app.ai.runpod.base-url:https://api.runpod.ai/v2/}")
    private String runpodBaseUrl;

    @Value("${app.ai.runpod.api-key:}")
    private String runpodApiKey;

    @Value("${app.ai.runpod.model:mistral:instruct}")
    private String runpodModel;

    @Value("${app.ai.runpod.timeout:180}")
    private int runpodTimeout;

    @Value("${app.ai.runpod.poll-interval:2}")
    private int runpodPollInterval;

    // Ollama Configuration (for local/self-hosted deployments)
    @Value("${app.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ai.ollama.model:mistral:instruct}")
    private String ollamaModel;

    @Value("${app.ai.ollama.timeout:180}")
    private int ollamaTimeout;

    /**
     * Creates the AI chat model bean when AI is enabled.
     * Automatically selects RunPod or Ollama based on app.ai.provider setting.
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.risk-analysis.enabled", havingValue = "true", matchIfMissing = false)
    public ChatLanguageModel chatLanguageModel() {
        if ("ollama".equalsIgnoreCase(aiProvider)) {
            return createOllamaChatModel();
        } else {
            return createRunPodChatModel();
        }
    }

    /**
     * Creates RunPod Serverless GPU chat model (cloud-based).
     * Best for production deployments without local GPU.
     */
    private ChatLanguageModel createRunPodChatModel() {
        log.info("Initializing AI with RunPod Serverless GPU - Model: {}, URL: {}", runpodModel, runpodBaseUrl);
        
        return RunPodChatModel.builder()
                .baseUrl(runpodBaseUrl)
                .apiKey(runpodApiKey)
                .modelName(runpodModel)
                .timeout(Duration.ofSeconds(runpodTimeout))
                .pollInterval(Duration.ofSeconds(runpodPollInterval))
                .build();
    }

    /**
     * Creates Ollama chat model (local/self-hosted).
     * Best for development or self-hosted deployments with local GPU/CPU.
     */
    private ChatLanguageModel createOllamaChatModel() {
        log.info("Initializing AI with Ollama (local) - Model: {}, URL: {}", ollamaModel, ollamaBaseUrl);
        
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .timeout(Duration.ofSeconds(ollamaTimeout))
                .temperature(0.3) // Lower temperature for more consistent analysis
                .build();
    }

    public boolean isAiRiskAnalysisEnabled() {
        return aiRiskAnalysisEnabled;
    }

    public String getModelName() {
        return "ollama".equalsIgnoreCase(aiProvider) ? ollamaModel : runpodModel;
    }

    public String getAiProvider() {
        return aiProvider;
    }
}
