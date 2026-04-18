package com.liquidfuran.furan.data

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface NtfyApi {
    @POST("{topic}")
    @Headers("Content-Type: text/plain; charset=utf-8")
    suspend fun publish(
        @Path("topic") topic: String,
        @Body message: RequestBody
    ): Response<Unit>
}
