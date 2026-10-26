package com.shrivatsav.monomail.core.network.provider.resend

import com.shrivatsav.monomail.core.network.provider.*
import com.shrivatsav.monomail.core.network.remote.resend.*
import com.shrivatsav.monomail.data.model.EmailAttachment
import com.shrivatsav.monomail.core.network.provider.ProviderCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResendProvider @Inject constructor(
    private val apiService: ResendApiService
) : EmailProvider {

    override val providerName: String = "resend"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseDate(dateString: String): Long {
        return try {
            dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override suspend fun listThreads(
        folder: EmailFolder,
        maxResults: Int,
        pageToken: String?,
        query: String?,
        bodyFetchLimitMs: Long?,
        sinceDate: Long?,
        onProgress: ((Float) -> Unit)?
    ): ProviderThreadListResult {
        // Resend API doesn't support pagination tokens, so we ignore pageToken
        // For now, we'll implement basic folder filtering
        if (folder != EmailFolder.INBOX) {
            // Resend doesn't have folder support, return empty for non-INBOX
            return ProviderThreadListResult(emptyList(), null)
        }

        // This method needs the inbox ID and API key from ProviderCache
        // In a real implementation, we'd get these from the authenticated context
        throw UnsupportedOperationException("Resend listThreads requires inbox context")
    }

    override suspend fun getThread(threadId: String, folderHints: List<String>): ProviderThread {
        // This method needs the inbox ID and API key from ProviderCache
        throw UnsupportedOperationException("Resend getThread requires inbox context")
    }

    override suspend fun getAttachmentBytes(messageId: String, attachmentId: String): ByteArray? {
        // Resend doesn't support attachments in their Inboxes API
        return null
    }

    override suspend fun archiveThread(threadId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun unarchiveThread(threadId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun trashThread(threadId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun restoreThread(threadId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun permanentlyDeleteThread(threadId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun toggleStar(threadId: String, starred: Boolean) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun archiveMessage(messageId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun trashMessage(messageId: String) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun toggleMessageStar(messageId: String, starred: Boolean) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun markRead(threadId: String, read: Boolean) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun batchMarkRead(messageIds: List<String>) {
        // Not supported by Resend Inboxes API
    }

    override suspend fun sendEmail(
        from: String,
        to: String,
        subject: String,
        body: String,
        options: SendEmailOptions
    ): SendEmailResult? {
        // This method needs the inbox ID and API key from ProviderCache
        throw UnsupportedOperationException("Resend sendEmail requires inbox context")
    }

    override suspend fun getSendAsAliases(): List<SendAsAlias> {
        // Resend doesn't have send-as aliases
        return emptyList()
    }

    // Helper method to convert Resend DTOs to Provider models
    private fun ResendThreadDto.toProviderThread(inboxId: String, apiKey: String): ProviderThread {
        return ProviderThread(
            threadId = id,
            messages = listOf(), // Messages need to be fetched separately
            isComplete = false
        )
    }

    private fun ResendMessageDto.toProviderMessage(inboxId: String): ProviderMessage {
        return ProviderMessage(
            id = id,
            threadId = "", // This would come from thread context
            subject = subject ?: "",
            from = from,
            fromEmail = from.takeIf { it.contains('<') }?.substringAfter('<')?.substringBefore('>') ?: from,
            to = to.joinToString(", "),
            cc = "",
            bcc = "",
            snippet = text_body?.take(200) ?: html_body?.take(200) ?: "",
            body = html_body ?: text_body ?: "",
            bodyIsHtml = html_body != null,
            date = parseDate(created_at),
            isRead = true, // Resend doesn't track read status
            isStarred = false,
            folders = setOf(EmailFolder.INBOX),
            attachments = emptyList()
        )
    }

    // Context-aware methods for use with ResendRepository
    suspend fun listThreadsWithContext(
        inboxId: String,
        apiKey: String,
        maxResults: Int = 50
    ): List<ResendThreadDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getThreads(
                    inboxId = inboxId,
                    authorization = "Bearer $apiKey"
                )
                response.data.take(maxResults)
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    throw ProviderExceptions.AuthenticationException("Invalid Resend API key")
                }
                throw ProviderExceptions.NetworkException("Failed to fetch threads: ${e.message}")
            } catch (e: Exception) {
                throw ProviderExceptions.NetworkException("Failed to fetch threads: ${e.message}")
            }
        }
    }

    suspend fun getThreadWithContext(
        inboxId: String,
        apiKey: String,
        threadId: String
    ): ResendThreadDetailResponse {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getThreadDetail(
                    inboxId = inboxId,
                    threadId = threadId,
                    authorization = "Bearer $apiKey"
                )
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    throw ProviderExceptions.NotFoundException("Thread not found")
                }
                throw ProviderExceptions.NetworkException("Failed to fetch thread: ${e.message}")
            } catch (e: Exception) {
                throw ProviderExceptions.NetworkException("Failed to fetch thread: ${e.message}")
            }
        }
    }

    suspend fun sendMessageWithContext(
        inboxId: String,
        apiKey: String,
        request: ResendSendRequest
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.sendMessage(
                    inboxId = inboxId,
                    authorization = "Bearer $apiKey",
                    request = request
                )
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}