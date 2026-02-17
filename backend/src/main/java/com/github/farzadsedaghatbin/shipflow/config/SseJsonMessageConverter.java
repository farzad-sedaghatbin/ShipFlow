package com.github.farzadsedaghatbin.shipflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Custom message converter to handle Server-Sent Events (SSE) responses from MCP servers.
 * SSE format: each line starts with "data: " followed by JSON content.
 */
public class SseJsonMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    private final ObjectMapper objectMapper;
    private static final MediaType TEXT_EVENT_STREAM = MediaType.parseMediaType("text/event-stream");

    public SseJsonMessageConverter(ObjectMapper objectMapper) {
        super(TEXT_EVENT_STREAM);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;  // Support all classes
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return mediaType != null && TEXT_EVENT_STREAM.includes(mediaType);
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return false;  // We don't write SSE format
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return readFromStream(inputMessage);
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return readFromStream(inputMessage);
    }

    private Object readFromStream(HttpInputMessage inputMessage) throws IOException {
        StringBuilder jsonData = new StringBuilder();
        boolean jsonCollected = false;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputMessage.getBody(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // SSE format: lines starting with "data: " contain the JSON
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();  // Remove "data: " prefix
                    if (!data.isEmpty() && !data.equals("[DONE]")) {
                        // Collect only the first non-empty JSON message to avoid
                        // concatenating multiple JSON roots (e.g., "{...}{...}").
                        jsonData.append(data);
                        jsonCollected = true;
                        break;
                    }
                }
            }
        }

        // Parse the accumulated JSON
        String json = jsonData.toString();
        if (json.isEmpty()) {
            return null;
        }

        return objectMapper.readValue(json, Object.class);
    }

    @Override
    protected void writeInternal(Object t, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Writing SSE format is not supported");
    }
}
