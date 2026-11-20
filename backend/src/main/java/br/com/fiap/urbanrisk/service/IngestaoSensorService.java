package br.com.fiap.urbanrisk.service;

import br.com.fiap.urbanrisk.dto.LeituraSensorRequest;
import br.com.fiap.urbanrisk.ia.DetectorAnomalias;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.model.CicloVidaEvento;
import br.com.fiap.urbanrisk.model.LeituraSensor;
import br.com.fiap.urbanrisk.model.TipoSensor;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import br.com.fiap.urbanrisk.repository.LeituraSensorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recebe leituras de sensores (reais via API ou simuladas), detecta anomalias e abre/atualiza/encerra
 * alertas automaticamente. Para reduzir falsos positivos, um alerta só é aberto após 2 leituras
 * anômalas consecutivas, ou imediatamente quando um limiar físico de segurança é excedido.
 * Ciclo de vida: leituras normais por 2 min levam um ALERTA_EMITIDO a EM_MONITORAMENTO; mais 3 min
 * normais encerram o evento; se a anomalia voltar durante o monitoramento, o alerta é reemitido.
 */
@Service
public class IngestaoSensorService {

    private static final int LEITURAS_PARA_CONFIRMAR = 2;
    private static final Duration TEMPO_PARA_MONITORAR = Duration.ofMinutes(2);
    private static final Duration TEMPO_PARA_ENCERRAR = Duration.ofMinutes(3);
    private static final Duration RETENCAO_LEITURAS = Duration.ofHours(2);

    private final LeituraSensorRepository leituraRepository;
    private final AlertaRepository alertaRepository;
    private final DetectorAnomalias detector;
    private final PriorizacaoRiskService priorizacao;
    private final Map<String, Integer> anomaliasConsecutivas = new ConcurrentHashMap<>();

    public IngestaoSensorService(LeituraSensorRepository leituraRepository, AlertaRepository alertaRepository,
                                 DetectorAnomalias detector, PriorizacaoRiskService priorizacao) {
        this.leituraRepository = leituraRepository;
        this.alertaRepository = alertaRepository;
        this.detector = detector;
        this.priorizacao = priorizacao;
    }

    @Transactional
    public LeituraSensor registrarLeitura(LeituraSensorRequest req) {
        TipoSensor tipo = req.tipo();
        LocalDateTime agora = LocalDateTime.now();
        var r = detector.avaliar(req.sensorId(), req.valor(), tipo.sentidoRisco, tipo.limiar, tipo.desvioMinimo);

        LeituraSensor leitura = new LeituraSensor();
        leitura.setSensorId(req.sensorId());
        leitura.setTipo(tipo.name());
        leitura.setBairro(req.bairro());
        leitura.setLatitude(req.latitude());
        leitura.setLongitude(req.longitude());
        leitura.setValor(arredondar(req.valor()));
        leitura.setUnidade(tipo.unidade);
        leitura.setEscoreZ(arredondar(r.zScore()));
        leitura.setAnomalia(r.anomalia());
        leitura.setDataHora(agora);
        leituraRepository.save(leitura);

        var alertaAtivo = alertaRepository.findFirstBySensorIdAndStatusNotInOrderByDataHoraDesc(
                req.sensorId(), CicloVidaEvento.INATIVOS);

        if (r.anomalia()) {
            int seguidas = anomaliasConsecutivas.merge(req.sensorId(), 1, Integer::sum);
            boolean confirmada = seguidas >= LEITURAS_PARA_CONFIRMAR || r.limiarExcedido();
            if (alertaAtivo.isPresent() || confirmada) {
                AlertaRisco a = alertaAtivo.orElseGet(() -> novoAlerta(req, tipo, agora));
                // mantém a maior intensidade observada no episódio
                double intensidade = Math.max(r.intensidade(), a.getIntensidadeAnomalia() == null ? 0 : a.getIntensidadeAnomalia());
                a.setIntensidadeAnomalia(intensidade);
                a.setValorMedido(arredondar(req.valor()));
                a.setEscoreZ(arredondar(r.zScore()));
                a.setAtualizadoEm(agora);
                a.setTitulo(String.format(Locale.ROOT, "%s (%.1f %s)", tipo.tituloAlerta, req.valor(), tipo.unidade));
                a.setDescricao(String.format(Locale.ROOT,
                        "Sensor %s registrou %.1f %s, %.1f desvios-padrão %s da média recente (%.1f %s)%s.",
                        req.sensorId(), req.valor(), tipo.unidade, Math.abs(r.zScore()),
                        r.zScore() >= 0 ? "acima" : "abaixo", r.media(), tipo.unidade,
                        r.limiarExcedido() ? ". Limiar de segurança excedido" : ""));
                if (CicloVidaEvento.EM_MONITORAMENTO.equals(a.getStatus())) {
                    priorizacao.transitar(a, CicloVidaEvento.ALERTA_EMITIDO, "Anomalia voltou a ser detectada pelo sensor", "IA");
                }
                priorizacao.avaliarESalvar(a);
            }
        } else {
            anomaliasConsecutivas.remove(req.sensorId());
            alertaAtivo.ifPresent(a -> {
                LocalDateTime ultimaAnomalia = a.getAtualizadoEm() != null ? a.getAtualizadoEm() : a.getDataHora();
                String status = a.getStatus();
                if (CicloVidaEvento.ALERTA_EMITIDO.equals(status) && ultimaAnomalia.isBefore(agora.minus(TEMPO_PARA_MONITORAR))) {
                    priorizacao.transitar(a, CicloVidaEvento.EM_MONITORAMENTO, "Leituras normalizadas há 2 min", "SISTEMA");
                    alertaRepository.save(a);
                } else if ((CicloVidaEvento.EM_MONITORAMENTO.equals(status) || CicloVidaEvento.CONFIRMADO.equals(status))
                        && ultimaAnomalia.isBefore(agora.minus(TEMPO_PARA_MONITORAR.plus(TEMPO_PARA_ENCERRAR)))) {
                    priorizacao.transitar(a, CicloVidaEvento.ENCERRADO, "Leituras normais por 5 min: evento encerrado", "SISTEMA");
                    alertaRepository.save(a);
                }
            });
        }
        return leitura;
    }

    public List<LeituraSensor> leiturasRecentes() {
        return leituraRepository.findTop30ByOrderByDataHoraDesc();
    }

    /** Retenção mínima de dados brutos (escalabilidade + minimização de dados). */
    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    @Transactional
    public void limparLeiturasAntigas() {
        leituraRepository.deleteByDataHoraBefore(LocalDateTime.now().minus(RETENCAO_LEITURAS));
    }

    private AlertaRisco novoAlerta(LeituraSensorRequest req, TipoSensor tipo, LocalDateTime agora) {
        AlertaRisco a = new AlertaRisco();
        a.setCategoria(tipo.categoria);
        a.setBairro(req.bairro());
        a.setLatitude(req.latitude());
        a.setLongitude(req.longitude());
        a.setOrigemDado(PriorizacaoRiskService.SENSOR_IOT);
        a.setSensorId(req.sensorId());
        a.setAutorAnonimizado(req.sensorId());
        a.setUnidadeMedida(tipo.unidade);
        a.setUrgenciaIa(0.5);
        a.setDataHora(agora);
        return a;
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
