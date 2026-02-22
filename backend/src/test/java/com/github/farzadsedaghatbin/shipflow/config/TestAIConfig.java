package com.github.farzadsedaghatbin.shipflow.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.security.CustomUserDetailsService;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Test configuration that provides mock beans for dependencies that are needed
 * by @WebMvcTest but should not require actual configuration in tests.
 *
 * <p>
 * IMPORTANT: This uses @TestConfiguration which is NOT auto-detected by
 * component scanning. It must be explicitly imported
 * using @Import(TestAIConfig.class) in tests that need it.
 *
 * <p>
 * Only @WebMvcTest tests should import this configuration, not @SpringBootTest
 * integration tests.
 */
@TestConfiguration
@EnableMethodSecurity
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class, 
    OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
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

  /**
   * Mock EntityManagerFactory bean to prevent JPA from trying to initialize
   */
  @Bean
  @Primary
  public jakarta.persistence.EntityManagerFactory entityManagerFactory() {
    jakarta.persistence.EntityManagerFactory emf = mock(jakarta.persistence.EntityManagerFactory.class);
    jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
    jakarta.persistence.metamodel.Metamodel metamodel = mock(jakarta.persistence.metamodel.Metamodel.class);
    
    when(emf.createEntityManager()).thenReturn(em);
    when(em.getMetamodel()).thenReturn(metamodel);
    when(em.getDelegate()).thenReturn(em);
    when(em.isOpen()).thenReturn(true);
    
    return emf;
  }

  /**
   * Mock JPA MappingContext to prevent "Metamodel must not be null" error
   */
  @Bean
  @Primary
  public org.springframework.data.mapping.context.MappingContext<?, ?> jpaMappingContext() {
    org.springframework.data.mapping.context.MappingContext<?, ?> context =  
        mock(org.springframework.data.mapping.context.MappingContext.class);
    return context;
  }

  /**
   * Test security filter chain that allows webhook endpoints to bypass
   * authentication and returns 401 for unauthenticated requests to other
   * endpoints
   */
  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/github/webhook/**").permitAll().anyRequest().authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .build();
  }
}
