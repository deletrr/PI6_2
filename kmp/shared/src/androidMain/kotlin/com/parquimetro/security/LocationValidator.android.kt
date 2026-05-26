package com.parquimetro.security

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build

actual class LocationValidator(private val context: Context) {
    actual fun validarLocalizacao(): Coordenada {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        val location: Location = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: error("Localização indisponível")

        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

        if (isMock) throw MockLocationException()

        return Coordenada(location.latitude, location.longitude)
    }
}
