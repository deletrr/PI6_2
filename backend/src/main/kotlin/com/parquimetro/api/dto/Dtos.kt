package com.parquimetro.api.dto

import com.parquimetro.domain.entity.VagaStatus
import java.util.UUID

data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val token: String)

data class VagaResponse(
    val id: UUID,
    val codigo: String,
    val lat: Double,
    val lng: Double,
    val status: VagaStatus,
    val battery: Int?
)

data class VagaCreateRequest(
    val codigo: String,
    val lat: Double,
    val lng: Double
)

data class InfracaoRequest(
    val vagaId: UUID,
    val fiscalId: String,
    val fotoHash: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)

data class PagamentoRequest(
    val vagaId: UUID,
    val motoristaCpf: String,
    val placa: String,
    val duracaoMinutos: Int
)

data class PagamentoResponse(
    val id: UUID,
    val vagaId: UUID,
    val expiraEm: Long,
    val valor: Double
)
