package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.security.CustomUserDetailsService;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides mock beans for dependencies
 * that are needed by @WebMvcTest but should not require actual configuration in tests.
 */
@TestConfiguration
public class TestAIConfig {

    @Bean
    @Primary
    public ChatLanguageModel testChatLanguageModel() {
        ChatLanguageModel mock = mock(ChatLanguageModel.class);
        // Provide default mock behavior - return a simple string response
        when(mock.generate(any(String.class))).thenReturn("Test response");
        return mock;
    }
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
}
