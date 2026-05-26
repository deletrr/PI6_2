# Sistema Inteligente de Parquimetros

Sistema distribuido de gerenciamento de vagas de estacionamento, composto por hardware IoT (ESP32), backend Spring Boot, aplicativos mobile multiplataforma (KMP) e infraestrutura containerizada.

---

## Sumario

- [Arquitetura](#arquitetura)
- [Stack Tecnologica](#stack-tecnologica)
- [Estrutura do Repositorio](#estrutura-do-repositorio)
- [Seguranca](#seguranca)
- [Prerequisitos](#prerequisitos)
- [Execucao Local (Docker Compose)](#execucao-local-docker-compose)
- [Deploy em Producao (K3s)](#deploy-em-producao-k3s)
- [Firmware ESP32](#firmware-esp32)
- [Apps Mobile (KMP)](#apps-mobile-kmp)
- [API Reference](#api-reference)
- [Fluxo de Dados IoT](#fluxo-de-dados-iot)

---

## Arquitetura

```
[ESP32 + Sensores]
        |
    MQTT / mTLS (porta 8883)
        |
[Eclipse Mosquitto]
        |
[Spring Boot Backend] <-- JWT HS512 --> [Apps KMP (Android / iOS)]
        |
  [PostgreSQL + PostGIS]
```

Cada ESP32 monitora uma vaga com sensor ultrasonico e sensor magnetico em paralelo. A cada mudanca de estado (ou a cada 30 segundos por timer), o firmware assina o payload JSON com HMAC-SHA256 usando uma chave simetrica unica por dispositivo e publica no topico MQTT. O backend valida a assinatura antes de persistir qualquer dado.

Os aplicativos mobile consomem a API REST autenticada por JWT de 512 bits. O app do Fiscal adiciona marca d'agua nas fotos de infracao e gera um hash SHA-256 da imagem antes do upload. Ambos os apps bloqueiam GPS falso (Mock Location no Android, `sourceInformation` no iOS).

---

## Stack Tecnologica

| Camada       | Tecnologia                                         |
|--------------|----------------------------------------------------|
| Backend      | Spring Boot 3.3 (Kotlin), Spring Security, Flyway  |
| Banco        | PostgreSQL 16 + PostGIS 3.4                        |
| Mensageria   | Eclipse Mosquitto 2.0 (MQTT over TLS 1.3)          |
| Mobile       | Kotlin Multiplatform 2.0 + Compose Multiplatform   |
| HTTP Client  | Ktor Client 2.3                                    |
| Hardware     | ESP32 + Arduino / C++ + mbedtls                    |
| Infra Dev    | Docker Compose                                     |
| Infra Prod   | Kubernetes / K3s                                   |
| Autenticacao | JWT HS512 (jjwt 0.12)                              |
| Integridade  | HMAC-SHA256 (mbedtls no ESP32, javax.crypto no JVM)|

---

## Estrutura do Repositorio

```
parquimetro-v7/
|
+-- backend/                         Spring Boot
|   +-- Dockerfile
|   +-- build.gradle.kts
|   +-- src/main/
|       +-- kotlin/com/parquimetro/
|       |   +-- ParquimetroApplication.kt
|       |   +-- api/
|       |   |   +-- controller/Controllers.kt   REST endpoints
|       |   |   +-- dto/Dtos.kt                 Request/Response DTOs
|       |   +-- config/
|       |   |   +-- MqttConfig.kt               Conexao MQTT + TLS
|       |   |   +-- SecurityConfig.kt           Spring Security stateless
|       |   +-- domain/
|       |   |   +-- entity/
|       |   |   |   +-- Vaga.kt                 Entidade com Point PostGIS
|       |   |   |   +-- HardwareESP32.kt        Vinculo hardware <-> vaga
|       |   |   |   +-- Operacional.kt          Infracao + Pagamento
|       |   |   |   +-- VagaStatus.kt           Enum LIVRE/OCUPADA/etc
|       |   |   +-- repository/
|       |   |       +-- VagaRepository.kt
|       |   |       +-- HardwareESP32Repository.kt
|       |   |       +-- OperacionalRepositories.kt
|       |   +-- mqtt/
|       |   |   +-- TelemetriaListener.kt       Subscriber + validacao HMAC
|       |   +-- security/
|       |   |   +-- JwtService.kt               Gera/valida JWT HS512
|       |   |   +-- JwtAuthFilter.kt            Filtro de autenticacao
|       |   |   +-- HmacUtils.kt                compute / verify HMAC-SHA256
|       |   +-- service/
|       |       +-- VagaService.kt
|       |       +-- OperacionalServices.kt      PagamentoService + InfracaoService
|       +-- resources/
|           +-- application.yml
|           +-- db/migration/
|               +-- V1__init.sql                PostGIS, vagas, hardware_esp32
|               +-- V2__operacional.sql         infracoes, pagamentos
|
+-- firmware/
|   +-- parquimetro/
|       +-- parquimetro.ino          Main loop + deep sleep
|       +-- config.h                 WiFi, MQTT, pinos, constantes
|       +-- hmac_util.h              HMAC-SHA256 via mbedtls
|       +-- sensor.h                 Ultrasonico + magnetico
|       +-- display.h                LCD I2C + nonce dinamico 30s
|       +-- mqtt_handler.h           Conexao mTLS + publish assinado
|
+-- kmp/
|   +-- settings.gradle.kts
|   +-- shared/
|   |   +-- build.gradle.kts
|   |   +-- src/
|   |       +-- commonMain/kotlin/com/parquimetro/
|   |       |   +-- dto/Dtos.kt                 DTOs serializaveis
|   |       |   +-- network/
|   |       |   |   +-- ApiClient.kt            Ktor Client + JWT header
|   |       |   |   +-- Repositories.kt         VagaRepo, PagamentoRepo, etc
|   |       |   +-- security/
|   |       |   |   +-- LocationValidator.kt    expect class
|   |       |   +-- feature/
|   |       |       +-- fiscal/
|   |       |       |   +-- FotoProcessor.kt    expect class
|   |       |       |   +-- FiscalViewModel.kt
|   |       |       +-- motorista/
|   |       |           +-- MotoristaViewModel.kt
|   |       +-- androidMain/kotlin/com/parquimetro/
|   |       |   +-- security/LocationValidator.android.kt  isMock / isFromMockProvider
|   |       |   +-- feature/fiscal/FotoProcessor.android.kt  Canvas + SHA-256
|   |       +-- iosMain/kotlin/com/parquimetro/
|   |           +-- security/LocationValidator.ios.kt  sourceInformation
|   |           +-- feature/fiscal/FotoProcessor.ios.kt  CoreGraphics + CommonCrypto
|   +-- androidApp/
|       +-- src/main/kotlin/com/parquimetro/android/
|           +-- MainActivity.kt
|           +-- camera/CameraModule.kt    CameraX + captura
|           +-- ui/AppNavHost.kt          Compose Navigation
|
+-- infra/
    +-- .env.example
    +-- docker-compose.yml
    +-- mosquitto/
    |   +-- mosquitto.conf
    +-- certs/
    |   +-- gen_certs.sh             Gera CA, servidor, backend e ESP32
    +-- k8s/
        +-- secrets.yml
        +-- postgres.yml             Deployment + Service + PVC
        +-- mosquitto.yml            Deployment + Service + ConfigMap
        +-- backend.yml              Deployment + Service
        +-- deploy.sh                Script de apply no cluster
```

---

## Seguranca

### JWT HS512
Todos os endpoints REST (exceto `/api/auth/login`) exigem `Authorization: Bearer <token>`. O token e assinado com HS512 usando uma chave de 512 bits derivada da variavel de ambiente `JWT_SECRET` (Base64 de 64 bytes).

Para gerar uma chave valida:
```bash
openssl rand -base64 64
```

### HMAC-SHA256 nos payloads MQTT
O ESP32 assina o JSON de telemetria com a chave simetrica `hmacSecret` armazenada no banco em `hardware_esp32.hmac_secret`. O backend recupera essa chave pelo `deviceId` e rejeita silenciosamente qualquer mensagem com assinatura invalida.

Formato do payload publicado:
```json
{
  "deviceId": "ESP32-VAG-001",
  "status": "OCUPADA",
  "battery": 87,
  "nonce": "a3f1c9b2",
  "timestamp": 1716320000000,
  "hmac": "e3b0c44298fc1c149..."
}
```

### Bloqueio de GPS Falso
- Android: `Location.isMock` (API 31+) ou `Location.isFromMockProvider` (legado)
- iOS: inspecao de `CLLocation.sourceInformation.isSimulatedBySoftware`

Qualquer chamada a `LocationValidator.validarLocalizacao()` lanca `MockLocationException` se GPS simulado for detectado, abortando o fluxo antes de qualquer requisicao de pagamento ou registro de infracao.

### Hash de Fotos
O app do Fiscal gera um hash SHA-256 da imagem apos aplicar a marca d'agua com nome do fiscal, data e hora. Somente o hash e enviado para a API junto com os metadados da infracao. A imagem em si e armazenada em storage externo (S3/GCS) e pode ser verificada posteriormente pelo hash.

---

## Prerequisitos

**Para execucao local:**
- Docker 24+ e Docker Compose v2
- OpenSSL 3 (para gerar certificados)
- Java 17+ (para build manual do backend)

**Para firmware:**
- Arduino IDE 2.x ou PlatformIO
- Board package: `esp32` by Espressif 2.x
- Bibliotecas: `PubSubClient`, `LiquidCrystal_I2C`

**Para apps mobile:**
- Android Studio Hedgehog+
- JDK 17
- Xcode 15+ (para iOS)
- Kotlin Multiplatform plugin

**Para deploy K3s:**
- K3s instalado no node
- `kubectl` configurado
- `helm` (opcional)

---

## Execucao Local (Docker Compose)

### 1. Gerar os certificados

```bash
cd infra/certs
chmod +x gen_certs.sh
./gen_certs.sh
```

O script gera:
- `ca.crt` / `ca.key` - Autoridade Certificadora do projeto
- `server.crt` / `server.key` - Certificado do broker Mosquitto
- `backend.crt` / `backend.key` - Certificado cliente do Spring Boot
- `ESP32-VAG-001.crt` / `.key` - Certificado cliente para o primeiro dispositivo
- `ESP32-VAG-002.crt` / `.key` - Certificado cliente para o segundo dispositivo

Ao final, o script imprime o valor do `JWT_SECRET` gerado automaticamente.

### 2. Configurar variaveis de ambiente

```bash
cp infra/.env.example infra/.env
```

Edite `infra/.env`:
```env
JWT_SECRET=<valor impresso pelo gen_certs.sh>
ADMIN_PASS=sua_senha_admin
DB_PASS=parquimetro
POSTGRES_PASSWORD=parquimetro
```

### 3. Subir os servicos

```bash
cd infra
docker compose --env-file .env up -d
```

Ordem de inicializacao gerenciada por `healthcheck` no compose:
1. PostgreSQL (aguarda `pg_isready`)
2. Mosquitto (aguarda conexao MQTT)
3. Spring Boot (conecta ao banco e ao broker)

### 4. Verificar

```bash
# Logs do backend
docker compose logs -f backend

# Testar autenticacao
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"sua_senha_admin"}'

# Listar vagas (substitua o token)
curl http://localhost:8080/api/vagas \
  -H "Authorization: Bearer <token>"
```

### 5. Parar

```bash
docker compose down
# Para remover volumes tambem:
docker compose down -v
```

---

## Deploy em Producao (K3s)

### 1. Preparar os secrets

Preencha `infra/k8s/secrets.yml` com os valores em Base64:

```bash
# Exemplo para JWT_SECRET
echo -n "seu_jwt_secret" | base64 -w0

# Exemplo para certificados
base64 -w0 < infra/certs/ca.crt
```

### 2. Aplicar os manifestos

```bash
cd infra/k8s
chmod +x deploy.sh
./deploy.sh
```

O script cria o namespace `parquimetro`, aplica os manifestos na ordem correta e aguarda os pods ficarem prontos.

### 3. Verificar o cluster

```bash
kubectl get pods -n parquimetro
kubectl get services -n parquimetro

# Logs do backend em producao
kubectl logs -n parquimetro deployment/parquimetro-backend -f
```

### Ordem dos manifestos

```
secrets.yml  ->  postgres.yml  ->  mosquitto.yml  ->  backend.yml
```

O Mosquitto e exposto como `LoadBalancer` na porta 8883 para receber conexoes dos ESP32 externos ao cluster. O backend e `ClusterIP` e deve ser exposto via Ingress (nginx, traefik) para acesso externo.

---

## Firmware ESP32

### Configuracao

Edite `firmware/parquimetro/config.h`:

```cpp
#define WIFI_SSID    "nome_da_rede"
#define WIFI_PASS    "senha_da_rede"
#define MQTT_BROKER  "ip_ou_dominio_do_broker"
#define DEVICE_ID    "ESP32-VAG-001"
#define HMAC_SECRET  "chave_hmac_do_dispositivo_no_banco"
```

### Certificados no firmware

Abra `firmware/parquimetro/mqtt_handler.h` e substitua os placeholders pelo conteudo dos arquivos gerados:

```
CA_CERT      <- infra/certs/ca.crt
CLIENT_CERT  <- infra/certs/ESP32-VAG-001.crt
CLIENT_KEY   <- infra/certs/ESP32-VAG-001.key
```

Para novos dispositivos, execute novamente o `gen_certs.sh` com um novo `DEVICE_ID` e cadastre o hardware no banco:

```sql
INSERT INTO hardware_esp32 (vaga_id, device_id, hmac_secret, firmware_version, battery_level)
VALUES ('<uuid_da_vaga>', 'ESP32-VAG-003', 'chave_hmac_unica', '1.0.0', 100);
```

### Fluxo de operacao do firmware

1. Acorda por interrupcao no `PIN_WAKEUP` (sensor magnetico) ou por timer a cada 30 segundos
2. Le sensor ultrasonico e magnetico; decide o estado da vaga
3. Atualiza o display LCD com status atual e novo nonce (expira a cada 30s)
4. Se estado mudou ou bateria baixa: conecta WiFi, conecta MQTT com mTLS, publica telemetria assinada
5. Entra em deep sleep

### Bibliotecas necessarias (Arduino IDE)

Instale via Library Manager:
- `PubSubClient` by Nick O'Leary
- `LiquidCrystal I2C` by Frank de Brabander

A biblioteca `mbedtls` ja esta incluida no board package do ESP32 (Espressif).

---

## Apps Mobile (KMP)

### Build Android

```bash
cd kmp
./gradlew :androidApp:assembleDebug
```

O APK fica em `kmp/androidApp/build/outputs/apk/debug/`.

### Build iOS

Abra `kmp/` no Xcode ou Android Studio com o plugin KMP instalado. O target iOS compila o modulo `shared` como framework XCFramework.

### Configurar a URL da API

Em `kmp/shared/src/commonMain/kotlin/com/parquimetro/network/ApiClient.kt`, altere:

```kotlin
private const val BASE_URL = "https://api.parquimetro.com"
```

Para o endereco real do backend.

### Permissoes necessarias (Android)

O `AndroidManifest.xml` deve conter:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## API Reference

Todos os endpoints (exceto `/api/auth/login`) exigem `Authorization: Bearer <token>`.

### Autenticacao

```
POST /api/auth/login
Body: { "username": "admin", "password": "..." }
Response: { "token": "..." }
```

### Vagas

```
GET    /api/vagas?page=0&size=20        Lista paginada
GET    /api/vagas/{id}                  Detalhe de uma vaga
POST   /api/vagas                       Criar vaga
       Body: { "codigo": "A-01", "lat": -23.5, "lng": -46.6 }
PATCH  /api/vagas/{id}/status?status=MANUTENCAO   Atualizar status
```

Status validos: `LIVRE`, `OCUPADA`, `IRREGULAR`, `MANUTENCAO`

### Pagamentos

```
POST /api/pagamentos
Body: {
  "vagaId": "<uuid>",
  "motoristaCpf": "000.000.000-00",
  "placa": "ABC1D23",
  "duracaoMinutos": 60
}
Response: { "id": "...", "vagaId": "...", "expiraEm": 1716320000000, "valor": 6.0 }
```

### Infracoes

```
POST /api/infracoes
Body: {
  "vagaId": "<uuid>",
  "fiscalId": "fiscal-001",
  "fotoHash": "<sha256 da imagem com marca dagua>",
  "lat": -23.5,
  "lng": -46.6,
  "timestamp": 1716320000000
}
```

---

## Fluxo de Dados IoT

```
ESP32 acorda
    |
Sensor magnetico + ultrasonico
    |
Estado diferente do anterior?
  Nao -> atualiza display + volta a sleep
  Sim ->
    Conecta WiFi
    Conecta MQTT (mTLS com certificado de cliente)
    Monta JSON: { deviceId, status, battery, nonce, timestamp }
    Assina com HMAC-SHA256(payload, hmacSecret)
    Publica em parquimetro/{deviceId}/telemetria
    Aguarda 500ms (processa ACK)
    Deep sleep ate proximo evento
         |
[Mosquitto recebe e repassa]
         |
[TelemetriaListener no backend]
    Desserializa JSON
    Busca hardware pelo deviceId
    Valida HMAC com chave do banco
    Rejeita se invalido (log warning, sem erro HTTP)
    Atualiza battery + lastSeen no HardwareESP32
    Atualiza status na Vaga
    PagamentoService.expirarPagamentos() roda a cada 60s
        -> pagamentos vencidos: status = EXPIRADO, vaga = LIVRE
```

---

## Variaveis de Ambiente

| Variavel          | Obrigatoria | Descricao                                      |
|-------------------|-------------|------------------------------------------------|
| `JWT_SECRET`      | Sim         | Base64 de 64 bytes (saida do gen_certs.sh)     |
| `ADMIN_PASS`      | Sim         | Senha do usuario admin inicial                 |
| `DB_HOST`         | Sim         | Host do PostgreSQL                             |
| `DB_USER`         | Sim         | Usuario do banco                               |
| `DB_PASS`         | Sim         | Senha do banco                                 |
| `MQTT_BROKER_URL` | Sim         | Ex: `ssl://mosquitto:8883`                     |
| `MQTT_CA_CERT`    | Sim         | Caminho para ca.crt dentro do container        |
| `MQTT_CLIENT_CERT`| Sim         | Caminho para backend.crt dentro do container   |
| `MQTT_CLIENT_KEY` | Sim         | Caminho para backend.key dentro do container   |

---

## Licenca

Projeto academico. Uso livre para fins educacionais.
