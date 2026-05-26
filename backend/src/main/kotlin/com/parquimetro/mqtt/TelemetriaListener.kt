package com.parquimetro.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.parquimetro.domain.entity.VagaStatus
import com.parquimetro.domain.repository.HardwareESP32Repository
import com.parquimetro.domain.repository.VagaRepository
import com.parquimetro.security.HmacUtils
import jakarta.annotation.PostConstruct
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

data class TelemetriaPayload(
    val deviceId: String,
    val status: String,
    val battery: Int,
    val timestamp: Long,
    val hmac: String
)

@Component
class TelemetriaListener(
    private val mqttClient: MqttClient,
    private val hardwareRepo: HardwareESP32Repository,
    private val vagaRepo: VagaRepository,
    private val mapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun subscribe() {
        mqttClient.subscribe("parquimetro/+/telemetria", IMqttMessageListener { topic, message ->
            handle(topic, String(message.payload))
        })
    }

    private fun handle(topic: String, raw: String) {
        val payload = runCatching { mapper.readValue(raw, TelemetriaPayload::class.java) }
            .getOrElse { log.warn("Malformed payload on $topic"); return }

        val hardware = hardwareRepo.findByDeviceId(payload.deviceId)
            ?: run { log.warn("Unknown device: ${payload.deviceId}"); return }

        val signedPart = raw.substringBeforeLast(",\"hmac\"").trimStart('{') + "}"
        if (!HmacUtils.verify(signedPart, hardware.hmacSecret, payload.hmac)) {
            log.warn("HMAC mismatch for device ${payload.deviceId}")
            return
        }

        hardware.batteryLevel = payload.battery
        hardware.lastSeen = Instant.now()
        hardwareRepo.save(hardware)

        val vaga = hardware.vaga
        vaga.status = runCatching { VagaStatus.valueOf(payload.status) }
            .getOrElse { log.warn("Unknown status: ${payload.status}"); return }
        vagaRepo.save(vaga)

        log.info("Vaga ${vaga.codigo} -> ${vaga.status}")
    }
}
