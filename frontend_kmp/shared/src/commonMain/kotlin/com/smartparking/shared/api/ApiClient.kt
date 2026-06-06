package com.smartparking.shared.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// Base URL — change for physical device or web deployment
const val BASE_URL = "http://10.0.2.2:8080"

// Token storage — platform-specific implementations override this
expect object TokenStorage {
    fun getToken(): String?
    fun setToken(token: String?)
    fun getUserJson(): String?
    fun setUserJson(json: String?)
    fun clear()
}

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
            defaultRequest {
                url(BASE_URL)
                contentType(ContentType.Application.Json)
                val token = TokenStorage.getToken()
                if (token != null) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}

// ── Result wrapper ────────────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

suspend inline fun <reified T> HttpResponse.toResult(): ApiResult<T> {
    return if (status.isSuccess()) {
        try {
            ApiResult.Success(body<T>())
        } catch (e: Exception) {
            ApiResult.Error("Erro ao processar resposta: ${e.message}")
        }
    } else {
        val errorMessage = try {
            val err = body<com.smartparking.shared.model.ErrorResponse>()
            err.message
        } catch (e: Exception) {
            "Erro ${status.value}: ${status.description}"
        }
        ApiResult.Error(errorMessage, status.value)
    }
}
