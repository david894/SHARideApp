package com.kxxr.sharide.db


data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>,
    val role: String
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)