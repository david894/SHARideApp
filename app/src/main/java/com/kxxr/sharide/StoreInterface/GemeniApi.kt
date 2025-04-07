package com.kxxr.sharide.StoreInterface

import com.kxxr.sharide.db.GeminiRequest
import com.kxxr.sharide.db.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Call

interface GeminiApi {
    @Headers("Content-Type: application/json")
    @POST("v1/models/gemini-1.5-flash:generateContent")
    fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}