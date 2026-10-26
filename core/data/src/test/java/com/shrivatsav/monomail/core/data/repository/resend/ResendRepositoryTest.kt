package com.shrivatsav.monomail.core.data.repository.resend

import com.shrivatsav.monomail.core.network.provider.resend.ResendProvider
import com.shrivatsav.monomail.core.network.provider.resend.ResendThreadDto
import com.shrivatsav.monomail.core.network.provider.resend.ResendThreadDetailResponse
import com.shrivatsav.monomail.core.network.provider.resend.ResendMessageDto
import com.shrivatsav.monomail.core.network.provider.ProviderExceptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ResendRepositoryTest {

    @Test
    fun testValidateCredentialsSuccess() = runTest {
        // Given
        val mockProvider = mock<ResendProvider>()
        val repository = ResendRepository(mockProvider)
        val inboxId = "inbox_123"
        val apiKey = "re_test123"
        
        // Mock successful thread list
        whenever(mockProvider.listThreadsWithContext(inboxId, apiKey, 1))
            .thenReturn(emptyList())
        
        // When
        val result = repository.validateCredentials(inboxId, apiKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun testValidateCredentialsFailure() = runTest {
        // Given
        val mockProvider = mock<ResendProvider>()
        val repository = ResendRepository(mockProvider)
        val inboxId = "inbox_123"
        val apiKey = "re_invalid"
        
        // Mock authentication exception
        whenever(mockProvider.listThreadsWithContext(inboxId, apiKey, 1))
            .thenThrow(ProviderExceptions.AuthenticationException("Invalid API key"))
        
        // When
        val result = repository.validateCredentials(inboxId, apiKey)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun testGetThreads() = runTest {
        // Given
        val mockProvider = mock<ResendProvider>()
        val repository = ResendRepository(mockProvider)
        val inboxId = "inbox_123"
        val apiKey = "re_test123"
        
        val expectedThreads = listOf(
            ResendThreadDto(
                id = "thread_1",
                subject = "Test 1",
                snippet = "Snippet 1",
                last_message_at = "2024-01-15T10:30:00.000Z",
                unread = true
            ),
            ResendThreadDto(
                id = "thread_2",
                subject = "Test 2",
                snippet = "Snippet 2",
                last_message_at = "2024-01-14T09:15:00.000Z",
                unread = false
            )
        )
        
        whenever(mockProvider.listThreadsWithContext(inboxId, apiKey, 50))
            .thenReturn(expectedThreads)
        
        // When
        val result = repository.getThreads(inboxId, apiKey)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("thread_1", result[0].id)
        assertEquals("Test 1", result[0].subject)
        assertEquals(true, result[0].unread)
        assertEquals("thread_2", result[1].id)
        assertEquals(false, result[1].unread)
    }

    @Test
    fun testGetThreadDetail() = runTest {
        // Given
        val mockProvider = mock<ResendProvider>()
        val repository = ResendRepository(mockProvider)
        val inboxId = "inbox_123"
        val apiKey = "re_test123"
        val threadId = "thread_456"
        
        val expectedResponse = ResendThreadDetailResponse(
            id = threadId,
            messages = listOf(
                ResendMessageDto(
                    id = "msg_1",
                    from = "sender@example.com",
                    to = listOf("recipient@example.com"),
                    subject = "Test",
                    html_body = "<p>HTML</p>",
                    text_body = "Text",
                    created_at = "2024-01-15T10:30:00.000Z"
                )
            )
        )
        
        whenever(mockProvider.getThreadWithContext(inboxId, apiKey, threadId))
            .thenReturn(expectedResponse)
        
        // When
        val result = repository.getThreadDetail(inboxId, apiKey, threadId)
        
        // Then
        assertEquals(threadId, result.id)
        assertEquals(1, result.messages.size)
        assertEquals("msg_1", result.messages[0].id)
        assertEquals("sender@example.com", result.messages[0].from)
    }

    @Test
    fun testSendMessage() = runTest {
        // Given
        val mockProvider = mock<ResendProvider>()
        val repository = ResendRepository(mockProvider)
        val inboxId = "inbox_123"
        val apiKey = "re_test123"
        val threadId = "thread_456"
        
        whenever(mockProvider.sendMessageWithContext(
            inboxId, apiKey, 
            match {
                it.thread_id == threadId &&
                it.to == listOf("recipient@example.com") &&
                it.subject == "Test Subject" &&
                it.html == "<p>Test</p>"
            }
        )).thenReturn(true)
        
        // When
        val result = repository.sendMessage(
            inboxId, apiKey, threadId,
            to = listOf("recipient@example.com"),
            subject = "Test Subject",
            html = "<p>Test</p>"
        )
        
        // Then
        assertTrue(result)
    }

    @Test
    fun testExtractEmail() {
        val repository = ResendRepository(mock())
        
        // Test with name and email
        assertEquals(
            "john@example.com",
            repository.extractEmail("John Doe <john@example.com>")
        )
        
        // Test with just email
        assertEquals(
            "jane@example.com",
            repository.extractEmail("jane@example.com")
        )
        
        // Test with malformed string
        assertEquals(
            "invalid",
            repository.extractEmail("invalid")
        )
    }

    @Test
    fun testExtractDisplayName() {
        val repository = ResendRepository(mock())
        
        // Test with name and email
        assertEquals(
            "John Doe",
            repository.extractDisplayName("John Doe <john@example.com>")
        )
        
        // Test with just email
        assertEquals(
            "",
            repository.extractDisplayName("jane@example.com")
        )
        
        // Test with malformed string
        assertEquals(
            "",
            repository.extractDisplayName("<invalid>")
        )
    }
}