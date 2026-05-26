package com.parquimetro.security

import platform.CoreLocation.*

actual class LocationValidator {
    actual fun validarLocalizacao(): Coordenada {
        val manager = CLLocationManager()
        val location = manager.location
            ?: error("Localização indisponível")

        // iOS 15+: sourceInformation indica simulação
        if (location.responds(platform.Foundation.NSSelectorFromString("sourceInformation"))) {
            val info = location.performSelector(
                platform.Foundation.NSSelectorFromString("sourceInformation")
            )
            // isSimulatedBySoftware ou isProducedByAccessory indicam fake GPS
            val simulated = info?.let {
                val sel = platform.Foundation.NSSelectorFromString("isSimulatedBySoftware")
                it.responds(sel) && it.performSelector(sel) != null
            } ?: false
            if (simulated) throw MockLocationException()
        }

        return Coordenada(
            lat = location.coordinate.useContents { latitude },
            lng = location.coordinate.useContents { longitude }
        )
    }
}
