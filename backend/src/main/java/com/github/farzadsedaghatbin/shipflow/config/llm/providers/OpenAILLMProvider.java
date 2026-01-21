package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM Provider implementation for OpenAI ChatGPT.
 * 
 * <p>OpenAI/ChatGPT is ideal for:
 * <ul>
 *   <li>Production deployments requiring high-quality responses</li>
 *   <li>Complex reasoning and analysis tasks</li>
 *   <li>Broad general knowledge applications</li>
 * </ul>
 * 
 * <p>Supported models:
 * <ul>
 *   <li>gpt-4o - Most capable model for complex tasks</li>
 *   <li>gpt-4o-mini - Fast and cost-effective</li>
 *   <li>gpt-4-turbo - Previous generation, still powerful</li>
 *   <li>gpt-3.5-turbo - Fast and cost-effective for simpler tasks</li>
 * </ul>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>apiKey: OpenAI API key (required)</li>
 *   <li>modelName: Model to use (default: gpt-4o-mini)</li>
 *   <li>baseUrl: Optional custom endpoint (for Azure OpenAI or proxies)</li>
 *   <li>temperature: Model temperature (0.0-2.0)</li>
 *   <li>maxTokens: Maximum tokens in response</li>
 *   <li>organizationId: Optional OpenAI organization ID</li>
 * </ul>
 */
@Component
@Slf4j
public class OpenAILLMProvider implements LLMProvider {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    @Override
    public LLMProviderType getProviderType() {
        return LLMProviderType.OPENAI;
    }

    @Override
    public ChatLanguageModel createModel(LLMProviderConfig config) {
        validateConfig(config);
        
        String modelName = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        
        log.info("Creating OpenAI ChatLanguageModel - Model: {}", modelName);
        
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(modelName);
        
        // Optional base URL for custom endpoints (Azure, proxies, etc.)
        if (config.getBaseUrl() != null && !config.getBaseUrl().trim().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        
        if (config.getTimeout() != null) {
            builder.timeout(config.getTimeout());
        }
        
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        
        // Handle extra parameters specific to OpenAI
        String organizationId = config.getExtraParam("organizationId", null);
        if (organizationId != null && !organizationId.trim().isEmpty()) {
            builder.organizationId(organizationId);
        }
        
        Double topP = config.getExtraParam("topP", null);
        if (topP != null) {
            builder.topP(topP);
        }
        
        Double frequencyPenalty = config.getExtraParam("frequencyPenalty", null);
        if (frequencyPenalty != null) {
            builder.frequencyPenalty(frequencyPenalty);
        }
        
        Double presencePenalty = config.getExtraParam("presencePenalty", null);
        if (presencePenalty != null) {
            builder.presencePenalty(presencePenalty);
        }
        
        // Logging callback (disabled by default for security)
        Boolean logRequests = config.getExtraParam("logRequests", false);
        Boolean logResponses = config.getExtraParam("logResponses", false);
        builder.logRequests(logRequests);
        builder.logResponses(logResponses);
        
        return builder.build();
    }

    @Override
    public void validateConfig(LLMProviderConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI requires an API key (app.ai.openai.api-key). " +
                    "Get one at https://platform.openai.com/api-keys");
        }
        
        // Validate API key format (should start with sk-)
        if (!config.getApiKey().startsWith("sk-")) {
            log.warn("OpenAI API key does not start with 'sk-'. Ensure you're using a valid API key.");
        }
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public boolean requiresBaseUrl() {
        return false; // Base URL is optional (defaults to OpenAI API)
    }
}
