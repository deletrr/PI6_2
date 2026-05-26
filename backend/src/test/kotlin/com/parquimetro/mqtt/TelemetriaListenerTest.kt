package com.parquimetro.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.parquimetro.domain.entity.*
import com.parquimetro.domain.repository.HardwareESP32Repository
import com.parquimetro.domain.repository.VagaRepository
import com.parquimetro.security.HmacUtils
import org.eclipse.paho.client.mqttv3.MqttClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TelemetriaListenerTest {

    @Mock lateinit var mqttClient: MqttClient
    @Mock lateinit var hardwareRepo: HardwareESP32Repository
    @Mock lateinit var vagaRepo: VagaRepository

    private lateinit var listener: TelemetriaListener
    private val gf = GeometryFactory(PrecisionModel(), 4326)
    private val secret = "test-hmac-secret"

    @BeforeEach
    fun setup() {
        listener = TelemetriaListener(mqttClient, hardwareRepo, vagaRepo, ObjectMapper())
    }

    private fun buildVaga() = Vaga(
        id = UUID.randomUUID(),
        codigo = "A-01",
        localizacao = gf.createPoint(Coordinate(-46.6, -23.5)),
        status = VagaStatus.LIVRE
    )

    private fun buildHardware(vaga: Vaga) = HardwareESP32(
        vaga = vaga,
        deviceId = "ESP32-VAG-001",
        hmacSecret = secret,
        firmwareVersion = "1.0.0",
        batteryLevel = 100,
        lastSeen = Instant.now()
    )

    @Test
    fun `handle persiste vaga quando HMAC valido`() {
        val vaga = buildVaga()
        val hw = buildHardware(vaga)
        val base = """{"deviceId":"ESP32-VAG-001","status":"OCUPADA","battery":87,"timestamp":1000}"""
        val hmac = HmacUtils.compute(base.trimStart('{').dropLast(1) + "}", secret)
        val raw = base.dropLast(1) + ""","hmac":"$hmac"}"""

        `when`(hardwareRepo.findByDeviceId("ESP32-VAG-001")).thenReturn(hw)
        `when`(hardwareRepo.save(hw)).thenReturn(hw)
        `when`(vagaRepo.save(vaga)).thenReturn(vaga)

        listener.subscribe()
        verify(mqttClient).subscribe(eq("parquimetro/+/telemetria"), any())
    }

    @Test
    fun `handle rejeita payload com HMAC invalido`() {
        val vaga = buildVaga()
        val hw = buildHardware(vaga)
        val raw = """{"deviceId":"ESP32-VAG-001","status":"OCUPADA","battery":87,"timestamp":1000,"hmac":"invalido"}"""

        `when`(hardwareRepo.findByDeviceId("ESP32-VAG-001")).thenReturn(hw)

        verify(vagaRepo, never()).save(any())
    }
}
