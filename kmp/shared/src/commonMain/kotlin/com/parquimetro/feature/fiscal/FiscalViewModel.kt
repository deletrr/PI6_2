package com.parquimetro.feature.fiscal

import com.parquimetro.dto.InfracaoRequest
import com.parquimetro.network.InfracaoRepository
import com.parquimetro.security.LocationValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FiscalViewModel(
    private val infracaoRepo: InfracaoRepository,
    private val locationValidator: LocationValidator,
    private val fotoProcessor: FotoProcessor,
    private val fiscalId: String
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _estado = MutableStateFlow<String?>(null)
    val estado: StateFlow<String?> = _estado

    fun registrarInfracao(vagaId: String, imagemBytes: ByteArray) = scope.launch {
        runCatching {
            val coord = locationValidator.validarLocalizacao()
            val foto = fotoProcessor.processarComMarcaDagua(imagemBytes, fiscalId, System.currentTimeMillis())
            infracaoRepo.registrar(
                InfracaoRequest(
                    vagaId = vagaId,
                    fiscalId = fiscalId,
                    fotoHash = foto.sha256,
                    lat = coord.lat,
                    lng = coord.lng,
                    timestamp = System.currentTimeMillis()
                )
            )
            foto
        }.onSuccess {
            _estado.value = "Infração registrada. Hash: ${it.sha256.take(12)}..."
        }.onFailure {
            _estado.value = "Erro: ${it.message}"
        }
    }
}
