package com.smartparking.mqtt

import com.smartparking.config.MqttProperties
import com.smartparking.entity.MqttLog
import com.smartparking.entity.ParkingStatus
import com.smartparking.repository.MqttLogRepository
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
    private val mqttLogRepository: MqttLogRepository,
    private val transactionTemplate: TransactionTemplate,
    private val properties: MqttProperties
) {

    private val log = LoggerFactory.getLogger(MqttSubscriber::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun connect() {
        println(">>>> [MQTT HEARTBEAT] STARTUP INITIATED <<<<")
        println(">>>> Broker: ${properties.brokerUrl}")
        println(">>>> Topics: ${properties.topics}")

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                val msg = ">>>> [MQTT SUCCESS] CONECTADO AO BROKER: $serverURI"
                println(msg)
                log.info(msg)
                subscribeToTopics()
            }

            override fun connectionLost(cause: Throwable) {
                val msg = ">>>> [MQTT ERROR] CONEXAO PERDIDA: ${cause.message}"
                println(msg)
                log.error(msg)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload, StandardCharsets.UTF_8).trim()
                println(">>>> [MQTT MESSAGE] TOPICO: $topic | PAYLOAD: $payload")
                handleMessage(topic, payload)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        Thread {
            while (true) {
                if (!mqttClient.isConnected) {
                    try {
                        println(">>>> [MQTT] Tentando conectar em ${properties.brokerUrl}...")
                        mqttClient.connect(mqttConnectOptions)
                    } catch (e: Exception) {
                        println(">>>> [MQTT] Falha na conexao: ${e.message}")
                    }
                }
                Thread.sleep(10000)
            }
        }.start()
    }

    private fun subscribeToTopics() {
        val topicsToSubscribe = if (properties.topics.isEmpty()) listOf("parquimetro/+/status") else properties.topics
        topicsToSubscribe.forEach { topic ->
            try {
                mqttClient.subscribe(topic, 1)
                println(">>>> [MQTT] Inscrito com sucesso no topico: $topic")
            } catch (e: Exception) {
                println(">>>> [MQTT] Erro ao inscrever no topico $topic: ${e.message}")
            }
        }
    }

    private fun handleMessage(topic: String, payload: String) {
        try {
            val meterCode = topic.split("/").getOrNull(1) ?: run {
                log.warn("Tópico inválido (esperado parquimetro/CODIGO/status): $topic")
                return
            }

            println(">>>> [MQTT MESSAGE] INICIANDO PROCESSAMENTO: $meterCode | PAYLOAD: $payload")

            transactionTemplate.execute {
                val mqttLog = MqttLog(topic = topic, payload = payload, meterCode = meterCode)
                
                val status = when(payload.lowercase().trim()) {
                    "ocupado" -> ParkingStatus.OCCUPIED
                    "livre" -> ParkingStatus.FREE
                    else -> {
                        println(">>>> [MQTT WARNING] Payload desconhecido ignorado: $payload")
                        null
                    }
                }

                if (status != null) {
                    try {
                        val meter = meterService.updateStatus(meterCode, status)
                        println(">>>> [MQTT INFO] Parquímetro $meterCode atualizado para $status")
                        
                        if (status == ParkingStatus.FREE) {
                            runCatching {
                                sessionService.endSessionByMqtt(meter.id)
                            }.onFailure {
                                log.warn("Nenhuma sessão ativa para encerrar: ${it.message}")
                            }
                        }
                        mqttLog.processed = true
                    } catch (e: Exception) {
                        println(">>>> [MQTT ERROR] Falha ao atualizar status no banco: ${e.message}")
                        mqttLog.processed = false
                    }
                } else {
                    mqttLog.processed = false
                }

                mqttLogRepository.save(mqttLog)
                println(">>>> [MQTT SUCCESS] Log salvo no banco.")
            }
        } catch (e: Exception) {
            println(">>>> [MQTT CRITICAL ERROR] Falha no handleMessage: ${e.message}")
            e.printStackTrace()
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
