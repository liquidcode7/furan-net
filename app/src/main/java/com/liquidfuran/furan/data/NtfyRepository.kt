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

    fun listenForApproval(config: NtfyConfig): Flow<ApprovalResult> = callbackFlow {
        val url = "${config.serverUrl.trimEnd('/')}/${config.approvalTopic}/sse"
        val request = Request.Builder().url(url).build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                // ntfy SSE events wrap messages in JSON; extract the message field
                val message = extractMessageFromSseData(data)
                if (message != null) {
                    val result = processApprovalMessage(message, config.sharedSecret)
                    trySend(result)
                    if (result is ApprovalResult.Approved) {
                        eventSource.cancel()
                        channel.close()
                    }
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed: ${response?.code()}"))
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    private fun extractMessageFromSseData(data: String): String? {
        // ntfy SSE data is JSON: {"id":"...","event":"message","time":...,"message":"..."}
        return try {
            val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
            messageRegex.find(data)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun processApprovalMessage(message: String, secret: String): ApprovalResult {
        return if (HmacUtil.verifyApproval(message, secret)) {
            ApprovalResult.Approved
        } else {
            ApprovalResult.Denied("Invalid or expired approval message")
        }
    }
}
