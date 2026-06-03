package com.smartparking.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mqtt")
class MqttProperties {
    var brokerUrl: String = ""
    var clientId: String = ""
    var username: String = ""
    var password: String = ""
    var topics: List<String> = mutableListOf()
}
