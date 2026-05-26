package com.parquimetro.security

data class Coordenada(val lat: Double, val lng: Double)

class MockLocationException : Exception("Localização simulada detectada")

expect class LocationValidator {
    fun validarLocalizacao(): Coordenada
}
