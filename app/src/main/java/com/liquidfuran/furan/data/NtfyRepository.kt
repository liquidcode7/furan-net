package com.liquidfuran.furan.data

import com.liquidfuran.furan.model.NtfyConfig
import com.liquidfuran.furan.util.HmacUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApprovalResult {
    object Approved : ApprovalResult()
    data class Denied(val reason: String) : ApprovalResult()
}

@Singleton
class NtfyRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val retrofit: Retrofit
) {
    private fun apiFor(serverUrl: String): NtfyApi =
        retrofit.newBuilder()
            .baseUrl(serverUrl.trimEnd('/') + "/")
            .build()
            .create(NtfyApi::class.java)

    suspend fun publishUnlockRequest(config: NtfyConfig): Result<Unit> {
        if (!config.isConfigured) return Result.failure(IllegalStateException("ntfy not configured"))
        return runCatching {
            val body = "UNLOCK_REQUEST:${System.currentTimeMillis() / 1000}"
                .toRequestBody("text/plain; charset=utf-8".toMediaType())
            val response = apiFor(config.serverUrl).publish(config.requestTopic, body)
            if (!response.isSuccessful) {
                throw Exception("ntfy publish failed: ${response.code()}")
            }
        }
    }

    /**
     * Subscribes to the approval topic via SSE and emits [ApprovalResult].
     *
     * Only emits [ApprovalResult.Denied] when a message explicitly starts with "APPROVE:"
     * but fails HMAC/timestamp validation — so stray messages to the topic (test pings,
     * keepalives, etc.) are silently ignored rather than terminating the listener.
     *
     * Uses `since=0` so only messages published after subscription are delivered.
     */
    fun listenForApproval(config: NtfyConfig): Flow<ApprovalResult> = callbackFlow {
        // since=0 tells ntfy to stream only new messages from this point forward
        val url = "${config.serverUrl.trimEnd('/')}/${config.approvalTopic}/sse?since=0"
        val request = Request.Builder().url(url).build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                // Only process "message" type events (ignore "open", "keepalive", etc.)
                if (type != "message") return

                val message = extractMessageBody(data) ?: return

                // Silently ignore messages that aren't APPROVE attempts
                if (!message.startsWith("APPROVE:")) return

                val result = processApproval(message, config.sharedSecret)
                trySend(result)
                if (result is ApprovalResult.Approved) {
                    eventSource.cancel()
                    channel.close()
                }
                // On denial, keep the channel open so the user can try again
                // (they'd need to restart the request, but we don't hard-close here)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed: ${response?.code()}"))
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    /**
     * Extracts the `message` field from ntfy's SSE JSON payload.
     * ntfy format: {"id":"...","event":"message","time":1234,"message":"..."}
     */
    private fun extractMessageBody(data: String): String? {
        return try {
            // Simple extraction — avoids a full JSON dependency for one field
            val match = """"message"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex().find(data)
            match?.groupValues?.getOrNull(1)
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
        } catch (e: Exception) {
            null
        }
    }

    private fun processApproval(message: String, secret: String): ApprovalResult {
        return if (HmacUtil.verifyApproval(message, secret)) {
            ApprovalResult.Approved
        } else {
            ApprovalResult.Denied("Invalid or expired approval token")
        }
    }
}
