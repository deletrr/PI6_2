package com.parquimetro.feature.fiscal

data class FotoProcessada(val bytes: ByteArray, val sha256: String)

expect class FotoProcessor {
    fun processarComMarcaDagua(
        imagemBytes: ByteArray,
        nomeFiscal: String,
        timestamp: Long
    ): FotoProcessada
}
