package com.parquimetro.android

import android.app.Application
import android.content.Context
import com.parquimetro.feature.fiscal.FiscalViewModel
import com.parquimetro.feature.fiscal.FotoProcessor
import com.parquimetro.feature.motorista.MotoristaViewModel
import com.parquimetro.network.ApiClient
import com.parquimetro.network.InfracaoRepository
import com.parquimetro.network.PagamentoRepository
import com.parquimetro.network.VagaRepository
import com.parquimetro.security.LocationValidator

class ParquimetroApp : Application() {

    private var tokenInterno: String? = null

    val apiClient by lazy {
        ApiClient(tokenProvider = { tokenInterno })
    }

    val vagaRepository by lazy { VagaRepository(apiClient) }
    val pagamentoRepository by lazy { PagamentoRepository(apiClient) }
    val infracaoRepository by lazy { InfracaoRepository(apiClient) }

    fun locationValidator(ctx: Context) = LocationValidator(ctx)

    fun motoristaViewModel() = MotoristaViewModel(
        vagaRepo = vagaRepository,
        pagamentoRepo = pagamentoRepository,
        locationValidator = locationValidator(this)
    )

    fun fiscalViewModel(fiscalId: String) = FiscalViewModel(
        infracaoRepo = infracaoRepository,
        locationValidator = locationValidator(this),
        fotoProcessor = FotoProcessor(),
        fiscalId = fiscalId
    )

    fun setToken(token: String) { tokenInterno = token }
    fun clearToken() { tokenInterno = null }
}

val Context.app get() = applicationContext as ParquimetroApp
