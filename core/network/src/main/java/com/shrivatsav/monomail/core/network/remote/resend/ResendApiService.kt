package com.shrivatsav.monomail.core.network.remote.resend

import retrofit2.Response
import retrofit2.http.*

interface ResendApiService {
    @GET("v1/inboxes/{inbox_id}/threads")
    suspend fun getThreads(
        @Path("inbox_id") inboxId: String,
        @Header("Authorization") authorization: String
    ): ResendThreadsResponse

    @GET("v1/inboxes/{inbox_id}/threads/{thread_id}")
    suspend fun getThreadDetail(
        @Path("inbox_id") inboxId: String,
        @Path("thread_id") threadId: String,
        @Header("Authorization") authorization: String
    ): ResendThreadDetailResponse

    @POST("v1/inboxes/{inbox_id}/messages")
    suspend fun sendMessage(
        @Path("inbox_id") inboxId: String,
        @Header("Authorization") authorization: String,
        @Body request: ResendSendRequest
    ): Response<Unit>
}

data class ResendThreadsResponse(
    val data: List<ResendThreadDto>
)

data class ResendThreadDto(
    val id: String,
    val subject: String?,
    val snippet: String?,
    val last_message_at: String?,
    val unread: Boolean?
)

data class ResendThreadDetailResponse(
    val id: String,
    val messages: List<ResendMessageDto>
)

data class ResendMessageDto(
    val id: String,
    val from: String,
    val to: List<String>,
    val subject: String?,
    val html_body: String?,
    val text_body: String?,
    val created_at: String
)

data class ResendSendRequest(
    val thread_id: String,
    val to: List<String>,
    val subject: String,
    val html: String
)