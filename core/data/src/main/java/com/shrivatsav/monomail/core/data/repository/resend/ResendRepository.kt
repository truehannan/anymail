package com.shrivatsav.monomail.core.data.repository.resend

import com.shrivatsav.monomail.core.network.provider.resend.ResendProvider
import com.shrivatsav.monomail.core.network.provider.resend.ResendThreadDto
import com.shrivatsav.monomail.core.network.provider.resend.ResendThreadDetailResponse
import com.shrivatsav.monomail.core.network.provider.resend.ResendSendRequest
import com.shrivatsav.monomail.core.network.provider.ProviderExceptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResendRepository @Inject constructor(
    private val resendProvider: ResendProvider
) {

    suspend fun validateCredentials(inboxId: String, apiKey: String): Boolean {
        return try {
            // Test credentials by fetching first thread
            resendProvider.listThreadsWithContext(inboxId, apiKey, maxResults = 1)
            true
        } catch (e: ProviderExceptions.AuthenticationException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getThreads(
        inboxId: String,
        apiKey: String,
        maxResults: Int = 50
    ): List<ResendThreadDto> {
        return resendProvider.listThreadsWithContext(inboxId, apiKey, maxResults)
    }

    fun getThreadsFlow(
        inboxId: String,
        apiKey: String,
        maxResults: Int = 50
    ): Flow<List<ResendThreadDto>> = flow {
        val threads = getThreads(inboxId, apiKey, maxResults)
        emit(threads)
    }

    suspend fun getThreadDetail(
        inboxId: String,
        apiKey: String,
        threadId: String
    ): ResendThreadDetailResponse {
        return resendProvider.getThreadWithContext(inboxId, apiKey, threadId)
    }

    suspend fun sendMessage(
        inboxId: String,
        apiKey: String,
        threadId: String,
        to: List<String>,
        subject: String,
        html: String
    ): Boolean {
        val request = ResendSendRequest(
            thread_id = threadId,
            to = to,
            subject = subject,
            html = html
        )
        return resendProvider.sendMessageWithContext(inboxId, apiKey, request)
    }

    // Helper method to extract email from "Name <email@example.com>" format
    fun extractEmail(from: String): String {
        return from.takeIf { it.contains('<') }?.substringAfter('<')?.substringBefore('>') ?: from
    }

    // Helper method to extract display name from "Name <email@example.com>" format
    fun extractDisplayName(from: String): String {
        return if (from.contains('<')) {
            from.substringBefore('<').trim()
        } else {
            ""
        }
    }
}