# Projeto Interdisciplinar – Plano de Teste de Software

---

## Grupo 1: Especificação e Arquitetura do Software

### 1. Descrição Textual do Caso de Uso (Quadro 1a)
**Caso de Uso:** Gerenciar Ocupação e Tarifação Inteligente  
**Descrição:** O sistema monitora vagas de estacionamento rotativo por meio de sensores IoT. Ao detectar um veículo, inicia o fluxo de cobrança com período de tolerância, tarifas configuradas e fiscalização automática.  
**Ator Primário:** Sensor IoT  
**Atores Secundários:** Motorista, Fiscal de Trânsito

| Seção | Descrição |
| :--- | :--- |
| **Precondições** | PC01: A vaga está registrada com status inicial LIVRE. <br>PC02: Tabela de tarifas e horário de funcionamento configurados. |
| **Fluxo Principal** | 1. Sensor detecta entrada e envia "Carro Detectado" (MQTT).<br>2. Sistema confirma Horário Comercial (RN03).<br>3. Sistema altera status para AGUARDANDO_PAGAMENTO.<br>4. Sistema inicia Tolerância de 15 min (RN02).<br>5. Motorista abre app, insere placa e paga 1 hora.<br>6. Sistema altera status para OCUPADA_PAGA.<br>7. Sensor detecta saída e sistema encerra sessão (LIVRE). |
| **Fluxos Alternativos** | A1: Tolerância Expirada sem Pagamento (Status OVERTIME).<br>A2: Veículo Fora do Horário Comercial (Custo R$ 0,00).<br>A3: Extensão de Tempo via App. |
| **Fluxos de Exceção** | E1: Falha na comunicação MQTT (Log de erro).<br>E2: Falha no processamento do Pagamento. |
| **Pós-condições** | Vaga retorna a LIVRE e registros financeiros/infrações salvos no banco. |

**Regras de Negócio (RN):**
* **RN01 - Tarifação por Hora Cheia:** Tarifa de R$ 2,00/h. Após 5 min de fração, cobra hora integral (ex: 1h 06min = R$ 4,00).
* **RN02 - Tolerância Inicial:** Período gratuito de 15 min. Status "AGUARDANDO_PAGAMENTO".
* **RN03 - Horário Comercial:** Seg-Sex (08h-18h), Sáb (08h-13h). Fora disso: R$ 0,00.

### 2. Diagrama Arquitetural Completo
A arquitetura do PontoLivre é baseada em uma pilha tecnológica moderna e distribuída, integrando hardware, serviços de nuvem e aplicações multiplataforma.

```mermaid
graph TD
    subgraph "Camada de Dispositivos (IoT)"
        ESP32[Sensores ESP32 - C++/Arduino]
        DistSensor(Sensor Ultrassônico/Infravermelho)
        ESP32 --- DistSensor
    end

    subgraph "Protocolos de Comunicação"
        MQTT[Broker MQTT - Mosquitto]
        WS[WebSockets - Real Time Updates]
        REST[REST API - JSON/HTTPS]
    end

    subgraph "Servidor de Aplicação (Backend - Kotlin/Spring Boot)"
        direction TB
        MqttSub[MqttSubscriber - Processador de Eventos IoT]
        Auth[Security - JWT / BCrypt / OAuth2]
        Core[Business Logic - Billing, Sessions, Wallet]
        Sched[Schedulers - Verificação de Overtime]
        Socket[WebSocket Controller - Broadcast de Mapas]
    end

    subgraph "Camada de Persistência"
        DB[(PostgreSQL 15)]
        Redis[Redis - Cache/Session Opcional]
    end

    subgraph "Interfaces de Usuário (KMP - Kotlin Multiplatform)"
        Android[App Android - Jetpack Compose]
        Web[Admin Dashboard - Compose HTML]
        Shared[Shared Code - Logic, API, ViewModels]
    end

    %% Conexões
    ESP32 -->|Publish Status| MQTT
    MQTT -->|Subscribe| MqttSub
    MqttSub --> Core
    Core <--> DB
    Core --> Socket
    Socket -->|Real Time| Web
    Android & Web -->|REST| Auth
    Auth <--> Core
    Android & Web --- Shared
```

### 3. Diagrama de Classes Completo
Mapeamento de toda a estrutura lógica do sistema, incluindo serviços de segurança, regras de negócio e persistência.

```mermaid
classDiagram
    namespace Security_Module {
        class JwtService { +generateToken() +validateToken() }
        class JwtAuthFilter { +doFilterInternal() }
        class CustomUserDetailsService { +loadUserByUsername() }
    }

    namespace Billing_Module {
        class BillingService { 
            +calculateCharge() 
            +estimateCurrentCost()
            -countBillableMinutes()
        }
        class SessionScheduler { +checkOvertimeSessions() }
    }

    namespace Business_Services {
        class UserService { +register() +updateProfile() }
        class WalletService { +addBalance() +debit() }
        class ParkingSessionService { +start() +close() +extend() }
        class ParkingMeterService { +listActive() +updateStatus() }
        class SupportService { +openTicket() +reply() }
        class FineService { +applyFine() +payFine() }
    }

    namespace Domain_Entities {
        class User { UUID id, String name, String email, String passwordHash, UserRole role, BigDecimal balance }
        class ParkingMeter { UUID id, String code, ParkingStatus status, Double lat, Double lon, String mqttTopic }
        class ParkingSession { UUID id, String vehiclePlate, LocalDateTime start, LocalDateTime end, SessionStatus status }
        class WalletTransaction { UUID id, TransactionType type, BigDecimal amount, BigDecimal balanceAfter }
        class SupportTicket { UUID id, String subject, String message, Boolean resolved }
        class Fine { UUID id, BigDecimal amount, String reason, FineStatus status }
        class MqttLog { Long id, String topic, String payload, Boolean processed }
    }

    namespace IoT_Firmware {
        class MainFirmware { +setup() +loop() +publishStatus() +handleWifi() }
    }

    %% Relationships
    ParkingSession "n" --> "1" User
    ParkingSession "n" --> "1" ParkingMeter
    WalletTransaction "n" --> "1" User
    SupportTicket "n" --> "1" User
    Fine "n" --> "1" ParkingSession
    Fine "n" --> "1" User
    
    ParkingSessionService ..> BillingService
    ParkingSessionService ..> SessionScheduler
    MqttSub ..> ParkingMeterService
    MqttSub ..> ParkingSessionService
```

---

## Grupo 2: Modelagem e Projeto dos Casos de Teste

### 4. Diagrama de Atividades e GFC

#### Diagrama de Atividades (Swimlanes)
```mermaid
sequenceDiagram
    participant Sensor as Sensor IoT
    participant Sistema as Sistema (Backend)
    participant Motorista as Motorista (App)

    Sensor->>Sistema: Detecta Entrada
    Note right of Sistema: RN02 (Inicia Tolerância)
    Sistema->>Sistema: Valida RN03 (Horário)
    Motorista->>Sistema: Informa Placa e Confirma Pagamento
    Note right of Sistema: RN01 (Monitora Fração)
    Sensor->>Sistema: Detecta Saída
    Sistema->>Sistema: Calcula Valor Final
    Sistema->>Motorista: Notifica Valor e Debita Saldo
```

#### Grafo de Fluxo de Controle (GFC)
```mermaid
graph TD
    N1((Início)) --> N2[Calcula Minutos]
    N2 --> D1{Minutos <= 15? RN02}
    D1 -- Sim --> EndZero[Retorna R$ 0,00]
    D1 -- Não --> N3[Calcula Minutos Faturáveis RN03]
    N3 --> D2{Faturáveis == 0? RN03}
    D2 -- Sim --> EndZero
    D2 -- Não --> N4[Verifica Fração > 5min RN01]
    N4 --> D3{Fração > 5min?}
    D3 -- Sim --> N5[Cobra Hora Integral]
    D3 -- Não --> N6[Mantém Hora Atual]
    N5 --> EndFinal[Retorna Valor]
    N6 --> EndFinal
```

#### Quadro 2a: Caminhos Independentes do GFC
| # Caminho | Caminho | Arcos Primitivos | Regiões do Grafo |
| :--- | :--- | :--- | :--- |
| **C1** | N1 -> N2 -> D1(Sim) -> EndZero | (N1,N2), (N2,D1), (D1,EndZero) | Região 1 (Tolerância RN02) |
| **C2** | N1 -> N2 -> D1(Não) -> N3 -> D2(Sim) -> EndZero | (N1,N2), (N2,D1), (D1,N3), (N3,D2), (D2,EndZero) | Região 2 (Isenção RN03) |
| **C3** | N1 -> N2 -> D1(Não) -> N3 -> D2(Não) -> N4 -> D3(Não) -> N6 -> EndFinal | (N1,N2), (N2,D1), (D1,N3), (N3,D2), (D2,N4), (N4,D3), (D3,N6), (N6,EndFinal) | Região 3 (Cobrança Base RN01) |
| **C4** | N1 -> N2 -> D1(Não) -> N3 -> D2(Não) -> N4 -> D3(Sim) -> N5 -> EndFinal | (N1,N2), (N2,D1), (D1,N3), (N3,D2), (D2,N4), (N4,D3), (D3,N5), (N5,EndFinal) | Região 4 (Acréscimo Fração RN01) | Região 4 (Acréscimo Fração RN01) |


### 5. Diagrama de Transição e Grafo de Estados (GE)

#### Grafo de Estados (GE)
```mermaid
stateDiagram-v2
    [*] --> AGUARDANDO_PAGAMENTO : Detectado
    AGUARDANDO_PAGAMENTO --> OCUPADA_PAGA : Pago (RN02 OK)
    AGUARDANDO_PAGAMENTO --> OVERTIME : Tempo Excedido
    AGUARDANDO_PAGAMENTO --> CLOSED : Saída na Tolerância
    OCUPADA_PAGA --> CLOSED : Veículo Saiu
    OCUPADA_PAGA --> OVERTIME : Tempo Excedido
    OVERTIME --> CLOSED : Multa Paga / Saída
    CLOSED --> [*]
```

#### Quadro 2b: Sequências Independentes do GE
| # Sequência | Sequência de Transições | Estados Envolvidos |
| :--- | :--- | :--- |
| **S1** | [*] -> AGUARDANDO_PAGAMENTO -> OCUPADA_PAGA -> CLOSED -> [*] | INÍCIO, AGUARDANDO_PAGAMENTO, OCUPADA_PAGA, CLOSED, FIM |
| **S2** | [*] -> AGUARDANDO_PAGAMENTO -> OVERTIME -> CLOSED -> [*] | INÍCIO, AGUARDANDO_PAGAMENTO, OVERTIME, CLOSED, FIM |
| **S3** | [*] -> AGUARDANDO_PAGAMENTO -> CLOSED -> [*] | INÍCIO, AGUARDANDO_PAGAMENTO, CLOSED, FIM |
| **S4** | [*] -> AGUARDANDO_PAGAMENTO -> OCUPADA_PAGA -> OVERTIME -> CLOSED -> [*] | INÍCIO, AGUARDANDO_PAGAMENTO, OCUPADA_PAGA, OVERTIME, CLOSED, FIM |

### 6. Matriz de Casos de Teste (Quadro 2c)
| # CT | Cenário | Caminho GFC | Seq. GE | Condição Entrada “RN01” | Condição Entrada “RN02” | Condição Entrada “RN03” | Saída Esperada |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **CT01** | Fluxo Principal | **C1** | **S3** | N/A | 10 min (<= 15 min) | Comercial | R$ 0,00 e CLOSED |
| **CT02** | Cobrança 1h | **C3** | **S1** | 20 min (<= 1h + 5m) | > 15 min | Comercial | R$ 2,00 e CLOSED |
| **CT03** | Cobrança 2h | **C4** | **S1** | 66 min (> 1h + 5m) | > 15 min | Comercial | R$ 4,00 e CLOSED |
| **CT04** | Domingo | **C2** | **S1** | N/A | > 15 min | Domingo | R$ 0,00 e CLOSED |
| **CT05** | Inadimplência| **C3** | **S2** | N/A | Expirada | Comercial | OVERTIME |
| **CT06** | Saída Tolerância| **C1** | **S3** | N/A | 12 min (Dentro) | Comercial | R$ 0,00 e CLOSED |
| **CT07** | Excesso Tempo | **C3** | **S4** | 125 min (> 2h) | Excedida | Comercial | OVERTIME (Pós-pago) |

---

## Grupo 3: Implementação e Execução (Unidade)

### 7. Implementação (Links GitHub)
👉 [Código BillingServiceTest.kt](https://github.com/usuario/ponto-livre/backend/src/test/kotlin/com/smartparking/service/BillingServiceTest.kt)

### 8. Execução e Cobertura (Relatório JaCoCo)
👉 [Relatório de Cobertura JaCoCo](https://github.com/usuario/ponto-livre/reports/coverage)

---

## Grupo 4: Execução dos Testes de Sistema

### 9. Planilha de Testes e Vídeo
👉 [Planilha de Testes de Sistema](https://docs.google.com/spreadsheets/...)  
👉 [Vídeo de Execução (YouTube)](https://youtube.com/...)

---

## Grupo 5: Resumo do Plano de Teste (Quadro 5a)

| Etapas | Técnica: Funcional – Caixa Preta | Técnica: Estrutural – Caixa Branca |
| :--- | :--- | :--- |
| **Planejamento** | Foco na jornada do motorista e RNs de tarifação. | Foco na lógica de precisão do BillingService. |
| **Projeto** | Critérios: Valores Limite e Transição de Estados. | Critérios: Fluxo de Controle e Cobertura de Caminhos. |
| **Execução** | Procedimento: Manual via App Android e Sensores. | Procedimento: Automatizada via JUnit 5. |
| **Análise** | Verificação de logs e persistência financeira. | Análise de Cobertura via JaCoCo. |

---

## Grupo 6: Apresentação
👉 [Vídeo de Apresentação (20 min)](https://youtube.com/...)
