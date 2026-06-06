package com.smartparking.shared.api

import com.smartparking.shared.model.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object AuthApi {
    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> =
        ApiClient.httpClient.post("/api/auth/login") { setBody(request) }.toResult()

    suspend fun register(request: RegisterRequest): ApiResult<AuthResponse> =
        ApiClient.httpClient.post("/api/auth/register") { setBody(request) }.toResult()
}

object UserApi {
    suspend fun getMe(): ApiResult<UserModel> =
        ApiClient.httpClient.get("/api/users/me").toResult()

    suspend fun updateMe(request: UpdateUserRequest): ApiResult<UserModel> =
        ApiClient.httpClient.put("/api/users/me") { setBody(request) }.toResult()

    // Admin
    suspend fun listUsers(search: String? = null, page: Int = 0, size: Int = 20): ApiResult<PageResponse<UserModel>> =
        ApiClient.httpClient.get("/api/admin/users") {
            search?.let { parameter("search", it) }
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    suspend fun getUser(id: String): ApiResult<UserModel> =
        ApiClient.httpClient.get("/api/admin/users/$id").toResult()

    suspend fun updateUser(id: String, request: AdminUpdateUserRequest): ApiResult<UserModel> =
        ApiClient.httpClient.put("/api/admin/users/$id") { setBody(request) }.toResult()

    suspend fun deleteUser(id: String): ApiResult<Unit> =
        ApiClient.httpClient.delete("/api/admin/users/$id").toResult()
}

object ParkingMeterApi {
    suspend fun getMapMeters(): ApiResult<List<ParkingMeterModel>> =
        ApiClient.httpClient.get("/api/parking-meters/map").toResult()

    suspend fun getByCode(code: String): ApiResult<ParkingMeterModel> =
        ApiClient.httpClient.get("/api/parking-meters/$code/by-code").toResult()

    suspend fun getById(id: String): ApiResult<ParkingMeterModel> =
        ApiClient.httpClient.get("/api/parking-meters/$id").toResult()

    // Admin
    suspend fun listAll(search: String? = null, page: Int = 0, size: Int = 20): ApiResult<PageResponse<ParkingMeterModel>> =
        ApiClient.httpClient.get("/api/parking-meters") {
            search?.let { parameter("search", it) }
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    suspend fun listOrphans(): ApiResult<List<ParkingMeterModel>> =
        ApiClient.httpClient.get("/api/parking-meters/orphans").toResult()

    suspend fun create(request: CreateParkingMeterRequest): ApiResult<ParkingMeterModel> =
        ApiClient.httpClient.post("/api/parking-meters") { setBody(request) }.toResult()

    suspend fun update(id: String, request: UpdateParkingMeterRequest): ApiResult<ParkingMeterModel> =
        ApiClient.httpClient.put("/api/parking-meters/$id") { setBody(request) }.toResult()

    suspend fun delete(id: String): ApiResult<Unit> =
        ApiClient.httpClient.delete("/api/parking-meters/$id").toResult()
}

object SessionApi {
    suspend fun startSession(request: StartSessionRequest): ApiResult<SessionModel> =
        ApiClient.httpClient.post("/api/sessions/start") { setBody(request) }.toResult()

    suspend fun endSession(id: String): ApiResult<SessionModel> =
        ApiClient.httpClient.post("/api/sessions/$id/end").toResult()

    suspend fun getActiveSession(): ApiResult<SessionModel?> =
        try {
            val response = ApiClient.httpClient.get("/api/sessions/active")
            if (response.status.value == 204) ApiResult.Success(null)
            else response.toResult()
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Erro desconhecido")
        }

    suspend fun getHistory(page: Int = 0, size: Int = 20): ApiResult<PageResponse<SessionModel>> =
        ApiClient.httpClient.get("/api/sessions/history") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    // Admin
    suspend fun getAllSessions(page: Int = 0, size: Int = 20): ApiResult<PageResponse<SessionModel>> =
        ApiClient.httpClient.get("/api/sessions") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()
}

object WalletApi {
    suspend fun getBalance(): ApiResult<BalanceResponse> =
        ApiClient.httpClient.get("/api/wallet/balance").toResult()

    suspend fun recharge(request: RechargeRequest): ApiResult<RechargeResponse> =
        ApiClient.httpClient.post("/api/wallet/recharge") { setBody(request) }.toResult()

    suspend fun getExtract(page: Int = 0, size: Int = 20): ApiResult<PageResponse<WalletTransactionModel>> =
        ApiClient.httpClient.get("/api/wallet/extract") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    // Admin
    suspend fun getAdminExtract(page: Int = 0, size: Int = 20): ApiResult<PageResponse<WalletTransactionModel>> =
        ApiClient.httpClient.get("/api/admin/wallet/extract") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()
}

object FineApi {
    suspend fun getMyFines(page: Int = 0, size: Int = 20): ApiResult<PageResponse<FineModel>> =
        ApiClient.httpClient.get("/api/fines/mine") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    // Admin
    suspend fun getAllFines(page: Int = 0, size: Int = 20): ApiResult<PageResponse<FineModel>> =
        ApiClient.httpClient.get("/api/fines") {
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    suspend fun updateFine(id: String, status: String): ApiResult<FineModel> =
        ApiClient.httpClient.put("/api/fines/$id") {
            setBody(mapOf("status" to status))
        }.toResult()

    suspend fun deleteFine(id: String): ApiResult<Unit> =
        ApiClient.httpClient.delete("/api/fines/$id").toResult()
}

object SupportApi {
    suspend fun createTicket(request: CreateSupportTicketRequest): ApiResult<SupportTicketModel> =
        ApiClient.httpClient.post("/api/support") { setBody(request) }.toResult()

    suspend fun getMyTickets(): ApiResult<List<SupportTicketModel>> =
        ApiClient.httpClient.get("/api/support/mine").toResult()

    // Admin
    suspend fun getAllTickets(resolved: Boolean? = null, page: Int = 0, size: Int = 20): ApiResult<PageResponse<SupportTicketModel>> =
        ApiClient.httpClient.get("/api/support") {
            resolved?.let { parameter("resolved", it) }
            parameter("page", page)
            parameter("size", size)
        }.toResult()

    suspend fun respond(id: String, response: String): ApiResult<SupportTicketModel> =
        ApiClient.httpClient.post("/api/support/$id/respond") {
            setBody(mapOf("response" to response))
        }.toResult()
}

object DashboardApi {
    suspend fun getDashboard(): ApiResult<DashboardModel> =
        ApiClient.httpClient.get("/api/admin/dashboard").toResult()
}

@kotlinx.serialization.Serializable
data class ViaCepResponse(
    val cep: String? = null,
    val logradouro: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val erro: Boolean? = null
)

object ExternalApi {
    suspend fun searchCep(cep: String): ApiResult<ViaCepResponse> = try {
        val cleanCep = cep.replace("-", "").replace(".", "").trim()
        if (cleanCep.length != 8) ApiResult.Error("CEP inválido")
        else {
            val response = ApiClient.httpClient.get("https://viacep.com.br/ws/$cleanCep/json/")
            val body = response.toResult<ViaCepResponse>()
            if (body is ApiResult.Success && body.data.erro == true) ApiResult.Error("CEP não encontrado")
            else body
        }
    } catch (e: Exception) {
        ApiResult.Error("Falha ao buscar CEP: ${e.message}")
    }
}
