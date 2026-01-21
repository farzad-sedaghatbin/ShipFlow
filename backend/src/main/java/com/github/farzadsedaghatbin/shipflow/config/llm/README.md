# LLM Provider Plugin System

This directory contains the pluggable LLM (Large Language Model) provider architecture for ShipFlow.

## Overview

The LLM plugin system allows ShipFlow to integrate with multiple AI providers through a unified interface. This makes it easy to:

- Switch between providers via configuration
- Add new providers without modifying existing code
- Test with local models while deploying with cloud providers

## Architecture

```
llm/
├── LLMProvider.java           # Interface that all providers must implement
├── LLMProviderConfig.java     # Configuration container for provider settings
├── LLMProviderFactory.java    # Factory that creates providers based on config
├── LLMProviderType.java       # Enum of supported provider types
└── providers/
    ├── OllamaLLMProvider.java    # Ollama (local/self-hosted)
    ├── OpenAILLMProvider.java    # OpenAI ChatGPT
    └── RunPodLLMProvider.java    # RunPod Serverless GPU
```

## Supported Providers

| Provider | Class | Use Case |
|----------|-------|----------|
| Ollama | `OllamaLLMProvider` | Local development, privacy-first |
| OpenAI | `OpenAILLMProvider` | Production, high-quality responses |
| RunPod | `RunPodLLMProvider` | Scalable GPU compute |

## Adding a New Provider

### Step 1: Add Provider Type

Add a new entry to `LLMProviderType.java`:

```java
public enum LLMProviderType {
    // ... existing types ...
    MY_PROVIDER("my-provider", "My Custom Provider"),
}
```

### Step 2: Create Provider Implementation

Create a new class in `providers/` that implements `LLMProvider`:

```java
package com.github.farzadsedaghatbin.shipflow.config.llm.providers;

import com.github.farzadsedaghatbin.shipflow.config.llm.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyLLMProvider implements LLMProvider {

    @Override
    public LLMProviderType getProviderType() {
        return LLMProviderType.MY_PROVIDER;
    }

    @Override
    public ChatLanguageModel createModel(LLMProviderConfig config) {
        validateConfig(config);
        
        // Create and return your ChatLanguageModel implementation
        return MyModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .build();
    }

    @Override
    public void validateConfig(LLMProviderConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new IllegalArgumentException("My Provider requires an API key");
        }
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public boolean requiresBaseUrl() {
        return false;
    }
}
```

### Step 3: Add Configuration Properties

Add configuration in `application.properties`:

```properties
# My Provider Configuration
app.ai.my-provider.api-key=${MY_PROVIDER_API_KEY:}
app.ai.my-provider.model=${MY_PROVIDER_MODEL:default-model}
app.ai.my-provider.timeout=${MY_PROVIDER_TIMEOUT:120}
```

### Step 4: Update AIConfig

Add configuration handling in `AIConfig.java`:

```java
// Add @Value properties
@Value("${app.ai.my-provider.api-key:}")
private String myProviderApiKey;

@Value("${app.ai.my-provider.model:default-model}")
private String myProviderModel;

// Add to buildProviderConfig switch
case MY_PROVIDER:
    configBuilder
            .apiKey(myProviderApiKey)
            .modelName(myProviderModel)
            .timeout(Duration.ofSeconds(myProviderTimeout));
    break;
```

### Step 5: Add Dependency (if needed)

If your provider requires a LangChain4j module, add it to `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-my-provider</artifactId>
    <version>0.35.0</version>
</dependency>
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AI_PROVIDER` | Provider to use | `ollama` |
| `AI_TEMPERATURE` | Response randomness (0-1) | `0.3` |
| `AI_MAX_TOKENS` | Max response length | `2048` |

### Provider-Specific Variables

See provider implementations for their specific configuration options.

## Usage

The `LLMProviderFactory` is automatically injected into services that need LLM capabilities:

```java
@Service
public class MyService {
    
    private final ChatLanguageModel chatModel;
    
    public MyService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }
    
    public String analyze(String input) {
        return chatModel.generate(input);
    }
}
```

## Testing

For testing, you can mock the `ChatLanguageModel` or use a lightweight provider like Ollama with a small model.

```java
@MockBean
private ChatLanguageModel chatLanguageModel;

@Test
void testAnalysis() {
    when(chatLanguageModel.generate(anyString())).thenReturn("Mock response");
    // ...
}
```

## Future Providers

The following providers are planned for future implementation:

- **Anthropic Claude** - High-quality reasoning, safety-focused
- **Google Gemini** - Multimodal capabilities
- **Azure OpenAI** - Enterprise OpenAI deployment
- **AWS Bedrock** - Multi-model cloud service
- **Cohere** - Retrieval and generation
