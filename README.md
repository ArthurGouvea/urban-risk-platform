# \# Urban Risk Intelligence Platform (CitizenTech - FIAP 2026.2)

# 

# Plataforma integrada de monitoramento e análise de riscos cotidianos em áreas urbanas (alagamentos, ilhas de calor e interrupções de serviços essenciais), composta por API REST em \*\*Java + Spring Boot\*\* e aplicativo mobile em \*\*React Native (Expo)\*\*.

# 

# \## Integrantes do Grupo

# \- \*\*Juliana Barbosa Sandes\*\* - RM: 555605

# \- \*\*Arthur Macedo Gouvea\*\* - RM: 556499

# \- \*\*Clara Jullia Kondrasovas Costa e Silva\*\* - RM: 556064

# \## Arquitetura e Funcionalidades

# \- \*\*Backend (`/backend`):\*\* Arquitetura em camadas (`model`, `repository`, `service`, `controller`, `config`), persistência em banco H2, motor heurístico de priorização por severidade e fonte do dado (`SENSOR\_IOT`, `BASE\_PUBLICA`, `REGISTRO\_COMUNITARIO`), validação cruzada comunitária e anonimização de identificadores conforme a LGPD.

# \- \*\*Frontend Mobile (`/mobile`):\*\* 8 telas navegáveis (Boas-vindas/LGPD, Painel Geral, Alertas Priorizados, Detalhe com Auditoria de Origem/Confiança, Reportar Ocorrência, Central de Notificações, Transparência de Fontes e Perfil), atualização em tempo real e modo offline resiliente via `AsyncStorage`.

# 

# \## Como Executar o Projeto

# 

# \### 1. Backend (Spring Boot - Porta 8080)

# ```bash

# cd backend

# ./mvnw spring-boot:run

