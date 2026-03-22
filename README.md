# <img src="https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/solid/server.svg" width="28" height="28"> Space Cloud Server — Wake On LAN Controller

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.8-087CFA?logo=ktor&logoColor=white)](https://ktor.io/)
[![SQLite](https://img.shields.io/badge/SQLite-3-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Platform](https://img.shields.io/badge/Platform-Raspberry%20Pi%203%2B-C51A4A?logo=raspberrypi&logoColor=white)](https://www.raspberrypi.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Servidor standalone para **agendamento e envio de pacotes Wake On LAN**, projetado para rodar em um **Raspberry Pi 3+**. Possui uma interface web moderna com dashboard em tempo real, logs de atividade e configurações avançadas.

---

## Funcionalidades

- **Agendamento diário** — Configure um horário para acordar automaticamente seus dispositivos
- **Envio manual** — Botão para disparar Wake On LAN instantaneamente via dashboard
- **Verificação por ping** — Confirma se o dispositivo acordou com sucesso após o envio
- **Dashboard em tempo real** — Uptime do servidor, estatísticas de ativação e próximo agendamento
- **Histórico de logs** — Registro completo de todas as tentativas com status de sucesso/falha
- **Configurações avançadas** — Timeout de ping, intervalo entre tentativas e retenção de logs
- **Multi-broadcast** — Envia pacotes WOL para IP direto, broadcast global e subnet (múltiplas portas)
- **Persistência em SQLite** — Configurações e logs armazenados com WAL mode para confiabilidade
- **Auto-start com systemd** — Inicia automaticamente com o Raspberry Pi

---

## Screenshots

> **Dashboard** com tema escuro, cards de configuração, estatísticas e controle de agendamento.

```
┌──────────────────────────────────────────────────────────┐
│  🖥  Space Cloud Server                    v2.0.0        │
│  Dashboard  │  Logs  │  Configurações                    │
├──────────────────────────────────────────────────────────┤
│  ┌─ Configuração ──────┐  ┌─ Estatísticas ────────────┐ │
│  │ IP:   192.168.1.100 │  │ Total de ativações: 42    │ │
│  │ MAC:  AA:BB:CC:DD:EE│  │ Bem-sucedidas:     38     │ │
│  │ Hora: 07:00         │  │ Última tentativa: 07:00   │ │
│  │ [● Ativo]           │  │ Próximo wake: amanhã 07:00│ │
│  └─────────────────────┘  └───────────────────────────┘ │
│                    [ 🔌 Testar Wake On LAN ]             │
└──────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Camada     | Tecnologia                          |
|------------|-------------------------------------|
| Linguagem  | Kotlin 1.9.22                       |
| Framework  | Ktor 2.3.8 (Netty)                  |
| Banco      | SQLite + Exposed ORM 0.44.1        |
| Frontend   | HTML5, CSS3, JavaScript (vanilla)   |
| Build      | Gradle (Kotlin DSL) — Fat JAR       |
| Runtime    | JVM 11+                             |

---

## Pré-requisitos

### Na máquina de desenvolvimento (build)

- **JDK 11+** (recomendado: [Eclipse Temurin](https://adoptium.net/))
- **Git**

### No Raspberry Pi (deploy)

- **Raspberry Pi 3+** conectado à mesma rede do dispositivo alvo
- **JRE 11+** instalado:
  ```bash
  sudo apt update && sudo apt install -y default-jre
  ```
- **Acesso SSH** habilitado

---

## Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/space-cloud-standalone-server.git
cd space-cloud-standalone-server
```

### 2. Build do projeto

```bash
./gradlew buildFatJar
```

O JAR será gerado em `build/libs/space-cloud-server.jar`.

### 3. Deploy para o Raspberry Pi

Use o script de deploy automatizado:

```bash
chmod +x deploy.sh
./deploy.sh <ip-do-raspberry>
```

**Exemplo:**
```bash
./deploy.sh 192.168.1.50
```

O script executa automaticamente:
1. Build do fat JAR
2. Cria o diretório `/home/pi/space-cloud-server/` no Pi
3. Copia o JAR e o serviço systemd via SCP
4. Habilita e inicia o serviço

### 4. Acesse o dashboard

Abra no navegador:

```
http://<ip-do-raspberry>:8080
```

---

## Instalação Manual (sem script)

Se preferir fazer o deploy manualmente:

```bash
# 1. Build
./gradlew buildFatJar

# 2. Copie o JAR para o Raspberry Pi
scp build/libs/space-cloud-server.jar pi@<ip-do-raspberry>:/home/pi/space-cloud-server/

# 3. Copie o serviço systemd
scp wol-controller.service pi@<ip-do-raspberry>:/tmp/

# 4. No Raspberry Pi, instale o serviço
ssh pi@<ip-do-raspberry>
sudo cp /tmp/wol-controller.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable wol-controller
sudo systemctl start wol-controller
```

### Executar sem systemd (para testes)

```bash
java -jar space-cloud-server.jar
```

---

## Estrutura do Projeto

```
space-cloud-standalone-server/
│
├── src/main/kotlin/com/spacecloud/
│   ├── Application.kt              # Entry point — servidor Ktor (Netty :8080)
│   ├── ServerInfo.kt               # Tracking de uptime do servidor
│   ├── database/
│   │   ├── DatabaseFactory.kt      # Inicialização SQLite (WAL mode)
│   │   └── Tables.kt               # Schema: ConfigTable, WakeLogsTable
│   ├── model/
│   │   ├── Config.kt               # Data class de configuração
│   │   └── WakeLog.kt              # Data classes de log
│   ├── routes/
│   │   └── ApiRoutes.kt            # REST API endpoints
│   └── service/
│       ├── ConfigService.kt        # CRUD de configurações
│       ├── LogService.kt           # Gerenciamento de logs
│       ├── SchedulerService.kt     # Agendador em coroutine (por minuto)
│       └── WakeOnLanService.kt     # Envio de magic packets + verificação
│
├── src/main/resources/
│   ├── logback.xml                  # Configuração de logging (rolling daily)
│   └── static/                      # Frontend
│       ├── index.html
│       ├── css/style.css
│       └── js/script.js
│
├── build.gradle.kts                 # Build config (Kotlin DSL)
├── settings.gradle.kts              # Nome do projeto
├── gradle.properties                # Versões das dependências
├── deploy.sh                        # Script de deploy automatizado
├── wol-controller.service           # Arquivo de serviço systemd
└── README.md
```

---

## API REST

| Método | Endpoint           | Descrição                              |
|--------|--------------------|----------------------------------------|
| GET    | `/api/uptime`      | Retorna o uptime do servidor (ms)      |
| GET    | `/api/config`      | Retorna a configuração atual           |
| POST   | `/api/config`      | Atualiza a configuração (parcial/full) |
| POST   | `/api/test-wol`    | Dispara envio imediato de WOL          |
| GET    | `/api/logs`        | Retorna logs e estatísticas            |
| POST   | `/api/clear-logs`  | Limpa todos os logs                    |

### Exemplos

**Obter configuração:**
```bash
curl http://<ip>:8080/api/config
```

**Enviar WOL manualmente:**
```bash
curl -X POST http://<ip>:8080/api/test-wol
```

**Atualizar configuração:**
```bash
curl -X POST http://<ip>:8080/api/config \
  -H "Content-Type: application/json" \
  -d '{"ip": "192.168.1.200", "mac": "AA:BB:CC:DD:EE:FF", "hour": "08", "minute": "30", "enabled": true}'
```

---

## Gerenciamento do Serviço

```bash
# Ver status
sudo systemctl status wol-controller

# Parar o serviço
sudo systemctl stop wol-controller

# Reiniciar
sudo systemctl restart wol-controller

# Ver logs em tempo real
sudo journalctl -u wol-controller -f

# Desabilitar auto-start
sudo systemctl disable wol-controller
```

---

## Como Funciona

### Wake On LAN (Magic Packet)

O servidor constrói um **magic packet** composto por:
- 6 bytes `0xFF`
- O endereço MAC do dispositivo alvo repetido 16 vezes

O pacote é enviado via **UDP** para múltiplos destinos para maximizar a chance de entrega:

| Destino                     | Porta(s) |
|-----------------------------|----------|
| IP direto do dispositivo    | 9        |
| Broadcast global (255.255.255.255) | 7, 9, 7812 |
| Broadcast da subnet (X.X.X.255)   | 9        |

### Verificação

Após o envio, o servidor realiza **ping contínuo** ao dispositivo alvo:
- **Intervalo:** a cada 5 segundos (configurável)
- **Timeout:** até 120 segundos (configurável)
- O resultado (sucesso/falha) é registrado no banco de dados

---

## Configuração Avançada

Acessível pela aba **Configurações** no dashboard:

| Parâmetro              | Padrão  | Descrição                                  |
|------------------------|---------|--------------------------------------------|
| Ping Timeout           | 120s    | Tempo máximo de espera pela resposta        |
| Ping Interval          | 5s      | Intervalo entre tentativas de ping          |
| Max Log Entries        | 50      | Quantidade máxima de registros mantidos     |

---

## Requisitos de Rede

- O dispositivo alvo deve ter **Wake On LAN habilitado** na BIOS/UEFI
- O Raspberry Pi deve estar na **mesma rede local** (ou com broadcast routing configurado)
- A capability `CAP_NET_RAW` é necessária para envio de broadcasts UDP (já configurada no serviço systemd)

---

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.
