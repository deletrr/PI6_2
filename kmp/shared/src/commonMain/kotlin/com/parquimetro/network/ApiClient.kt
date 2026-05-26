package com.parquimetro.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://api.parquimetro.com"

class ApiClient(private val tokenProvider: () -> String?) {

    val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        install(DefaultRequest) {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
        }
    }

    fun HttpRequestBuilder.withAuth() {
        tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}
