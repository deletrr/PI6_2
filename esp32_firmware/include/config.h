#pragma once

// ── WiFi ──────────────────────────────────────────────────────────────────────
#define WIFI_SSID       "SeuWiFi"
#define WIFI_PASSWORD   "SuaSenha"

// ── MQTT ──────────────────────────────────────────────────────────────────────
// IP do computador rodando Mosquitto na mesma rede
#define MQTT_BROKER     "192.168.1.100"
#define MQTT_PORT       1883
#define MQTT_USER       ""
#define MQTT_PASS       ""

// ID único deste parquímetro — deve corresponder ao `code` no banco de dados
#define METER_ID        "PKM-001"

// Tópico publicado: parquimetro/<METER_ID>/status
// Payloads: "Ocupado" | "Livre"

// ── HC-SR04 ───────────────────────────────────────────────────────────────────
#define HCSR04_TRIG_PIN     5
#define HCSR04_ECHO_PIN     18

// Distância máxima para considerar vaga OCUPADA (cm)
// Ajuste conforme a altura de instalação do sensor
#define OCCUPIED_THRESHOLD_CM   80

// ── OLED SSD1306 (I2C) ────────────────────────────────────────────────────────
#define OLED_SDA_PIN        21
#define OLED_SCL_PIN        22
#define OLED_ADDRESS        0x3C
#define OLED_WIDTH          128
#define OLED_HEIGHT         64

// ── Temporização ──────────────────────────────────────────────────────────────
// Tempo contínuo vazio antes de publicar "Livre" (ms) — RN ESP32
#define FREE_CONFIRM_MS         15000UL   // 15 segundos

// Intervalo mínimo entre publicações do mesmo status (ms)
#define PUBLISH_DEBOUNCE_MS     2000UL

// Intervalo de leitura do sensor (ms) — sem delay() no loop
#define SENSOR_READ_INTERVAL_MS 200UL

// Timeout de eco do HC-SR04 (µs)
#define ECHO_TIMEOUT_US         30000UL

// Intervalo de verificação de conexão (ms)
#define CONN_CHECK_INTERVAL_MS  5000UL

// ── Versão do firmware ────────────────────────────────────────────────────────
#define FW_VERSION  "1.0.0"
