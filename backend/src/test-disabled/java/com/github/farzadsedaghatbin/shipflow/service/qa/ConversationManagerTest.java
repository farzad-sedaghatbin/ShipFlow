package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.ConversationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConversationManagerTest {

    private ConversationManager manager;

    @BeforeEach
    void setUp() {
        manager = new ConversationManager();
    }

    @Test
    void testCreateConversation() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        assertNotNull(convId);
        assertTrue(convId.startsWith("conv-"));
    }

    @Test
    void testGetOrCreateConversation_newConversation() {
        ConversationContext context = manager.getOrCreateConversation(null, 1L, "PITCH", 123L);
        
        assertNotNull(context);
        assertNotNull(context.getConversationId());
        assertEquals(1L, context.getUserId());
        assertEquals("PITCH", context.getContextType());
        assertEquals(123L, context.getContextId());
        assertTrue(context.getTurns().isEmpty());
    }

    @Test
    void testGetOrCreateConversation_existingConversation() {
        // Create initial conversation
        String convId = manager.createConversation(1L, "PITCH", 123L);
        manager.addTurn(convId, "Question 1", "Answer 1");
        
        // Get the same conversation
        ConversationContext context = manager.getOrCreateConversation(convId, 1L, "PITCH", 123L);
        
        assertEquals(convId, context.getConversationId());
        assertEquals(1, context.getTurns().size());
        assertEquals("Question 1", context.getTurns().get(0).getQuestion());
    }

    @Test
    void testAddTurn() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        manager.addTurn(convId, "What is the status?", "The pitch is in progress.");
        
        ConversationContext context = manager.getOrCreateConversation(convId, 1L, "PITCH", 123L);
        assertEquals(1, context.getTurns().size());
        assertEquals("What is the status?", context.getTurns().get(0).getQuestion());
        assertEquals("The pitch is in progress.", context.getTurns().get(0).getAnswer());
        assertNotNull(context.getTurns().get(0).getTimestamp());
    }

    @Test
    void testAddMultipleTurns() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        manager.addTurn(convId, "Question 1", "Answer 1");
        manager.addTurn(convId, "Question 2", "Answer 2");
        manager.addTurn(convId, "Question 3", "Answer 3");
        
        ConversationContext context = manager.getOrCreateConversation(convId, 1L, "PITCH", 123L);
        assertEquals(3, context.getTurns().size());
        assertEquals("Question 3", context.getTurns().get(2).getQuestion());
    }

    @Test
    void testBuildConversationHistory_emptyConversation() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        String history = manager.buildConversationHistory(convId, 5);
        
        assertEquals("", history);
    }

    @Test
    void testBuildConversationHistory_singleTurn() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        manager.addTurn(convId, "What is the deadline?", "The deadline is next Friday.");
        
        String history = manager.buildConversationHistory(convId, 5);
        
        assertTrue(history.contains("Previous conversation:"));
        assertTrue(history.contains("User: What is the deadline?"));
        assertTrue(history.contains("Assistant: The deadline is next Friday."));
    }

    @Test
    void testBuildConversationHistory_multipleTurns() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        manager.addTurn(convId, "Question 1", "Answer 1");
        manager.addTurn(convId, "Question 2", "Answer 2");
        manager.addTurn(convId, "Question 3", "Answer 3");
        
        String history = manager.buildConversationHistory(convId, 5);
        
        assertTrue(history.contains("Question 1"));
        assertTrue(history.contains("Answer 1"));
        assertTrue(history.contains("Question 2"));
        assertTrue(history.contains("Answer 2"));
        assertTrue(history.contains("Question 3"));
        assertTrue(history.contains("Answer 3"));
    }

    @Test
    void testBuildConversationHistory_limitsToMaxTurns() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        manager.addTurn(convId, "Question 1", "Answer 1");
        manager.addTurn(convId, "Question 2", "Answer 2");
        manager.addTurn(convId, "Question 3", "Answer 3");
        manager.addTurn(convId, "Question 4", "Answer 4");
        manager.addTurn(convId, "Question 5", "Answer 5");
        
        String history = manager.buildConversationHistory(convId, 2);
        
        // Should only include last 2 turns
        assertFalse(history.contains("Question 1"));
        assertFalse(history.contains("Question 2"));
        assertFalse(history.contains("Question 3"));
        assertTrue(history.contains("Question 4"));
        assertTrue(history.contains("Question 5"));
    }

    @Test
    void testBuildConversationHistory_nonExistentConversation() {
        String history = manager.buildConversationHistory("non-existent-id", 5);
        
        assertEquals("", history);
    }

    @Test
    void testGetConversation_exists() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        ConversationContext context = manager.getConversation(convId);
        
        assertNotNull(context);
        assertEquals(convId, context.getConversationId());
    }

    @Test
    void testGetConversation_notExists() {
        ConversationContext context = manager.getConversation("non-existent-id");
        
        assertNull(context);
    }

    @Test
    void testCleanupExpiredConversations() {
        // This test is tricky because we can't easily expire conversations
        // We'll just verify the cleanup method doesn't crash
        String convId = manager.createConversation(1L, "PITCH", 123L);
        
        int removed = manager.cleanupExpiredConversations();
        
        // Should be 0 since the conversation is fresh
        assertEquals(0, removed);
        
        // Conversation should still exist
        assertNotNull(manager.getConversation(convId));
    }

    @Test
    void testConversationContext_isNotExpired() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        ConversationContext context = manager.getConversation(convId);
        
        assertFalse(context.isExpired());
    }

    @Test
    void testConversationContext_getRecentTurns() {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        manager.addTurn(convId, "Q1", "A1");
        manager.addTurn(convId, "Q2", "A2");
        manager.addTurn(convId, "Q3", "A3");
        manager.addTurn(convId, "Q4", "A4");
        
        ConversationContext context = manager.getConversation(convId);
        var recentTurns = context.getRecentTurns(2);
        
        assertEquals(2, recentTurns.size());
        assertEquals("Q3", recentTurns.get(0).getQuestion());
        assertEquals("Q4", recentTurns.get(1).getQuestion());
    }

    @Test
    void testAddTurn_updatesLastActivity() throws InterruptedException {
        String convId = manager.createConversation(1L, "PITCH", 123L);
        ConversationContext context1 = manager.getConversation(convId);
        LocalDateTime firstActivity = context1.getLastActivityAt();
        
        Thread.sleep(10); // Small delay to ensure timestamp difference
        
        manager.addTurn(convId, "Q1", "A1");
        ConversationContext context2 = manager.getConversation(convId);
        LocalDateTime secondActivity = context2.getLastActivityAt();
        
        assertTrue(secondActivity.isAfter(firstActivity));
    }

    @Test
    void testMultipleConversations_independent() {
        String conv1 = manager.createConversation(1L, "PITCH", 123L);
        String conv2 = manager.createConversation(2L, "MEETING", 456L);
        
        manager.addTurn(conv1, "Pitch question", "Pitch answer");
        manager.addTurn(conv2, "Meeting question", "Meeting answer");
        
        ConversationContext context1 = manager.getConversation(conv1);
        ConversationContext context2 = manager.getConversation(conv2);
        
        assertEquals(1, context1.getTurns().size());
        assertEquals(1, context2.getTurns().size());
        assertEquals("Pitch question", context1.getTurns().get(0).getQuestion());
        assertEquals("Meeting question", context2.getTurns().get(0).getQuestion());
    }
}
