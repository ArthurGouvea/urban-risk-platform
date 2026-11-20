package br.com.fiap.urbanrisk.simulador;

import br.com.fiap.urbanrisk.dto.LeituraSensorRequest;
import br.com.fiap.urbanrisk.ia.DetectorAnomalias;
import br.com.fiap.urbanrisk.model.TipoSensor;
import br.com.fiap.urbanrisk.service.IngestaoSensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Rede de sensores simulada de São Paulo. A cada ciclo, cada sensor envia uma leitura com ruído
 * normal; ocasionalmente começa um "episódio" (chuva forte, onda de calor, queda de energia...)
 * que dura alguns ciclos. As leituras passam pelo MESMO pipeline do endpoint de sensores reais.
 * Desative com urbanrisk.simulador.habilitado=false.
 */
@Component
public class SimuladorSensores {

    private static final Logger log = LoggerFactory.getLogger(SimuladorSensores.class);

    record SensorVirtual(String id, TipoSensor tipo, String bairro, double lat, double lon,
                         double base, double ruido, double eventoMin, double eventoMax) {}

    private final List<SensorVirtual> sensores = List.of(
            new SensorVirtual("PLUV-CAMBUCI-01", TipoSensor.PLUVIOMETRO, "Cambuci", -23.5614, -46.6253, 2.0, 1.2, 35, 75),
            new SensorVirtual("PLUV-PINHEIROS-02", TipoSensor.PLUVIOMETRO, "Pinheiros", -23.5672, -46.7019, 1.5, 1.0, 32, 65),
            new SensorVirtual("NIVEL-TAMANDUATEI-03", TipoSensor.NIVEL_CORREGO, "Mooca", -23.5580, -46.6010, 120, 6, 210, 290),
            new SensorVirtual("TERM-BRAS-11", TipoSensor.TERMOMETRO, "Brás", -23.5453, -46.6164, 29.5, 0.7, 36.5, 40.5),
            new SensorVirtual("TERM-SE-12", TipoSensor.TERMOMETRO, "Sé", -23.5503, -46.6339, 28.5, 0.7, 36.2, 39.5),
            new SensorVirtual("ENERG-VMARIANA-21", TipoSensor.TENSAO_REDE, "Vila Mariana", -23.5895, -46.6347, 127, 1.5, 0, 85),
            new SensorVirtual("PRESS-MOOCA-31", TipoSensor.PRESSAO_AGUA, "Mooca", -23.5605, -46.5973, 2.6, 0.07, 0.2, 0.8)
    );

    private final IngestaoSensorService ingestao;
    private final DetectorAnomalias detector;
    private final boolean habilitado;
    private final double probabilidadeEvento;
    private final Random random = new Random();
    private final Map<String, Integer> episodioRestante = new HashMap<>();
    private final Map<String, Double> episodioValor = new HashMap<>();
    private boolean aquecido;

    public SimuladorSensores(IngestaoSensorService ingestao, DetectorAnomalias detector,
                             @Value("${urbanrisk.simulador.habilitado:true}") boolean habilitado,
                             @Value("${urbanrisk.simulador.probabilidade-evento:0.02}") double probabilidadeEvento) {
        this.ingestao = ingestao;
        this.detector = detector;
        this.habilitado = habilitado;
        this.probabilidadeEvento = probabilidadeEvento;
    }

    @Scheduled(fixedRateString = "${urbanrisk.simulador.intervalo-ms:4000}", initialDelay = 3000)
    public void gerarLeituras() {
        if (!habilitado) return;
        if (!aquecido) aquecerLinhaDeBase();

        for (SensorVirtual s : sensores) {
            try {
                ingestao.registrarLeitura(new LeituraSensorRequest(s.id(), s.tipo(), s.bairro(), s.lat(), s.lon(), proximoValor(s)));
            } catch (RuntimeException e) {
                log.warn("Falha ao registrar leitura simulada de {}: {}", s.id(), e.getMessage());
            }
        }
    }

    private double proximoValor(SensorVirtual s) {
        int restante = episodioRestante.getOrDefault(s.id(), 0);
        if (restante == 0 && episodioRestante.size() < 2 && random.nextDouble() < probabilidadeEvento) {
            restante = 6 + random.nextInt(12); // episódio de ~25 s a ~70 s
            episodioValor.put(s.id(), s.eventoMin() + random.nextDouble() * (s.eventoMax() - s.eventoMin()));
            log.info("Simulador: iniciando episódio em {} ({} ciclos)", s.id(), restante);
        }
        double valor;
        if (restante > 0) {
            valor = episodioValor.get(s.id()) + random.nextGaussian() * s.ruido() * 2;
            restante--;
            if (restante == 0) {
                episodioRestante.remove(s.id());
            } else {
                episodioRestante.put(s.id(), restante);
            }
        } else {
            valor = s.base() + random.nextGaussian() * s.ruido();
        }
        return Math.max(0, valor);
    }

    /** Carrega 30 leituras normais por sensor na linha de base do detector (sem gravar no banco). */
    private void aquecerLinhaDeBase() {
        for (SensorVirtual s : sensores) {
            List<Double> valores = new ArrayList<>();
            for (int i = 0; i < 30; i++) valores.add(Math.max(0, s.base() + random.nextGaussian() * s.ruido()));
            detector.aquecer(s.id(), valores);
        }
        aquecido = true;
    }
}
