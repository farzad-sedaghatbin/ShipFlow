package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderFactory;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for AI-powered features using LangChain4j.
 * 
 * <p>Supports pluggable AI providers through the LLM Provider Plugin System:
 * <ul>
 *   <li>ollama - Local/self-hosted using Ollama</li>
 *   <li>runpod - RunPod Serverless GPU (cloud)</li>
 *   <li>openai - OpenAI ChatGPT</li>
 *   <li>anthropic - Anthropic Claude (future)</li>
 *   <li>google - Google Gemini (future)</li>
 * </ul>
 * 
 * <p>Switch between providers using: app.ai.provider=openai (or ollama, runpod, etc.)
 * 
 * <p>To add a new provider:
 * <ol>
 *   <li>Add provider type to LLMProviderType enum</li>
 *   <li>Create implementation of LLMProvider interface</li>
 *   <li>Add configuration properties for the new provider</li>
 * </ol>
 */
@Configuration
@Slf4j
public class AIConfig {

    @Value("${app.ai.risk-analysis.enabled:true}")
    private boolean aiRiskAnalysisEnabled;

    @Value("${app.ai.provider:ollama}")
    private String aiProvider;

    @Value("${app.ai.temperature:0.3}")
    private double temperature;

    @Value("${app.ai.max-tokens:2048}")
    private int maxTokens;

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

    // OpenAI Configuration
    @Value("${app.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.ai.openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${app.ai.openai.base-url:}")
    private String openaiBaseUrl;

    @Value("${app.ai.openai.timeout:120}")
    private int openaiTimeout;

    @Value("${app.ai.openai.organization-id:}")
    private String openaiOrganizationId;

    @Autowired
    private LLMProviderFactory llmProviderFactory;

    /**
     * Creates the AI chat model bean when AI is enabled.
     * Uses the pluggable LLM Provider system to create the appropriate model.
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.risk-analysis.enabled", havingValue = "true", matchIfMissing = false)
    public ChatLanguageModel chatLanguageModel() {
        log.info("Creating ChatLanguageModel with provider: {}", aiProvider);
        
        LLMProviderType providerType;
        try {
            providerType = LLMProviderType.fromConfigValue(aiProvider);
        } catch (IllegalArgumentException e) {
            log.error("Invalid AI provider '{}'. Available providers: {}", 
                    aiProvider, LLMProviderType.getSupportedProviders());
            throw e;
        }

        LLMProviderConfig config = buildProviderConfig(providerType);
        
        if (!llmProviderFactory.isProviderAvailable(providerType)) {
            throw new IllegalStateException(
                "Provider " + providerType + " is not available. " +
                "Available providers: " + llmProviderFactory.getAvailableProviders()
            );
        }
        
        return llmProviderFactory.createModel(providerType, config);
    }

    /**
     * Build provider-specific configuration based on the selected provider type.
     */
    private LLMProviderConfig buildProviderConfig(LLMProviderType providerType) {
        LLMProviderConfig.Builder configBuilder = LLMProviderConfig.builder()
                .temperature(temperature)
                .maxTokens(maxTokens);

        switch (providerType) {
            case OLLAMA:
                configBuilder
                        .baseUrl(ollamaBaseUrl)
                        .modelName(ollamaModel)
                        .timeout(Duration.ofSeconds(ollamaTimeout));
                break;

            case RUNPOD:
                configBuilder
                        .baseUrl(runpodBaseUrl)
                        .apiKey(runpodApiKey)
                        .modelName(runpodModel)
                        .timeout(Duration.ofSeconds(runpodTimeout))
                        .extraParam("pollInterval", Duration.ofSeconds(runpodPollInterval));
                break;

            case OPENAI:
                configBuilder
                        .apiKey(openaiApiKey)
                        .modelName(openaiModel)
                        .timeout(Duration.ofSeconds(openaiTimeout));
                
                if (openaiBaseUrl != null && !openaiBaseUrl.trim().isEmpty()) {
                    configBuilder.baseUrl(openaiBaseUrl);
                }
                if (openaiOrganizationId != null && !openaiOrganizationId.trim().isEmpty()) {
                    configBuilder.extraParam("organizationId", openaiOrganizationId);
                }
                break;

            case ANTHROPIC:
            case GOOGLE:
            case AZURE_OPENAI:
                log.warn("Provider {} is defined but not yet fully implemented. " +
                        "Using default configuration.", providerType);
                break;

            default:
                log.warn("Unknown provider type: {}. Using default configuration.", providerType);
        }

        return configBuilder.build();
    }

    public boolean isAiRiskAnalysisEnabled() {
        return aiRiskAnalysisEnabled;
    }

    public String getProvider() {
        return aiProvider;
    }

    public String getModelName() {
        try {
            LLMProviderType providerType = LLMProviderType.fromConfigValue(aiProvider);
            switch (providerType) {
                case OLLAMA:
                    return ollamaModel;
                case RUNPOD:
                    return runpodModel;
                case OPENAI:
                    return openaiModel;
                default:
                    return ollamaModel;
            }
        } catch (IllegalArgumentException e) {
            return ollamaModel;
        }
    }

    public String getAiProvider() {
        return aiProvider;
    }
}
