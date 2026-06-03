# PontoLivre — Setup Instructions

## Pré-requisitos

 Ferramenta        | Versão mínima |
-------------------|---------------|
 JDK               | 17            |
 Android Studio    | Hedgehog+     |
 PostgreSQL        | 15            |
 Mosquitto MQTT    | 2.x           |
 PlatformIO        | Latest        |
 Node.js           | 18            |
 Docker (opcional) | 24            |

---

## 1. PostgreSQL

### Criar banco de dados

```bash
psql -U postgres
CREATE DATABASE ponto_livre;
CREATE USER pontolivre_user WITH ENCRYPTED PASSWORD 'pontolivre_pass';
GRANT ALL PRIVILEGES ON DATABASE ponto_livre TO pontolivre_user;
\c ponto_livre
GRANT ALL ON SCHEMA public TO pontolivre_user;
\q
```

### Executar schema + seed

```bash
psql -U pontolivre_user -d ponto_livre -f infra/init.sql
```

---

## 2. Mosquitto MQTT Broker

### Instalar

```bash
# Ubuntu/Debian
sudo apt-get install mosquitto mosquitto-clients

# macOS
brew install mosquitto
```

### Configurar (`/etc/mosquitto/mosquitto.conf` ou `mosquitto.conf` local)

```
listener 1883
allow_anonymous true
```

### Iniciar

```bash
mosquitto -c mosquitto.conf -v
```

### Testar

```bash
# Subscriber
mosquitto_sub -h localhost -t "parquimetro/#" -v

# Publisher (simula ESP32)
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Ocupado"
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Livre"
```

---

## 3. Backend Spring Boot

### Configurar variáveis de ambiente

Edite `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ponto_livre
    username: pontolivre_user
    password: pontolivre_pass

mqtt:
  broker-url: tcp://localhost:1883
  client-id: pontolivre-backend

jwt:
  secret: sua-chave-secreta-minimo-256-bits-aqui
  expiration: 86400000
```

### Executar

No diretório raiz do projeto:
```bash
./gradlew :backend:bootRun
```

Backend disponível em: `http://localhost:8080`

### Endpoints principais

 Método | Endpoint               | Descrição                    |
--------|------------------------|------------------------------|
 POST   | /api/auth/register     | Cadastro de usuário          |
 POST   | /api/auth/login        | Login (retorna JWT)          |
 GET    | /api/parking-meters    | Listar parquímetros          |
 POST   | /api/sessions/start    | Iniciar sessão               |
 POST   | /api/sessions/{id}/end | Encerrar sessão              |
 GET    | /api/wallet/balance    | Saldo da carteira            |
 POST   | /api/wallet/recharge   | Recarregar carteira          |
 GET    | /api/wallet/extract    | Extrato                      |
 GET    | /api/fines             | Listar multas                |

---

## 4. ESP32 Firmware

### Instalar PlatformIO

```bash
pip install platformio
```

### Configurar credenciais

Edite `esp32_firmware/src/config.h`:

```cpp
#define WIFI_SSID       "SeuWiFi"
#define WIFI_PASSWORD   "SuaSenha"
#define MQTT_BROKER     "192.168.1.100"   // IP do computador com Mosquitto
#define MQTT_PORT       1883
#define METER_ID        "PKM-001"         // ID do parquímetro
```

### Build e flash

```bash
cd esp32_firmware
pio run --target upload
pio device monitor --baud 115200
```

### Conexões de hardware

 ESP32 Pin | Componente   | Pino        |
-----------|--------------|-------------|
 GPIO 5    | HC-SR04      | TRIG        |
 GPIO 18   | HC-SR04      | ECHO        |
 GPIO 21   | OLED SDA     | SDA         |
 GPIO 22   | OLED SCL     | SCL         |
 3.3V      | HC-SR04      | VCC         |
 GND       | HC-SR04      | GND         |
 3.3V      | OLED         | VCC         |
 GND       | OLED         | GND         |

---

## 5. Frontend Android

### Configurar URL do backend

Edite `frontend_kmp/shared/src/commonMain/kotlin/com/smartparking/shared/api/ApiClient.kt`:

```kotlin
const val BASE_URL = "http://10.0.2.2:8080"  // emulador Android
// const val BASE_URL = "http://SEU_IP:8080"  // dispositivo físico
```

### Build e instalar

No diretório raiz do projeto:
```bash
./gradlew :frontend_kmp:androidApp:installDebug
```

---

## 6. Frontend Web

```bash
./gradlew :frontend_kmp:webApp:jsBrowserDevelopmentRun
```

Disponível em: `http://localhost:3000`

---

## 7. Docker Compose (alternativa)

Crie `infra/docker-compose.yml`:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ponto_livre
      POSTGRES_USER: pontolivre_user
      POSTGRES_PASSWORD: pontolivre_pass
    ports:
      - "5432:5432"
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql

  mosquitto:
    image: eclipse-mosquitto:2
    ports:
      - "1883:1883"
      - "9001:9001"
    volumes:
      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf
```

`infra/mosquitto.conf`:

```
listener 1883
allow_anonymous true
listener 9001
protocol websockets
allow_anonymous true
```

```bash
cd infra
docker compose up -d
docker compose ps
```
    


---

## 8. Usuários de demonstração

 Email                      | Senha     | Papel |
----------------------------|-----------|-------|
 admin@pontolivre.com       | Admin@123 | ADMIN |
 joao@email.com             | User@123  | USER  |

---

## 9. Simulação sem ESP32

Para simular mensagens MQTT sem hardware:

```bash
# Ocupar vaga PKM-001
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Ocupado"

# Liberar vaga PKM-001 (após 15s backend processa sessão)
mosquitto_pub -h localhost -t "parquimetro/PKM-001/status" -m "Livre"
```

---

## 10. Regras de negócio resumidas

 Regra | Detalhe                                                  |
-------|----------------------------------------------------------|
 RN01  | R$ 2,00/hora, fração integral                            |
 RN02  | 15 min de tolerância gratuita                            |
 RN03  | Cobrança Seg–Sex 08–18h / Sáb 08–13h; demais = R$ 0,00  |
 RN04  | Carteira virtual; recarga via Pix fake ou Cartão fictício|
 RN05  | MQTT "Livre" → encerra sessão, debita saldo              |
 RN06  | Máx 2h → encerra automaticamente + multa                 |
