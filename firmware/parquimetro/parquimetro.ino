#include <Arduino.h>
#include <WiFi.h>
#include <esp_sleep.h>
#include "config.h"
#include "sensor.h"
#include "display.h"
#include "mqtt_handler.h"

// Certificados mTLS
// Opção 1: execute firmware/flash_device.sh - ele gera certs_embedded.h automaticamente
// Opção 2: cole os conteúdos dos .crt/.key manualmente abaixo
#if __has_include("certs_embedded.h")
#include "certs_embedded.h"
#else
const char* CA_CERT = R"(
-----BEGIN CERTIFICATE-----
COLE_AQUI_O_CONTEUDO_DE_ca.crt
-----END CERTIFICATE-----
)";
const char* CLIENT_CERT = R"(
-----BEGIN CERTIFICATE-----
COLE_AQUI_O_CONTEUDO_DE_ESP32-VAG-001.crt
-----END CERTIFICATE-----
)";
const char* CLIENT_KEY = R"(
-----BEGIN RSA PRIVATE KEY-----
COLE_AQUI_O_CONTEUDO_DE_ESP32-VAG-001.key
-----END RSA PRIVATE KEY-----
)";
#endif

RTC_DATA_ATTR static int bateriaPct = 100;
RTC_DATA_ATTR static EstadoVaga ultimoEstado = EstadoVaga::LIVRE;

int lerBateria() {
    int raw = analogRead(35);
    return map(raw, 1500, 3100, 0, 100);
}

void conectarWifi() {
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    int t = 0;
    while (WiFi.status() != WL_CONNECTED && t < 20) {
        delay(500);
        t++;
    }
}

void entrarDeepSleep() {
    mqttClient.disconnect();
    WiFi.disconnect(true);
    esp_sleep_enable_ext0_wakeup(PIN_WAKEUP, 0);
    esp_sleep_enable_timer_wakeup(DEEP_SLEEP_US);
    esp_deep_sleep_start();
}

void setup() {
    Serial.begin(115200);
    initSensores();
    initDisplay();

    EstadoVaga estado = lerEstadoVaga();
    bateriaPct = lerBateria();
    String statusStr = (estado == EstadoVaga::OCUPADA) ? "OCUPADA" : "LIVRE";

    atualizarDisplay(statusStr, bateriaPct);

    if (estado != ultimoEstado || bateriaPct < 20) {
        conectarWifi();
        if (WiFi.status() == WL_CONNECTED) {
            initMqtt();
            if (conectarMqtt()) {
                publicarTelemetria(statusStr, bateriaPct, getNonce());
                delay(500);
                mqttClient.loop();
            }
        }
        ultimoEstado = estado;
    }

    entrarDeepSleep();
}

void loop() {
    // Não é utilizado - o ESP32 usa deep sleep
}
