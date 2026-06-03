package com.smartparking.mqtt

import com.smartparking.config.MqttProperties
import com.smartparking.entity.MqttLog
import com.smartparking.entity.ParkingStatus
import com.smartparking.repository.MqttLogRepository
import com.smartparking.repository.ParkingMeterRepository
import com.smartparking.service.ParkingMeterService
import com.smartparking.service.ParkingSessionService
import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.nio.charset.StandardCharsets

@Component
class MqttSubscriber(
    private val mqttClient: MqttClient,
    private val mqttConnectOptions: MqttConnectOptions,
    private val meterService: ParkingMeterService,
    private val sessionService: ParkingSessionService,
    private val meterRepository: ParkingMeterRepository,
    private val mqttLogRepository: MqttLogRepository,
    private val transactionTemplate: TransactionTemplate,
    private val properties: MqttProperties
) {

    private val log = LoggerFactory.getLogger(MqttSubscriber::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun connect() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                log.info("MQTT conectado: $serverURI (reconexão=$reconnect)")
                subscribeToTopics()
            }

            override fun connectionLost(cause: Throwable) {
                log.warn("MQTT desconectado: ${cause.message}")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload, StandardCharsets.UTF_8).trim()
                handleMessage(topic, payload)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        runCatching {
            mqttClient.connect(mqttConnectOptions)
        }.onFailure {
            log.error("Falha ao conectar ao broker MQTT: ${it.message}")
        }
    }

    private fun subscribeToTopics() {
        properties.topics.forEach { topic ->
            runCatching {
                mqttClient.subscribe(topic, 1)
                log.info("Inscrito no tópico MQTT: $topic")
            }.onFailure {
                log.error("Falha ao subscrever tópico $topic: ${it.message}")
            }
        }
    }

    // Wraps in a Spring transaction since this runs on the Paho thread
    private fun handleMessage(topic: String, payload: String) {
        log.info("MQTT recebido [$topic]: $payload")

        val meterCode = runCatching { topic.split("/")[1] }.getOrNull() ?: run {
            log.warn("Tópico inválido: $topic")
            return
        }

        transactionTemplate.execute {
            val mqttLog = MqttLog(topic = topic, payload = payload, meterCode = meterCode)

            when (payload.lowercase()) {
                "ocupado" -> {
                    meterService.updateStatus(meterCode, ParkingStatus.OCCUPIED)
                    mqttLog.processed = true
                }
                "livre" -> {
                    val meter = meterRepository.findByCode(meterCode).orElse(null)
                    if (meter != null) {
                        runCatching {
                            sessionService.endSessionByMqtt(meter.id)
                        }.onFailure {
                            log.warn("Nenhuma sessão ativa para encerrar no parquímetro $meterCode: ${it.message}")
                        }
                        meterService.updateStatus(meterCode, ParkingStatus.FREE)
                    }
                    mqttLog.processed = true
                }
                else -> {
                    log.warn("Payload desconhecido: $payload no tópico: $topic")
                    mqttLog.processed = false
                }
            }

            runCatching { mqttLogRepository.save(mqttLog) }
        }
    }

    fun publish(topic: String, payload: String) {
        runCatching {
            if (!mqttClient.isConnected) mqttClient.connect(mqttConnectOptions)
            val message = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8))
            message.qos = 1
            mqttClient.publish(topic, message)
            log.info("MQTT publicado [$topic]: $payload")
        }.onFailure {
            log.error("Falha ao publicar MQTT: ${it.message}")
        }
    }
}
