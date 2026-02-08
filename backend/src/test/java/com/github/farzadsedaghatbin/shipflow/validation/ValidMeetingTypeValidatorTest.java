package com.github.farzadsedaghatbin.shipflow.validation;

import com.github.farzadsedaghatbin.shipflow.dto.CreateMeetingRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for CreateMeetingRequest type normalization via setter.
 * The custom validator integration is tested through existing MeetingControllerIntegrationTest.
 * 
 * Note: Lombok @Builder does not call custom setters, so normalization only applies when
 * setType() is called directly (e.g., during JSON deserialization by Jackson).
 */
class ValidMeetingTypeValidatorTest {

  @Test
  void testNormalization_shouldTrimAndUppercase() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType("  kickoff  "); // lowercase with spaces

    // Then - the type should be normalized
    assertThat(request.getType()).isEqualTo("KICKOFF");
  }

  @Test
  void testNormalization_shouldHandleNull() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType(null);

    // Then - should not throw NPE
    assertThat(request.getType()).isNull();
  }

  @Test
  void testNormalization_shouldConvertMixedCase() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType("KickOff");

    // Then
    assertThat(request.getType()).isEqualTo("KICKOFF");
  }

  @Test
  void testNormalization_shouldHandleCustomTypes() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType("  custom_type_123  ");

    // Then
    assertThat(request.getType()).isEqualTo("CUSTOM_TYPE_123");
  }

  @Test
  void testNormalization_shouldHandleWhitespace() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType("   ");

    // Then - whitespace only becomes empty string
    assertThat(request.getType()).isEmpty();
  }
  
  @Test
  void testNormalization_preservesUnderscoresAndHyphens() {
    // Given
    CreateMeetingRequest request = new CreateMeetingRequest();
    
    // When
    request.setType("custom-type_123");

    // Then
    assertThat(request.getType()).isEqualTo("CUSTOM-TYPE_123");
  }
}
