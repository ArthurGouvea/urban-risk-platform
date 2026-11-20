package br.com.fiap.urbanrisk.service;

import br.com.fiap.urbanrisk.dto.NovoEventoRequest;
import br.com.fiap.urbanrisk.exception.RecursoNaoEncontradoException;
import br.com.fiap.urbanrisk.exception.TransicaoInvalidaException;
import br.com.fiap.urbanrisk.ia.AvaliadorConfianca;
import br.com.fiap.urbanrisk.ia.ClassificadorRelatos;
import br.com.fiap.urbanrisk.ia.MotorPriorizacao;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.model.CicloVidaEvento;
import br.com.fiap.urbanrisk.model.HistoricoStatus;
import br.com.fiap.urbanrisk.privacidade.AnonimizadorLgpd;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import br.com.fiap.urbanrisk.repository.HistoricoStatusRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static br.com.fiap.urbanrisk.model.CicloVidaEvento.ALERTA_EMITIDO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.CONFIRMADO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.DESCARTADO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.EM_MONITORAMENTO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.EM_VALIDACAO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.ENCERRADO;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.INATIVOS;
import static br.com.fiap.urbanrisk.model.CicloVidaEvento.REPORTADO;

/**
 * Regra de negócio central: recebe eventos das três origens, aplica LGPD, roda o pipeline de IA
 * (NLP → confiança → modelo de priorização), conduz o ciclo de vida do evento (máquina de estados
 * com histórico) e expõe a lista priorizada.
 */
@Service
public class PriorizacaoRiskService {

    public static final String SENSOR_IOT = "SENSOR_IOT";
    public static final String BASE_PUBLICA = "BASE_PUBLICA";
    public static final String REGISTRO_COMUNITARIO = "REGISTRO_COMUNITARIO";

    /** Score mínimo para emitir alerta e score abaixo do qual um alerta volta a monitoramento. */
    public static final int SCORE_EMISSAO = 70;
    public static final int SCORE_MONITORAMENTO = 60;
    public static final int CONFIANCA_MINIMA = 60;

    private static final Duration JANELA_CORRELACAO = Duration.ofHours(2);
    private static final Duration PRAZO_VALIDACAO = Duration.ofHours(2);
    private static final Duration PERMANENCIA_MINIMA_ALERTA = Duration.ofMinutes(10);
    private static final Duration MONITORAMENTO_SEM_EVIDENCIA = Duration.ofMinutes(30);
    private static final Duration EXPIRACAO_BAIXA_PRIORIDADE = Duration.ofHours(6);
    private static final int LIMITE_MAXIMO = 200;

    private final AlertaRepository repository;
    private final HistoricoStatusRepository historicoRepository;
    private final ClassificadorRelatos classificador;
    private final AvaliadorConfianca avaliadorConfianca;
    private final MotorPriorizacao motor;
    private final AnonimizadorLgpd anonimizador;

    public PriorizacaoRiskService(AlertaRepository repository, HistoricoStatusRepository historicoRepository,
                                  ClassificadorRelatos classificador, AvaliadorConfianca avaliadorConfianca,
                                  MotorPriorizacao motor, AnonimizadorLgpd anonimizador) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.classificador = classificador;
        this.avaliadorConfianca = avaliadorConfianca;
        this.motor = motor;
        this.anonimizador = anonimizador;
    }

    // ------------------------------------------------------------------ consultas

    public List<AlertaRisco> listarAlertasPriorizados() {
        return listarAlertasPriorizados(null, null, null, 100);
    }

    /** Alertas ativos ordenados por prioridade (maior primeiro) e, em empate, pelos mais recentes. */
    public List<AlertaRisco> listarAlertasPriorizados(String categoria, String nivelRisco, String bairro, int limite) {
        int tamanho = Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        Sort ordem = Sort.by(Sort.Order.desc("scorePrioridade"), Sort.Order.desc("dataHora"));
        int busca = (categoria == null && nivelRisco == null && bairro == null) ? tamanho : LIMITE_MAXIMO;
        return repository.findByStatusNotIn(INATIVOS, PageRequest.of(0, busca, ordem)).stream()
                .filter(a -> categoria == null || categoria.equalsIgnoreCase(a.getCategoria()))
                .filter(a -> nivelRisco == null || nivelRisco.equalsIgnoreCase(a.getNivelRisco()))
                .filter(a -> bairro == null || bairro.equalsIgnoreCase(a.getBairro()))
                .limit(tamanho)
                .toList();
    }

    public AlertaRisco buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta " + id + " não encontrado"));
    }

    public List<HistoricoStatus> historico(Long id) {
        buscarPorId(id);
        return historicoRepository.findByAlertaIdOrderByIdAsc(id);
    }

    public ClassificadorRelatos.AnaliseTexto analisarTexto(String texto, String categoria) {
        return classificador.analisar(texto, categoria);
    }

    // ------------------------------------------------------------------ registro

    @Transactional
    public AlertaRisco registrarEvento(NovoEventoRequest req) {
        return registrarEvento(req, LocalDateTime.now());
    }

    /** Registra relato comunitário ou boletim de base pública (dataHora permite carga histórica). */
    @Transactional
    public AlertaRisco registrarEvento(NovoEventoRequest req, LocalDateTime dataHora) {
        String origem = BASE_PUBLICA.equalsIgnoreCase(req.origemDado()) ? BASE_PUBLICA : REGISTRO_COMUNITARIO;
        boolean comunitario = REGISTRO_COMUNITARIO.equals(origem);

        AlertaRisco a = new AlertaRisco();
        // LGPD: minimização e pseudonimização ANTES de persistir
        a.setTitulo(anonimizador.mascararDadosPessoais(req.titulo().trim()));
        a.setDescricao(anonimizador.mascararDadosPessoais(req.descricao().trim()));
        a.setCategoria(req.categoria().toUpperCase(Locale.ROOT));
        a.setBairro(req.bairro().trim());
        a.setLatitude(comunitario ? anonimizador.generalizarCoordenada(req.latitude()) : req.latitude());
        a.setLongitude(comunitario ? anonimizador.generalizarCoordenada(req.longitude()) : req.longitude());
        a.setOrigemDado(origem);
        a.setAutorAnonimizado(comunitario ? anonimizador.pseudonimizar(req.dispositivoId()) : "FONTE-OFICIAL");
        a.setDataHora(dataHora);
        a.setAtualizadoEm(dataHora);

        // NLP: categoria sugerida, coerência e urgência
        var analise = classificador.analisar(a.getTitulo() + ". " + a.getDescricao(), a.getCategoria());
        a.setCategoriaSugeridaIa(analise.categoriaSugerida());
        a.setUrgenciaIa(analise.probabilidadeUrgencia());
        a.setCoerenciaIa(analise.temEvidencia() ? analise.coerenciaCategoriaInformada() : null);

        return avaliarESalvar(a);
    }

    // ------------------------------------------------------------------ pipeline de IA

    /**
     * Recalcula confiança, score e nível de risco usando o estado atual da base (correlação com outros
     * eventos e com sensores), aplica as transições automáticas do ciclo de vida e salva.
     */
    @Transactional
    public AlertaRisco avaliarESalvar(AlertaRisco a) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime desde = agora.minus(JANELA_CORRELACAO);
        boolean dentroDaJanela = a.getId() != null && a.getDataHora() != null && a.getDataHora().isAfter(desde)
                && !DESCARTADO.equals(a.getStatus());
        int proprio = dentroDaJanela ? 1 : 0;

        long correlacionados = Math.max(0, repository
                .countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndDataHoraAfterAndStatusNot(
                        a.getBairro(), a.getCategoria(), desde, DESCARTADO) - proprio);
        long similaresComunitarios = Math.max(0, repository
                .countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndOrigemDadoAndDataHoraAfterAndStatusNot(
                        a.getBairro(), a.getCategoria(), REGISTRO_COMUNITARIO, desde, DESCARTADO)
                - (REGISTRO_COMUNITARIO.equals(a.getOrigemDado()) ? proprio : 0));
        long sensoresAtivos = repository.countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndOrigemDadoAndStatusNotIn(
                a.getBairro(), a.getCategoria(), SENSOR_IOT, INATIVOS)
                - (SENSOR_IOT.equals(a.getOrigemDado()) && a.getId() != null && CicloVidaEvento.ativo(a.getStatus()) ? 1 : 0);

        // 1) Confiança da origem
        boolean limiarExcedido = a.getIntensidadeAnomalia() != null && a.getIntensidadeAnomalia() >= 0.6;
        var confianca = avaliadorConfianca.avaliar(new AvaliadorConfianca.Evidencias(
                a.getOrigemDado(),
                a.getCoerenciaIa() == null ? 0.5 : a.getCoerenciaIa(),
                a.getCoerenciaIa() != null,
                similaresComunitarios,
                Math.max(0, sensoresAtivos),
                limiarExcedido,
                Boolean.TRUE.equals(a.getValidadoPorAgente())));
        a.setPercentualConfianca(confianca.percentual());
        a.setNivelConfianca(confianca.nivel());
        a.setJustificativaConfianca(String.join("\n", confianca.justificativas()));

        // 2) Priorização (modelo logístico explicável)
        LocalDateTime referencia = a.getAtualizadoEm() != null ? a.getAtualizadoEm() : a.getDataHora();
        double minutos = referencia == null ? 0 : Duration.between(referencia, agora).toSeconds() / 60.0;
        var resultado = motor.avaliar(new MotorPriorizacao.Entrada(
                a.getCategoria(),
                confianca.percentual() / 100.0,
                a.getUrgenciaIa() == null ? 0.5 : a.getUrgenciaIa(),
                a.getIntensidadeAnomalia() == null ? 0 : a.getIntensidadeAnomalia(),
                (int) correlacionados,
                minutos));

        List<String> fatores = new ArrayList<>(resultado.fatores());
        if (a.getCategoriaSugeridaIa() != null && a.getCoerenciaIa() != null
                && !a.getCategoriaSugeridaIa().equalsIgnoreCase(a.getCategoria())) {
            fatores.add("Atenção: a IA sugere a categoria " + a.getCategoriaSugeridaIa() + " para este texto");
        }

        a.setScorePrioridade(resultado.score());
        a.setProbabilidadeRiscoIa((int) Math.round(resultado.probabilidade() * 100));
        a.setNivelRisco(resultado.nivelRisco());
        a.setEventosCorrelacionados((int) correlacionados);
        a.setExplicacaoIa(String.join("\n", fatores));

        // 3) Ciclo de vida
        if (a.getStatus() == null) {
            a.setStatus(REPORTADO);
            a.setStatusAlteradoEm(agora);
            a = repository.save(a);
            registrarHistorico(a, null, REPORTADO, "Evento recebido de " + nomeOrigem(a.getOrigemDado()),
                    "SISTEMA", a.getDataHora() != null ? a.getDataHora() : agora);
        }
        aplicarTransicoesAutomaticas(a, confianca.percentual(), resultado.score(), agora);
        return repository.save(a);
    }

    /** Regras de negócio RN-07 a RN-10 (ver documentação): transições disparadas pelo sistema. */
    private void aplicarTransicoesAutomaticas(AlertaRisco a, int confianca, int score, LocalDateTime agora) {
        if (REPORTADO.equals(a.getStatus())) {
            if (confianca < CONFIANCA_MINIMA) {
                transitar(a, EM_VALIDACAO, "Confiança " + confianca + "% < " + CONFIANCA_MINIMA + "%: aguarda validação", "SISTEMA");
            } else {
                transitar(a, CONFIRMADO, "Confiança " + confianca + "% ≥ " + CONFIANCA_MINIMA + "%", "SISTEMA");
            }
        } else if (EM_VALIDACAO.equals(a.getStatus()) && confianca >= CONFIANCA_MINIMA) {
            transitar(a, CONFIRMADO, "Validação cruzada elevou a confiança para " + confianca + "%", "SISTEMA");
        }

        if (CONFIRMADO.equals(a.getStatus()) && score >= SCORE_EMISSAO) {
            transitar(a, ALERTA_EMITIDO, "Prioridade " + score + " ≥ " + SCORE_EMISSAO + ": alerta emitido aos usuários", "IA");
        } else if (ALERTA_EMITIDO.equals(a.getStatus()) && score < SCORE_MONITORAMENTO
                && a.getStatusAlteradoEm() != null && a.getStatusAlteradoEm().isBefore(agora.minus(PERMANENCIA_MINIMA_ALERTA))) {
            transitar(a, EM_MONITORAMENTO, "Prioridade caiu para " + score + ": em monitoramento", "SISTEMA");
        }
    }

    /** Aplica uma transição validada pela máquina de estados e grava o histórico. */
    @Transactional
    public void transitar(AlertaRisco a, String destino, String motivo, String responsavel) {
        String origem = a.getStatus();
        if (!CicloVidaEvento.podeTransitar(origem, destino)) {
            throw new TransicaoInvalidaException("Transição inválida: " + origem + " → " + destino);
        }
        LocalDateTime agora = LocalDateTime.now();
        a.setStatus(destino);
        a.setStatusAlteradoEm(agora);
        if (a.getId() == null) a = repository.save(a);
        registrarHistorico(a, origem, destino, motivo, responsavel, agora);
    }

    /** Ação manual de agente da Defesa Civil / operador de serviço (RF-09). */
    @Transactional
    public AlertaRisco executarAcao(Long id, CicloVidaEvento.Acao acao, String motivo, String perfil) {
        AlertaRisco a = buscarPorId(id);
        if (!CicloVidaEvento.podeTransitar(a.getStatus(), acao.destino)) {
            throw new TransicaoInvalidaException("Ação " + acao + " não permitida no estado " + a.getStatus()
                    + ". Ações possíveis: " + CicloVidaEvento.acoesDisponiveis(a.getStatus()));
        }
        if (acao == CicloVidaEvento.Acao.CONFIRMAR) a.setValidadoPorAgente(true);
        String responsavel = "AGENTE: " + (perfil == null || perfil.isBlank() ? "Defesa Civil" : perfil.trim());
        transitar(a, acao.destino, motivo == null || motivo.isBlank() ? acao.motivoPadrao : motivo.trim(), responsavel);
        return CicloVidaEvento.ativo(a.getStatus()) ? avaliarESalvar(a) : repository.save(a);
    }

    /**
     * Reavaliação periódica (RN-10): decaimento por recência, novas correlações, descarte de relatos
     * não confirmados, encerramento de monitoramentos sem novas evidências e de eventos antigos.
     */
    @Scheduled(fixedDelayString = "${urbanrisk.reavaliacao.intervalo-ms:30000}", initialDelay = 15000)
    @Transactional
    public void reavaliarAlertasAtivos() {
        LocalDateTime agora = LocalDateTime.now();
        for (AlertaRisco a : repository.findByStatusNotIn(INATIVOS)) {
            LocalDateTime ref = a.getAtualizadoEm() != null ? a.getAtualizadoEm() : a.getDataHora();
            LocalDateTime mudanca = a.getStatusAlteradoEm() != null ? a.getStatusAlteradoEm() : ref;
            if (EM_VALIDACAO.equals(a.getStatus()) && a.getDataHora() != null
                    && a.getDataHora().isBefore(agora.minus(PRAZO_VALIDACAO))) {
                transitar(a, DESCARTADO, "Não confirmado por outras fontes em 2 h", "SISTEMA");
                repository.save(a);
            } else if (EM_MONITORAMENTO.equals(a.getStatus()) && !SENSOR_IOT.equals(a.getOrigemDado())
                    && mudanca != null && mudanca.isBefore(agora.minus(MONITORAMENTO_SEM_EVIDENCIA))) {
                transitar(a, ENCERRADO, "30 min em monitoramento sem novas evidências", "SISTEMA");
                repository.save(a);
            } else if (ref != null && ref.isBefore(agora.minus(EXPIRACAO_BAIXA_PRIORIDADE))
                    && a.getScorePrioridade() != null && a.getScorePrioridade() < 45
                    && CicloVidaEvento.podeTransitar(a.getStatus(), ENCERRADO)) {
                transitar(a, ENCERRADO, "Evento antigo de baixa prioridade", "SISTEMA");
                repository.save(a);
            } else {
                avaliarESalvar(a);
            }
        }
    }

    private void registrarHistorico(AlertaRisco a, String de, String para, String motivo, String responsavel,
                                    LocalDateTime quando) {
        historicoRepository.save(new HistoricoStatus(a.getId(), de, para, motivo, responsavel, quando));
    }

    private static String nomeOrigem(String origem) {
        if (SENSOR_IOT.equals(origem)) return "sensor IoT";
        if (BASE_PUBLICA.equals(origem)) return "base pública";
        return "registro comunitário";
    }
}
