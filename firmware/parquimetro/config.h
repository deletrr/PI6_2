#pragma once

#define WIFI_SSID       "SUA_REDE"
#define WIFI_PASS       "SUA_SENHA"

#define MQTT_BROKER     "seu-broker.parquimetro.com"
#define MQTT_PORT       8883
#define DEVICE_ID       "ESP32-VAG-001"
#define HMAC_SECRET     "chave-hmac-256bits-unica-por-dispositivo"

#define TOPIC_TELEMETRIA  "parquimetro/" DEVICE_ID "/telemetria"
#define TOPIC_CMD         "parquimetro/" DEVICE_ID "/cmd"

#define PIN_SENSOR_TRIGGER  18
#define PIN_SENSOR_ECHO     19
#define PIN_SENSOR_MAG      34
#define PIN_WAKEUP          GPIO_NUM_34

#define DIST_OCUPADA_CM     40
#define DEEP_SLEEP_US       30000000ULL   // 30s
#define NONCE_TTL_MS        30000
#define TELEMETRIA_INTERVAL 10000

#define LCD_ADDR   0x27
#define LCD_COLS   16
#define LCD_ROWS   2
