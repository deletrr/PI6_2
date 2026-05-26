package com.parquimetro.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

@Serializable
data class VagaResponse(
    val id: String,
    val codigo: String,
    val lat: Double,
    val lng: Double,
    val status: String,
    val battery: Int? = null
)

@Serializable
data class PagamentoRequest(
    val vagaId: String,
    val motoristaCpf: String,
    val placa: String,
    val duracaoMinutos: Int
)

@Serializable
data class PagamentoResponse(
    val id: String,
    val vagaId: String,
    val expiraEm: Long,
    val valor: Double
)

@Serializable
data class InfracaoRequest(
    val vagaId: String,
    val fiscalId: String,
    val fotoHash: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
