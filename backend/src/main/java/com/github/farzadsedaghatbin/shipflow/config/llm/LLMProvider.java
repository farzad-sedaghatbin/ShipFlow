package com.github.farzadsedaghatbin.shipflow.config.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Interface for LLM provider plugins.
 * Implement this interface to add support for a new LLM provider.
 * 
 * <p>Each provider implementation should:
 * <ul>
 *   <li>Return its supported provider type via {@link #getProviderType()}</li>
 *   <li>Create and return a properly configured ChatLanguageModel via {@link #createModel(LLMProviderConfig)}</li>
 *   <li>Validate that required configuration parameters are present</li>
 * </ul>
 * 
 * <p>Example implementation:
 * <pre>
 * {@code
 * @Component
 * public class MyLLMProvider implements LLMProvider {
 *     @Override
 *     public LLMProviderType getProviderType() {
 *         return LLMProviderType.MY_PROVIDER;
 *     }
 *     
 *     @Override
 *     public ChatLanguageModel createModel(LLMProviderConfig config) {
 *         validateConfig(config);
 *         return MyModel.builder()
 *             .apiKey(config.getApiKey())
 *             .modelName(config.getModelName())
 *             .build();
 *     }
 * }
 * }
 * </pre>
 */
public interface LLMProvider {

    /**
     * Get the provider type this implementation supports.
     * 
     * @return the LLM provider type
     */
    LLMProviderType getProviderType();

    /**
     * Create a ChatLanguageModel instance configured with the given settings.
     * 
     * @param config the provider configuration
     * @return a configured ChatLanguageModel instance
     * @throws IllegalArgumentException if required configuration is missing
     * @throws RuntimeException if the model cannot be created
     */
    ChatLanguageModel createModel(LLMProviderConfig config);

    /**
     * Validate that the configuration contains all required parameters for this provider.
     * Called automatically by the factory before creating the model.
     * 
     * @param config the provider configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    default void validateConfig(LLMProviderConfig config) {
        // Default implementation - providers can override for specific validation
    }

    /**
     * Get a human-readable display name for this provider.
     * 
     * @return the display name
     */
    default String getDisplayName() {
        return getProviderType().getDisplayName();
    }

    /**
     * Check if this provider requires an API key.
     * 
     * @return true if an API key is required
     */
    default boolean requiresApiKey() {
        return true;
    }

    /**
     * Check if this provider requires a base URL.
     * 
     * @return true if a base URL is required
     */
    default boolean requiresBaseUrl() {
        return false;
    }
}
