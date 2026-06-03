package com.smartparking.config

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class MqttConfig(private val properties: MqttProperties) {

    @Bean
    fun mqttConnectOptions(): MqttConnectOptions {
        return MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 30
            if (properties.username.isNotBlank()) {
                userName = properties.username
                this.password = properties.password.toCharArray()
            }
        }
    }

    @Bean
    @Profile("!test")
    fun mqttClient(): MqttClient =
        MqttClient(properties.brokerUrl, properties.clientId, MemoryPersistence())


    @Bean
    @Profile("test")
    fun mqttClientMock(): MqttClient =
        MqttClient("tcp://localhost:1883", "test-client-${System.nanoTime()}", MemoryPersistence())
}
