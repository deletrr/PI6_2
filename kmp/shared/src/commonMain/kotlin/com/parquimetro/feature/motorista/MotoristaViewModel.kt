package com.parquimetro.feature.motorista

import com.parquimetro.dto.PagamentoRequest
import com.parquimetro.dto.PagamentoResponse
import com.parquimetro.dto.VagaResponse
import com.parquimetro.network.PagamentoRepository
import com.parquimetro.network.VagaRepository
import com.parquimetro.security.LocationValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MotoristaViewModel(
    private val vagaRepo: VagaRepository,
    private val pagamentoRepo: PagamentoRepository,
    private val locationValidator: LocationValidator
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _vagas = MutableStateFlow<List<VagaResponse>>(emptyList())
    val vagas: StateFlow<List<VagaResponse>> = _vagas

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun carregarVagas() = scope.launch {
        runCatching { _vagas.value = vagaRepo.listar() }
            .onFailure { _erro.value = it.message }
    }

    fun pagar(vagaId: String, cpf: String, placa: String, minutos: Int, onSuccess: (PagamentoResponse) -> Unit) =
        scope.launch {
            runCatching {
                locationValidator.validarLocalizacao()
                pagamentoRepo.pagar(PagamentoRequest(vagaId, cpf, placa, minutos))
            }.onSuccess(onSuccess).onFailure { _erro.value = it.message }
        }
}
