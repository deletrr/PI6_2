#pragma once
#include <Arduino.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include "config.h"
#include "hmac_util.h"

// Certificados: gere com infra/certs/gen_certs.sh e embuta aqui
extern const char* CA_CERT;
extern const char* CLIENT_CERT;
extern const char* CLIENT_KEY;

static WiFiClientSecure tlsClient;
static PubSubClient mqttClient(tlsClient);

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
    String msg;
    for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
    // Processar comandos recebidos (ex: forçar status, OTA trigger)
    if (msg.indexOf("\"cmd\":\"MANUTENCAO\"") >= 0) {
        // TODO: implementar lógica de manutenção
    }
}

void initMqtt() {
    tlsClient.setCACert(CA_CERT);
    tlsClient.setCertificate(CLIENT_CERT);
    tlsClient.setPrivateKey(CLIENT_KEY);
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(onMqttMessage);
    mqttClient.setBufferSize(512);
}

bool conectarMqtt() {
    int tentativas = 0;
    while (!mqttClient.connected() && tentativas < 5) {
        if (mqttClient.connect(DEVICE_ID)) {
            mqttClient.subscribe(TOPIC_CMD);
            return true;
        }
        delay(2000);
        tentativas++;
    }
    return mqttClient.connected();
}

void publicarTelemetria(const String& status, int bateria, const String& nonce) {
    if (!mqttClient.connected()) conectarMqtt();

    long ts = (long)(esp_timer_get_time() / 1000);
    String base = "{\"deviceId\":\"" + String(DEVICE_ID) +
                  "\",\"status\":\"" + status +
                  "\",\"battery\":" + String(bateria) +
                  ",\"nonce\":\"" + nonce +
                  "\",\"timestamp\":" + String(ts) + "}";

    String hmac = generateHmacSha256(base, HMAC_SECRET);
    String payload = base.substring(0, base.length() - 1) +
                     ",\"hmac\":\"" + hmac + "\"}";

    mqttClient.publish(TOPIC_TELEMETRIA, payload.c_str(), false);
}
