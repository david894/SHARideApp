package com.kxxr.sharide.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kxxr.sharide.StoreInterface.GeminiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response


class ChatBotViewModel : ViewModel() {

    private val apiKey = "AIzaSyCQ-zfS1V7tSgOuv_znYHeOItHi_VmEfEM" // 🔑 Replace with your actual Gemini API key

    private val _response = MutableStateFlow("Gemini is ready to chat!")
    val response = _response.asStateFlow()

    private val geminiApi: GeminiApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        geminiApi = retrofit.create(GeminiApi::class.java)
    }

    fun generateContent(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt)),
                        role = "user"
                    )
                )
            )

            try {
                val response: Response<GeminiResponse> =
                    geminiApi.generateContent(apiKey, request).execute()

                if (response.isSuccessful) {
                    val body = response.body()
                    val reply = body?.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text

                    _response.value = reply ?: "Gemini gave no response."
                } else {
                    _response.value = "Error: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _response.value = "Error: ${e.localizedMessage}"
            }
        }
    }
}