# ESP32 Firmware — PontoLivre

## Hardware necessário

| Componente        | Quantidade |
|-------------------|-----------|
| ESP32 DevKit v1   | 1         |
| HC-SR04           | 1         |
| OLED SSD1306 128×64 (I2C) | 1 |
| Jumpers           | —         |
| Protoboard        | 1         |

---

## Diagrama de conexões

```
ESP32                HC-SR04
─────────────────────────────
GPIO 5  (TRIG)  →   TRIG
GPIO 18 (ECHO)  →   ECHO
3.3V            →   VCC
GND             →   GND

ESP32                OLED SSD1306
──────────────────────────────────
GPIO 21 (SDA)   →   SDA
GPIO 22 (SCL)   →   SCL
3.3V            →   VCC
GND             →   GND
```

> ⚠️ Alguns módulos HC-SR04 operam em 5V. Nesses casos, use um divisor de tensão
> no pino ECHO (5V → 3.3V) para não danificar o ESP32.
> VCC do HC-SR04 pode ser ligado ao pino 5V do ESP32 (VUSB).

---

## Configuração

Edite `include/config.h`:

```cpp
#define WIFI_SSID       "SuaRede"
#define WIFI_PASSWORD   "SuaSenha"
#define MQTT_BROKER     "192.168.1.100"   // IP da máquina com Mosquitto
#define METER_ID        "PKM-001"          // Deve existir no banco de dados
```

Ajuste `OCCUPIED_THRESHOLD_CM` conforme a altura de instalação:
- Sensor a ~1,5m do chão → use 120 cm
- Sensor a ~50cm do chão → use 40 cm

---

## Build e flash

```bash
# Instalar PlatformIO CLI
pip install platformio

# Build
cd esp32_firmware
pio run

# Flash
pio run --target upload

# Monitor serial
pio device monitor --baud 115200
```

Ou use a extensão PlatformIO no VS Code.

---

## Comportamento

```
┌──────────────────────────────────────────────────────────┐
│                     LOOP PRINCIPAL                        │
│                                                          │
│  A cada 200ms:  lê HC-SR04                               │
│                                                          │
│  Objeto ≤ 80cm  ──→  estado = OCUPADO                    │
│                       publica "Ocupado"                  │
│                                                          │
│  Sem objeto     ──→  inicia temporizador 15s             │
│                       se 15s contínuos vazios:           │
│                         estado = LIVRE                   │
│                         publica "Livre"                  │
│                                                          │
│  A cada 5s: verifica WiFi e MQTT, reconecta se necessário│
└──────────────────────────────────────────────────────────┘
```

### Tópico MQTT publicado

```
parquimetro/PKM-001/status
```

### Payloads

| Situação         | Payload    |
|------------------|-----------|
| Veículo detectado | `Ocupado` |
| Vaga livre (15s)  | `Livre`   |

---

## Display OLED

```
┌────────────────────────┐
│ Meter: PKM-001         │
│ WiFi:OK  MQTT:OK       │
│ Dist: 45 cm            │
│                        │
│ OCUPADA                │
└────────────────────────┘
```

Quando aguardando confirmação de livre:

```
┌────────────────────────┐
│ Meter: PKM-001         │
│ WiFi:OK  MQTT:OK       │
│ Dist: 200 cm           │
│ Confirmando...         │
│ 7s/15s                 │
└────────────────────────┘
```

---

## Simulação sem hardware (via MQTT)

```bash
# Simular vaga ocupada
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Ocupado"

# Simular vaga livre
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Livre"
```
