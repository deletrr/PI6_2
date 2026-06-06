#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "config.h"

// ── Estado da vaga ────────────────────────────────────────────────────────────
enum class SpotState { UNKNOWN, OCCUPIED, FREE };

static SpotState currentState   = SpotState::UNKNOWN;
static SpotState lastPublished  = SpotState::UNKNOWN;

// Marca o momento em que a vaga ficou continuamente vazia
static unsigned long freeStartMs = 0;
static bool         freeTimerRunning = false;

// ── Timestamps não-bloqueantes ────────────────────────────────────────────────
static unsigned long lastSensorReadMs  = 0;
static unsigned long lastConnCheckMs   = 0;
static unsigned long lastPublishMs     = 0;
static unsigned long lastOledUpdateMs  = 0;
static unsigned long lastHeartbeatMs   = 0; // Novo timer para envio periódico

// ── Clientes ─────────────────────────────────────────────────────────────────
WiFiClient   wifiClient;
PubSubClient mqttClient(wifiClient);

// ── OLED ─────────────────────────────────────────────────────────────────────
Adafruit_SSD1306 oled(OLED_WIDTH, OLED_HEIGHT, &Wire, -1);
static bool oledOk = false;

// ── Tópico MQTT ──────────────────────────────────────────────────────────────
static char mqttTopic[80];
static char mqttClientId[40];

// ─────────────────────────────────────────────────────────────────────────────
// Protótipos
// ─────────────────────────────────────────────────────────────────────────────
void setupOled();
void setupWifi();
void setupMqtt();
void ensureWifi();
void ensureMqtt();
long readDistanceCm();
void processDistance(long cm);
void publishState(SpotState state);
void updateOled(long cm);
void mqttCallback(char* topic, byte* payload, unsigned int length);

// ─────────────────────────────────────────────────────────────────────────────
// Setup
// ─────────────────────────────────────────────────────────────────────────────
void setup() {
    Serial.begin(115200);
    Serial.printf("\n[PontoLivre] FW %s  Meter: %s\n", FW_VERSION, METER_ID);

    // HC-SR04
    pinMode(HCSR04_TRIG_PIN, OUTPUT);
    pinMode(HCSR04_ECHO_PIN, INPUT);
    digitalWrite(HCSR04_TRIG_PIN, LOW);

    // OLED
    Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);
    setupOled();

    // Tópico + client ID
    snprintf(mqttTopic,    sizeof(mqttTopic),    "parquimetro/%s/status", METER_ID);
    snprintf(mqttClientId, sizeof(mqttClientId), "esp32-%s", METER_ID);

    // WiFi + MQTT
    setupWifi();
    setupMqtt();
}

// ─────────────────────────────────────────────────────────────────────────────
// Loop — sem delay(), sem blocking
// ─────────────────────────────────────────────────────────────────────────────
void loop() {
    unsigned long now = millis();

    // 1. Garantir conectividade
    if (now - lastConnCheckMs >= CONN_CHECK_INTERVAL_MS) {
        lastConnCheckMs = now;
        ensureWifi();
        ensureMqtt();
    }

    // 2. Processar loop MQTT (manter keep-alive e receber mensagens)
    mqttClient.loop();

    // 3. Leitura do sensor
    if (now - lastSensorReadMs >= SENSOR_READ_INTERVAL_MS) {
        lastSensorReadMs = now;
        long cm = readDistanceCm();
        processDistance(cm);

        // Atualizar OLED a cada 500ms (não a cada leitura, para reduzir flickering)
        if (now - lastOledUpdateMs >= 500) {
            lastOledUpdateMs = now;
            updateOled(cm);
        }
    }

    // 4. Envio periódico de status (Heartbeat a cada 5 segundos)
    if (now - lastHeartbeatMs >= 5000) {
        lastHeartbeatMs = now;
        if (currentState != SpotState::UNKNOWN) {
            Serial.println("[MQTT] Enviando status periodico...");
            publishState(currentState);
        } else {
            // Se ainda não sabe o estado, envia "Livre" por padrão para registro inicial
            publishState(SpotState::FREE);
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WiFi
// ─────────────────────────────────────────────────────────────────────────────
void setupWifi() {
    Serial.printf("[WiFi] Conectando a %s", WIFI_SSID);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    WiFi.setAutoReconnect(true);

    // Espera bloqueante apenas no setup inicial (até 20s)
    unsigned long t = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - t < 20000) {
        delay(500);
        Serial.print(".");
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.printf("\n[WiFi] Conectado. IP: %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println("\n[WiFi] Timeout — continuando sem WiFi.");
    }
}

void ensureWifi() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("[WiFi] Reconectando...");
        WiFi.disconnect();
        WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MQTT
// ─────────────────────────────────────────────────────────────────────────────
void setupMqtt() {
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(mqttCallback);
    mqttClient.setKeepAlive(30);
    mqttClient.setSocketTimeout(10);
    ensureMqtt();
}

void ensureMqtt() {
    if (WiFi.status() != WL_CONNECTED) return;
    if (mqttClient.connected()) return;

    Serial.printf("[MQTT] Conectando a %s:%d...", MQTT_BROKER, MQTT_PORT);

    bool ok;
    if (strlen(MQTT_USER) > 0) {
        ok = mqttClient.connect(mqttClientId, MQTT_USER, MQTT_PASS);
    } else {
        ok = mqttClient.connect(mqttClientId);
    }

    if (ok) {
        Serial.println(" OK");
        // Re-publicar estado atual após reconexão
        if (lastPublished != SpotState::UNKNOWN) {
            publishState(lastPublished);
        }
    } else {
        Serial.printf(" FALHOU (rc=%d)\n", mqttClient.state());
    }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
    // Parquímetro apenas publica; este callback é mantido por extensibilidade
    char msg[length + 1];
    memcpy(msg, payload, length);
    msg[length] = '\0';
    Serial.printf("[MQTT] Recebido [%s]: %s\n", topic, msg);
}

// ─────────────────────────────────────────────────────────────────────────────
// Sensor HC-SR04
// ─────────────────────────────────────────────────────────────────────────────
long readDistanceCm() {
    // Pulso de trigger
    digitalWrite(HCSR04_TRIG_PIN, LOW);
    delayMicroseconds(2);
    digitalWrite(HCSR04_TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(HCSR04_TRIG_PIN, LOW);

    long duration = pulseIn(HCSR04_ECHO_PIN, HIGH, ECHO_TIMEOUT_US);
    if (duration == 0) return -1; // timeout → sem objeto detectado

    long cm = duration / 58L;
    return cm;
}

// ─────────────────────────────────────────────────────────────────────────────
// Lógica de detecção de vaga
// ─────────────────────────────────────────────────────────────────────────────
void processDistance(long cm) {
    unsigned long now = millis();

    bool objectDetected = (cm > 0 && cm <= OCCUPIED_THRESHOLD_CM);

    if (objectDetected) {
        // Vaga ocupada — cancela timer de "livre"
        freeTimerRunning = false;
        freeStartMs = 0;

        if (currentState != SpotState::OCCUPIED) {
            currentState = SpotState::OCCUPIED;
            Serial.printf("[Sensor] Vaga OCUPADA (%.0ld cm)\n", cm);
        }

        // Publicar "Ocupado" se estado mudou ou debounce passou
        if (lastPublished != SpotState::OCCUPIED &&
            now - lastPublishMs >= PUBLISH_DEBOUNCE_MS) {
            publishState(SpotState::OCCUPIED);
        }

    } else {
        // Sem objeto detectado

        if (currentState == SpotState::OCCUPIED || currentState == SpotState::UNKNOWN) {
            // Acabou de ficar vazia — inicia temporizador de 15 segundos
            if (!freeTimerRunning) {
                freeTimerRunning = true;
                freeStartMs = now;
                Serial.printf("[Sensor] Vaga possivelmente livre — aguardando %lus de confirmacao\n",
                              FREE_CONFIRM_MS / 1000);
            }
        }

        // Checar se passou o tempo de confirmação (15s contínuos vazio)
        if (freeTimerRunning && now - freeStartMs >= FREE_CONFIRM_MS) {
            freeTimerRunning = false;

            if (currentState != SpotState::FREE) {
                currentState = SpotState::FREE;
                Serial.println("[Sensor] Vaga LIVRE confirmada");
            }

            if (lastPublished != SpotState::FREE &&
                now - lastPublishMs >= PUBLISH_DEBOUNCE_MS) {
                publishState(SpotState::FREE);
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Publicação MQTT
// ─────────────────────────────────────────────────────────────────────────────
void publishState(SpotState state) {
    if (!mqttClient.connected()) return;

    const char* payload = (state == SpotState::OCCUPIED) ? "Ocupado" : "Livre";
    bool ok = mqttClient.publish(mqttTopic, payload, true); // retained = true

    if (ok) {
        lastPublished = state;
        lastPublishMs = millis();
        Serial.printf("[MQTT] Publicado [%s]: %s\n", mqttTopic, payload);
    } else {
        Serial.println("[MQTT] Falha ao publicar");
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OLED
// ─────────────────────────────────────────────────────────────────────────────
void setupOled() {
    oledOk = oled.begin(SSD1306_SWITCHCAPVCC, OLED_ADDRESS);
    if (!oledOk) {
        Serial.println("[OLED] Display nao encontrado — continuando sem display");
        return;
    }
    oled.clearDisplay();
    oled.setTextColor(SSD1306_WHITE);
    oled.setTextSize(1);
    oled.setCursor(0, 0);
    oled.println("PontoLivre");
    oled.printf("Meter: %s\n", METER_ID);
    oled.printf("FW: %s\n", FW_VERSION);
    oled.display();
    Serial.println("[OLED] Display inicializado");
}

void updateOled(long cm) {
    if (!oledOk) return;

    oled.clearDisplay();

    // Linha 1 — ID do parquímetro
    oled.setTextSize(1);
    oled.setCursor(0, 0);
    oled.printf("Meter: %s", METER_ID);

    // Linha 2 — Status WiFi/MQTT
    oled.setCursor(0, 12);
    oled.printf("WiFi:%s MQTT:%s",
        WiFi.status() == WL_CONNECTED ? "OK" : "NO",
        mqttClient.connected() ? "OK" : "NO");

    // Linha 3 — Distância lida
    oled.setCursor(0, 24);
    if (cm < 0) {
        oled.print("Dist: ---");
    } else {
        oled.printf("Dist: %ld cm", cm);
    }

    // Linha 4 — Status da vaga (grande)
    oled.setTextSize(2);
    oled.setCursor(0, 40);

    if (currentState == SpotState::OCCUPIED) {
        oled.print("OCUPADA");
    } else if (currentState == SpotState::FREE) {
        oled.print("LIVRE");
    } else {
        // Aguardando confirmação de livre
        if (freeTimerRunning) {
            unsigned long elapsed = (millis() - freeStartMs) / 1000;
            oled.setTextSize(1);
            oled.setCursor(0, 40);
            oled.printf("Confirmando...\n%lus/%lus",
                         elapsed, FREE_CONFIRM_MS / 1000);
        } else {
            oled.print("...");
        }
    }

    oled.display();
}
