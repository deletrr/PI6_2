package com.parquimetro.network

import com.parquimetro.dto.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class VagaRepository(private val client: ApiClient) {
    suspend fun listar(): List<VagaResponse> =
        client.http.get("/api/vagas") { client.run { withAuth() } }.body()

    suspend fun buscar(id: String): VagaResponse =
        client.http.get("/api/vagas/$id") { client.run { withAuth() } }.body()
}

class PagamentoRepository(private val client: ApiClient) {
    suspend fun pagar(req: PagamentoRequest): PagamentoResponse =
        client.http.post("/api/pagamentos") {
            client.run { withAuth() }
            setBody(req)
        }.body()
}

class InfracaoRepository(private val client: ApiClient) {
    suspend fun registrar(req: InfracaoRequest) {
        client.http.post("/api/infracoes") {
            client.run { withAuth() }
            setBody(req)
        }
    }
}

class AuthRepository(private val client: ApiClient) {
    suspend fun login(username: String, password: String): AuthResponse =
        client.http.post("/api/auth/login") {
            setBody(AuthRequest(username, password))
        }.body()
}
