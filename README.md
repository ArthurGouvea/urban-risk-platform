# Urban Risk Intelligence Platform (CitizenTech – FIAP 2026.2)

Plataforma integrada de monitoramento e análise de riscos cotidianos em áreas urbanas — **alagamentos, ilhas de calor e interrupções de serviços essenciais (energia, água e transporte)** — composta por:

- **Backend** em **Java 17 + Spring Boot 4** (`/backend`): recebe dados de **sensores IoT** (reais ou simulados), **bases públicas** e **registros comunitários**, persiste tudo e expõe uma **API REST que prioriza os alertas com Inteligência Artificial**, informando a **origem** e o **nível de confiança** de cada dado.
- **App mobile** em **React Native (Expo SDK 58)** (`/mobile`): 9 telas com painel de acompanhamento, mapa de riscos, lista priorizada, detalhe com origem/confiança e linha do tempo, registro comunitário, central de notificações e modo offline resiliente, para três personas: **morador**, **agente da Defesa Civil** e **operador de serviço público**.

## Integrantes do grupo

| Nome completo | RM |
|---|---|
| Juliana Barbosa Sandes | 555605 |
| Arthur Macedo Gouvea | 556499 |
| Clara Jullia Kondrasovas Costa e Silva | 556064 |

---
## Videos de Demonstração
| São sobre o mesmo projeto, porêm atentendo requisitos diferentes.
| Engenharia de Software: https://youtu.be/3eyIKxUyb3M
| Dev Mobile: https://youtu.be/PNPv_qcBhgg


## Arquitetura

```
 Sensores IoT ──► POST /api/sensores/leituras ─┐        (simulador interno usa o mesmo serviço)
 Bases públicas ─► POST /api/alertas ──────────┤
 Moradores (app) ─► POST /api/alertas ─────────┤
                                               ▼
        ┌──────────────── Backend Spring Boot (camadas) ─────────────────┐
        │ controller → service → repository → model (JPA/H2 ou Postgres)│
        │                    │                                           │
        │                    ▼                                           │
        │   ia/  NLP Naive Bayes · Detector de anomalias · Regressão     │
        │        logística explicável · Avaliador de confiança           │
        │   privacidade/  pseudonimização e mascaramento (LGPD)          │
        └───────────────────────────────┬────────────────────────────────┘
                                        │ GET /api/alertas (já priorizado)
                                        ▼
        App React Native: services/api → context (sync 3 s + cache offline) → screens
```

**Backend** (`br.com.fiap.urbanrisk`):

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Apresentação | `controller`, `exception` | Endpoints REST, validação de entrada (`@Valid`) e respostas de erro padronizadas (400/404) |
| Negócio | `service` | Pipeline de priorização, ingestão de sensores, painel, reavaliação periódica |
| IA | `ia` | Modelos de IA em Java puro (sem dependências externas) |
| Privacidade | `privacidade` | LGPD: pseudonimização, mascaramento de dados pessoais, generalização de localização |
| Persistência | `repository`, `model` | Spring Data JPA (H2 em memória; perfil `postgres` para produção) |
| Infra | `config`, `simulador` | Beans de IA, CORS, carga inicial e simulador de sensores |
| Segurança | `seguranca` | Controle de acesso das ações de agente/operador (`X-Token-Agente`) |
| Entrada | `dto` | Records de requisição — o cliente **não** consegue enviar score/confiança |

**Mobile** (`/mobile/src`): `services` (API e notificações) → `context` (estado global, sincronização, fila offline) → `screens` (8 telas) + `components` e `utils` (funções puras testadas).

---

## Inteligência Artificial para priorização dos alertas

Toda a IA roda **dentro do backend**, sem APIs externas (funciona offline e sem custo), e é **explicável**: cada alerta traz a lista de fatores que justificam sua prioridade (`fatoresIa`) e de evidências que justificam sua confiança (`justificativasConfianca`).

### 1. NLP dos relatos comunitários — Naive Bayes multinomial
- Implementado do zero (`ia/NaiveBayesMultinomial.java`), com suavização de Laplace, remoção de acentos/stopwords e stemming por prefixo.
- Treinado na inicialização com um corpus rotulado de **123 relatos** (`resources/ia/corpus-relatos.csv`).
- Dois modelos: **categoria** (alagamento, ilha de calor, falta de energia, falta de água, transporte) e **urgência** (alta/baixa: risco à vida, pessoas vulneráveis, evolução rápida).
- **Qualidade medida por validação cruzada leave-one-out**: ~**85%** de acurácia na categoria e ~**80%** na urgência (exposto em `GET /api/ia/modelo`).
- Usos: sugerir a categoria correta no app enquanto o morador digita, medir a **coerência** entre texto e categoria escolhida (afeta a confiança) e estimar a **urgência** (afeta a prioridade).

### 2. Detecção de anomalias em sensores — z-score em janela móvel
- Para cada sensor, média e desvio-padrão das últimas 60 leituras normais; anomalia quando |z| ≥ 3,5 no sentido do risco (chuva/temperatura/nível **subindo**, tensão/pressão **caindo**) ou quando um **limiar físico** é excedido (ex.: chuva > 30 mm/h).
- Leituras anômalas não entram na linha de base (um evento longo não "se normaliza").
- **Redução de falsos positivos**: o alerta só é aberto após **2 leituras anômalas seguidas** (ou limiar físico excedido); é **encerrado automaticamente** depois de 2 min de leituras normais.

### 3. Priorização — regressão logística explicável

```
logit = -4,2 + 2,2·severidade + 2,0·confiança + 1,6·urgência(NLP) + 1,8·anomalia(sensor)
             + 1,2·correlação(eventos no bairro em 2 h) + 1,0·recência(meia-vida 120 min)
probabilidade = 1 / (1 + e^-logit)      score = 100 · probabilidade
CRÍTICO ≥ 85 · ALTO ≥ 70 · MODERADO ≥ 45 · BAIXO < 45
```

Os alertas ativos são **reavaliados a cada 30 s** (decaimento por recência + novas correlações — ex.: um relato comunitário é confirmado quando um sensor do mesmo bairro detecta anomalia).

### Origem e nível de confiança de cada dado

| Origem (`origemDado`) | Confiança | Regra |
|---|---|---|
| `SENSOR_IOT` | 90–95% | telemetria automática; +5% se limiar físico excedido |
| `BASE_PUBLICA` | 95% | boletins oficiais (Defesa Civil, concessionárias) |
| `REGISTRO_COMUNITARIO` | 30–98% | base 50%; +10% por relato semelhante no bairro (até 3); +20% se um sensor do bairro confirma; +10% texto coerente (NLP); −15% texto incoerente |

Nível: **ALTA** ≥ 80%, **MÉDIA** ≥ 60%, **BAIXA** < 60% (relatos com confiança < 60% ficam **EM_VALIDAÇÃO** — fila de trabalho do agente). Validação em campo por agente garante no mínimo 90%.

### Ciclo de vida do evento (máquina de estados)

`REPORTADO → EM_VALIDACAO → CONFIRMADO → ALERTA_EMITIDO → EM_MONITORAMENTO → ENCERRADO` (ou `DESCARTADO`)

- Transições **automáticas**: confiança ≥ 60% confirma; score ≥ 70 emite alerta; sensor normalizado por 2 min → monitoramento, +3 min → encerrado; anomalia que volta → alerta reemitido; relato sem confirmação em 2 h → descartado.
- Transições **manuais** (agente da Defesa Civil / operador): `CONFIRMAR`, `DESCARTAR`, `EMITIR_ALERTA`, `MONITORAR`, `ENCERRAR` — só as permitidas pelo estado atual (senão HTTP 409).
- Cada transição fica no **histórico** (data, motivo e responsável), exibido como linha do tempo no app. Diagrama: [`docs/diagramas/estados.png`](docs/diagramas/estados.png).

---

## Pré-requisitos

- **Java 17+** (o Maven Wrapper `mvnw` baixa o Maven automaticamente)
- **Node.js 20.19+ ou 22+** e npm
- App **Expo Go** no celular (ou emulador Android / simulador iOS / navegador)
- Celular e computador **na mesma rede Wi-Fi** (para testar em aparelho físico)

## Como executar

### 1. Backend (Spring Boot – porta 8080)

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Ao subir, o backend carrega 7 eventos iniciais (das 3 origens) e liga o **simulador de sensores**, que envia leituras a cada 4 s e, de vez em quando, gera episódios de chuva forte, calor extremo, queda de energia ou de pressão da água — então novos alertas aparecem sozinhos durante a demonstração.

- Teste rápido: http://localhost:8080/api/alertas
- Saúde (disponibilidade): http://localhost:8080/actuator/health
- Console do banco H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:urbanriskdb`, usuário `sa`, sem senha)
- Testes automatizados: `./mvnw test`

Configurações úteis (`application.properties`): `urbanrisk.simulador.habilitado`, `urbanrisk.simulador.intervalo-ms`, `urbanrisk.simulador.probabilidade-evento` (aumente para mais eventos na demo, ex.: `0.05`).

PostgreSQL (produção): `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres` com `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### 2. App mobile (Expo)

```bash
cd mobile
npm install
npx expo start
```

- **Celular (Expo Go)**: leia o QR code. O app **detecta sozinho** o IP do computador que roda o Expo e usa `http://<IP>:8080/api`.
- **Emulador Android**: tecle `a` (usa `http://10.0.2.2:8080/api`).
- **Navegador**: tecle `w` (usa `http://localhost:8080/api`).
- Se precisar fixar o endereço: crie `mobile/.env` com `EXPO_PUBLIC_API_URL=http://SEU_IP:8080/api` (veja `.env.example`) **ou** altere em **Perfil → Endereço da API → Testar e salvar**.
- Testes das funções de negócio do app: `npm test`

> Se o celular não conectar, libere a porta 8080 no firewall do computador e confirme que os dois estão na mesma rede.

---

## Endpoints da API

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/health` | Status do servidor |
| GET | `/api/alertas?categoria=&nivelRisco=&bairro=&limite=` | Alertas ativos **já priorizados** (maior score primeiro), com origem, confiança e fatores da IA |
| GET | `/api/alertas/{id}` | Detalhe de um alerta (404 se não existir) |
| POST | `/api/alertas` | Relato comunitário (201) ou boletim de base pública (exige `X-Token-Agente`). Score/confiança/estado são sempre calculados no servidor |
| GET | `/api/alertas/{id}/historico` | Linha do tempo do evento (transições de estado) |
| POST | `/api/alertas/{id}/acoes` | Ação de agente/operador `{acao, motivo, perfil}` — exige `X-Token-Agente` (401/409) |
| POST | `/api/sensores/leituras` | Ingestão de leitura de sensor IoT real |
| GET | `/api/sensores/leituras/recentes` | Últimas 30 leituras (feed em tempo real) |
| GET | `/api/painel/resumo` | Contagens por nível, categoria e origem |
| POST | `/api/ia/analisar-texto` | Classifica um texto (categoria, coerência, urgência) |
| GET | `/api/ia/modelo` | Transparência: algoritmos, pesos e acurácia medida |
| GET | `/actuator/health` | Health check para orquestradores / load balancer |

Exemplo de relato:

```bash
curl -X POST http://localhost:8080/api/alertas -H "Content-Type: application/json" -d '{
  "titulo": "Rua alagada", "descricao": "Água entrando nas casas, idosos precisando de ajuda",
  "categoria": "ALAGAMENTO", "bairro": "Cambuci", "origemDado": "REGISTRO_COMUNITARIO", "dispositivoId": "demo-1"}'
```

Exemplo de leitura de sensor real:

```bash
curl -X POST http://localhost:8080/api/sensores/leituras -H "Content-Type: application/json" -d '{
  "sensorId": "PLUV-LAPA-07", "tipo": "PLUVIOMETRO", "bairro": "Lapa", "latitude": -23.52, "longitude": -46.70, "valor": 52.0}'
```

---

## App: telas e funcionalidades

**Perfis (personas):** 🏠 Morador · 🦺 Agente da Defesa Civil · 🛠️ Operador de Serviço Público. Os dois últimos pedem o **código de acesso** — na demonstração: `defesa-civil-2026` (no backend: variável `URBANRISK_TOKEN_AGENTE`).

1. **T1 Boas-vindas / LGPD** – escolha de perfil, código de acesso (perfis institucionais) e consentimento explicado em linguagem simples.
2. **T2 Painel** – contagem por nível, **mapa de riscos** resumido, *prioridades agora*, **sensores em tempo real**, zonas por bairro e **fila de validação** para agente/operador.
3. **T3 Mapa de riscos** – eventos posicionados por coordenada (cor = nível, tamanho = prioridade); toque abre o evento.
4. **T4 Alertas** – lista priorizada com filtros por categoria, nível, situação (estado) e bairro.
5. **T5 Detalhe** – **origem e nível de confiança** com justificativas, “por que esta prioridade?” (IA), **linha do tempo** do ciclo de vida, **ações do agente** (confirmar, descartar, emitir, monitorar, encerrar), sensor e localização.
6. **T6 Reportar** – registro comunitário com **análise da IA em tempo real**; operador pode **publicar boletim oficial**; offline vai para a fila.
7. **T7 Avisos (central de notificações)** – notificações recebidas e alertas urgentes ativos.
8. **T8 Fontes, confiança e IA** – regras de confiança por origem, ciclo de vida e métricas dos modelos.
9. **T9 Perfil** – perfil, código de acesso, notificações, endereço da API, estado da conexão e revogação LGPD.

**Notificações de alerta**: quando surge um alerta **ALTO/CRÍTICO** novo ou um alerta **escala para CRÍTICO**, o app mostra um aviso no topo da tela (toque abre o detalhe), vibra e dispara uma **notificação local** do sistema (Android/iOS via `expo-notifications`).

**Acessibilidade**: textos ≥ 13 px, áreas de toque ≥ 44 px, `accessibilityLabel`/`accessibilityRole` em cartões e botões, avisos de conexão em *live region* (lidos pelo leitor de tela), nível de risco indicado por **cor + texto** (não só cor) e rótulos em português claro.

---

## Roteiro da demonstração (Global Solution)

1. **API rodando**: `./mvnw spring-boot:run` e abrir http://localhost:8080/api/alertas e http://localhost:8080/api/ia/modelo.
2. **App recebendo em tempo real**: abrir o app; a faixa verde mostra “Online • atualizado agora • NN ms”. Os valores dos sensores mudam a cada poucos segundos; quando o simulador gera um episódio, o alerta aparece e chega a notificação. Para forçar, envie o `curl` de leitura de sensor acima (52 mm/h) ou um relato pelo próprio app.
3. **IA**: abrir um alerta e mostrar origem/confiança e os fatores; na aba Reportar, digitar “fio caído soltando faíscas” com a categoria Alagamento e mostrar a sugestão da IA.
4. **Agente da Defesa Civil**: em Perfil, trocar para “Agente da Defesa Civil” e salvar o código `defesa-civil-2026`; no Painel, tocar na fila de validação, abrir um relato “Em validação” e tocar em **Confirmar** — a linha do tempo mostra a transição e a confiança sobe para 90%.
5. **Backend parado**: `Ctrl+C` no terminal do backend. Em ~2,5 s a faixa fica vermelha: “SEM CONEXÃO COM O SERVIDOR”, com o horário dos dados exibidos (cache local). O app **não trava**: dá para navegar, abrir detalhes e até registrar um relato (vai para a fila).
6. **Reconexão**: subir o backend de novo. O app tenta reconectar a cada 3 s; ao voltar, mostra “✅ Reconectado!”, atualiza os dados e envia sozinho os relatos da fila.

## Indicadores

| Indicador | Como a solução atende |
|---|---|
| **Tempo de atualização** | Sincronização a cada 3 s, sem requisições sobrepostas; latência medida e exibida na faixa de status; queda detectada em ≤ 2,5 s (timeout) e reconexão em ≤ 3 s |
| **Qualidade dos alertas** | Priorização por IA explicável; confiança por origem com validação cruzada; anomalias confirmadas por persistência; encerramento automático; acurácia do NLP medida por validação cruzada |
| **Facilidade de uso** | 5 abas, linguagem simples, filtros por toque, sugestões da IA no formulário, acessibilidade, perfis Morador/Agente |

## Escalabilidade, disponibilidade e proteção de dados

- **Escalabilidade**: as requisições do app são *stateless* (várias instâncias podem rodar atrás de um balanceador; a linha de base do detector de anomalias fica em memória por instância — em produção, mover para Redis ou distribuir os sensores por instância), listagem paginada/limitada (`limite` ≤ 200), retenção de leituras brutas de 2 h, banco trocável para PostgreSQL por perfil, IA leve (sem GPU nem serviço externo).
- **Disponibilidade**: `/actuator/health` para monitoramento; no app, **cache local**, fila de relatos offline e reconexão automática; erros de notificação nunca derrubam o app.
- **LGPD**:
  - nenhum dado pessoal é pedido (nem nome, CPF, telefone ou e-mail);
  - **pseudonimização**: o id aleatório da instalação vira `USR-` + SHA-256 com *salt* (configurável por variável de ambiente `URBANRISK_LGPD_SALT`);
  - **minimização**: e-mails, CPFs, telefones e placas digitados nos textos são **mascarados antes de salvar**;
  - **generalização**: coordenadas de relatos comunitários arredondadas (~110 m);
  - **direito de revogação**: o app apaga cache, fila e identificador local ao revogar o consentimento;
  - o cliente não consegue alterar campos calculados (score, confiança, status).
  - Em produção: HTTPS, restringir CORS e trocar o *salt*.

## Testes

- **Backend** (`./mvnw test`): testes unitários da IA (classificador, detector de anomalias, modelo de priorização, avaliador de confiança), da máquina de estados, da LGPD e testes de integração (ciclo de vida, ações de agente, 401/409) do pipeline completo e da API REST (servidor em porta aleatória).
- **Mobile** (`npm test`): detecção de alertas novos/escalados para notificação, datas, agrupamento por bairro.

## Estrutura

```
urban-risk-platform/
├── backend/
│   ├── src/main/java/br/com/fiap/urbanrisk/
│   │   ├── config/  controller/  dto/  exception/  ia/  model/
│   │   ├── privacidade/  repository/  seguranca/  service/  simulador/
│   ├── src/main/resources/ (application*.properties, ia/corpus-relatos.csv)
│   └── src/test/java/...  (unitários + integração)
├── docs/                      (PDF da documentação, diagramas UML, telas)
└── mobile/
    ├── App.js                 (navegação)
    ├── src/ config.js · constants.js · navegacao.js
    │   ├── services/ (api.js, notificacoes.js)
    │   ├── context/  (RiskContext.js – sincronização, offline, notificações)
    │   ├── components/ · screens/ · utils/
    └── __tests__/
```
