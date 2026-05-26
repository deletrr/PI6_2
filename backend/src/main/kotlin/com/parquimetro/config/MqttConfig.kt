package com.parquimetro.config

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@Configuration
class MqttConfig(
    @Value("\${mqtt.broker-url}") private val brokerUrl: String,
    @Value("\${mqtt.client-id}") private val clientId: String,
    @Value("\${mqtt.ca-cert-path}") private val caCertPath: String
) {
    @Bean
    fun mqttClient(): MqttClient {
        val client = MqttClient(brokerUrl, clientId)
        client.connect(connectOptions())
        return client
    }

    private fun connectOptions(): MqttConnectOptions =
        MqttConnectOptions().apply {
            isCleanSession = false
            connectionTimeout = 30
            keepAliveInterval = 60
            socketFactory = buildSslContext().socketFactory
        }

    private fun buildSslContext(): SSLContext {
        val cf = CertificateFactory.getInstance("X.509")
        val ca = FileInputStream(caCertPath).use { cf.generateCertificate(it) }
        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", ca)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(ks)
        }
        return SSLContext.getInstance("TLSv1.3").apply {
            init(null, tmf.trustManagers, null)
        }
    }
}
