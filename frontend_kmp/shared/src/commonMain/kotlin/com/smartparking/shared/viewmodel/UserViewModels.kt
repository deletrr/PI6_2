package com.smartparking.shared.viewmodel

import com.smartparking.shared.api.*
import com.smartparking.shared.model.*
import com.smartparking.shared.repository.AppSession
import com.smartparking.shared.util.format
import com.smartparking.shared.util.GeocodingUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

// ── HomeViewModel ─────────────────────────────────────────────────────────────

data class HomeUiState(
    val meters: List<ParkingMeterModel> = emptyList(),
    val filteredMeters: List<ParkingMeterModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchAddress: String = ""
)

class HomeViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun loadMapMeters() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = ParkingMeterApi.getMapMeters()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        meters = result.data, 
                        filteredMeters = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun filterByAddress(address: String) {
        if (address.isBlank()) {
            _state.value = _state.value.copy(filteredMeters = _state.value.meters, searchAddress = "")
            return
        }

        scope.launch {
            _state.value = _state.value.copy(isLoading = true, searchAddress = address)
            when (val geoResult = GeocodingUtil.getCoordinates(address)) {
                is ApiResult.Success -> {
                    val (lat, lon) = geoResult.data
                    val filtered = _state.value.meters.filter { meter ->
                        if (meter.latitude != null && meter.longitude != null) {
                            calculateDistance(lat, lon, meter.latitude, meter.longitude) <= 2.0 // 2km de raio
                        } else false
                    }.sortedBy { 
                        calculateDistance(lat, lon, it.latitude!!, it.longitude!!)
                    }
                    _state.value = _state.value.copy(filteredMeters = filtered, isLoading = false)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = geoResult.message)
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Raio da Terra em km
        val dLat = (lat2 - lat1) * PI / 180
        val dLon = (lon2 - lon1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180) * cos(lat2 * PI / 180) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

// ── SessionViewModel ──────────────────────────────────────────────────────────

data class SessionUiState(
    val activeSession: SessionModel? = null,
    val history: List<SessionModel> = emptyList(),
    val isLoading: Boolean = false,
    val isStarting: Boolean = false,
    val isEnding: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SessionViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state

    private var pollingJob: Job? = null

    fun loadActiveSession() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = SessionApi.getActiveSession()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    activeSession = result.data, isLoading = false
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun startSession(meterCode: String, vehiclePlate: String, onSuccess: () -> Unit) {
        scope.launch {
            _state.value = _state.value.copy(isStarting = true, error = null)
            when (val result = SessionApi.startSession(StartSessionRequest(meterCode, vehiclePlate))) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        activeSession = result.data,
                        isStarting = false,
                        successMessage = "Sessão iniciada com sucesso!"
                    )
                    startPolling()
                    onSuccess()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isStarting = false, error = result.message
                )
            }
        }
    }

    fun endSession(sessionId: String, onSuccess: () -> Unit) {
        scope.launch {
            _state.value = _state.value.copy(isEnding = true, error = null)
            when (val result = SessionApi.endSession(sessionId)) {
                is ApiResult.Success -> {
                    stopPolling()
                    _state.value = _state.value.copy(
                        activeSession = null,
                        isEnding = false,
                        successMessage = "Sessão encerrada. Valor: R$ ${"%.2f".format(result.data.amountCharged)}"
                    )
                    onSuccess()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isEnding = false, error = result.message
                )
            }
        }
    }

    fun loadHistory() {
        scope.launch {
            when (val result = SessionApi.getHistory()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    history = result.data.content
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    error = result.message
                )
            }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(10_000)
                when (val result = SessionApi.getActiveSession()) {
                    is ApiResult.Success -> _state.value = _state.value.copy(
                        activeSession = result.data
                    )
                    else -> {}
                }
            }
        }
    }

    fun stopPolling() { pollingJob?.cancel() }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}

// ── WalletViewModel ───────────────────────────────────────────────────────────

data class WalletUiState(
    val balance: Double = 0.0,
    val transactions: List<WalletTransactionModel> = emptyList(),
    val rechargeResponse: RechargeResponse? = null,
    val isLoading: Boolean = false,
    val isRecharging: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class WalletViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state

    fun loadWallet() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val balResult = WalletApi.getBalance()
            val txResult = WalletApi.getExtract()
            val balance = if (balResult is ApiResult.Success) balResult.data.balance
                          else _state.value.balance
            val transactions = if (txResult is ApiResult.Success) txResult.data.content
                               else _state.value.transactions
            _state.value = _state.value.copy(
                balance = balance, transactions = transactions, isLoading = false
            )
            // Update session balance
            AppSession.currentUser.value?.let { user ->
                AppSession.updateUser(user.copy(balance = balance))
            }
        }
    }

    fun recharge(request: RechargeRequest, onSuccess: () -> Unit) {
        scope.launch {
            _state.value = _state.value.copy(isRecharging = true, error = null)
            when (val result = WalletApi.recharge(request)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isRecharging = false,
                        balance = result.data.newBalance,
                        rechargeResponse = result.data,
                        successMessage = "Recarga de R$ ${"%.2f".format(result.data.amount)} realizada!"
                    )
                    loadWallet()
                    onSuccess()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isRecharging = false, error = result.message
                )
            }
        }
    }

    fun loadAdminExtract() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = WalletApi.getAdminExtract()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    transactions = result.data.content, isLoading = false
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null, rechargeResponse = null)
    }
}

// ── FinesViewModel ────────────────────────────────────────────────────────────

data class FinesUiState(
    val fines: List<FineModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FinesViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(FinesUiState())
    val state: StateFlow<FinesUiState> = _state

    fun loadFines() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = FineApi.getMyFines()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    fines = result.data.content, isLoading = false
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }
}

// ── SupportViewModel ──────────────────────────────────────────────────────────

data class SupportUiState(
    val tickets: List<SupportTicketModel> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SupportViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(SupportUiState())
    val state: StateFlow<SupportUiState> = _state

    fun loadTickets() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = SupportApi.getMyTickets()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    tickets = result.data, isLoading = false
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun sendTicket(subject: String, message: String, onSuccess: () -> Unit) {
        if (subject.isBlank() || message.isBlank()) {
            _state.value = _state.value.copy(error = "Preencha assunto e mensagem.")
            return
        }
        scope.launch {
            _state.value = _state.value.copy(isSending = true, error = null)
            when (val result = SupportApi.createTicket(
                CreateSupportTicketRequest(subject.trim(), message.trim())
            )) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isSending = false, successMessage = "Chamado aberto com sucesso!"
                    )
                    loadTickets()
                    onSuccess()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isSending = false, error = result.message
                )
            }
        }
    }

    fun clearMessages() { _state.value = _state.value.copy(error = null, successMessage = null) }
}
