package com.kxxr.sharide.db

import android.content.Context
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
    private var busRouteData: String? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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
    // Call this in Composable to load the route data
    fun initialize(context: Context) {
        if (busRouteData == null) {
            busRouteData = loadRoutesFromAssets(context)
        }
    }
    // Load data.json from assets
    private fun loadRoutesFromAssets(context: Context): String {
        return context.assets.open("data.json")
            .bufferedReader()
            .use { it.readText() }
    }
    fun generateContent(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val isBusQuestion = listOf(
                "route", "stop", "bus", "arrival", "Wangsa", "Melati", "PV"
            ).any { prompt.contains(it, ignoreCase = true) }

            val fullPrompt = if (isBusQuestion && busRouteData != null) {
                """
            You are a helpful university campus bus assistant.
            Below is the full bus route data in JSON format:

            $busRouteData

            Based on this, answer the following question:
            $prompt
            """.trimIndent()
            } else {
                prompt // Let Gemini use its general knowledge
            }

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = fullPrompt)),
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
            } finally {
                _isLoading.value = false
            }
        }
    }
}