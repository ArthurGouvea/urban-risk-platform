package br.com.fiap.urbanrisk.ia;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Detecção de anomalias em séries de sensores por z-score sobre uma janela móvel
 * (média e desvio-padrão das últimas leituras normais de cada sensor),
 * combinada com limiares absolutos de segurança (ex.: chuva &gt; 30 mm/h).
 * <p>
 * Leituras anômalas NÃO entram na linha de base, para que um evento longo
 * não "normalize" a si próprio.
 */
public class DetectorAnomalias {

    /** Sentido em que o valor representa risco. */
    public enum Sentido { ALTA, BAIXA }

    public record Resultado(double valor, double media, double desvio, double zScore,
                            boolean anomalia, boolean limiarExcedido, double intensidade) {}

    public static final double Z_LIMIAR = 3.5;
    private static final int JANELA = 60;
    private static final int MINIMO_AMOSTRAS = 8;

    private final Map<String, Deque<Double>> janelas = new HashMap<>();

    public synchronized Resultado avaliar(String sensorId, double valor, Sentido sentido,
                                          Double limiarAbsoluto, double desvioMinimo) {
        Deque<Double> janela = janelas.computeIfAbsent(sensorId, k -> new ArrayDeque<>());
        double media = valor;
        double desvio = desvioMinimo;
        double z = 0;

        if (janela.size() >= MINIMO_AMOSTRAS) {
            media = janela.stream().mapToDouble(Double::doubleValue).average().orElse(valor);
            final double m = media;
            double variancia = janela.stream().mapToDouble(x -> (x - m) * (x - m)).sum() / (janela.size() - 1);
            desvio = Math.max(Math.sqrt(variancia), desvioMinimo);
            z = (valor - media) / desvio;
        }

        int sinal = sentido == Sentido.ALTA ? 1 : -1;
        double zNoSentido = sinal * z;
        boolean limiarExcedido = limiarAbsoluto != null && sinal * (valor - limiarAbsoluto) >= 0;
        boolean anomalia = zNoSentido >= Z_LIMIAR || limiarExcedido;

        double intensidadeZ = limitar((zNoSentido - 2.0) / 4.0);
        double intensidadeLimiar = 0;
        if (limiarExcedido) {
            double escala = Math.max(Math.abs(limiarAbsoluto) * 0.5, 1e-6);
            intensidadeLimiar = limitar(0.6 + 0.4 * sinal * (valor - limiarAbsoluto) / escala);
        }
        double intensidade = anomalia ? Math.max(intensidadeZ, intensidadeLimiar) : 0;

        if (!anomalia) {
            janela.addLast(valor);
            if (janela.size() > JANELA) janela.removeFirst();
        }
        return new Resultado(valor, media, desvio, z, anomalia, limiarExcedido, intensidade);
    }

    /** Pré-carrega a linha de base de um sensor (usado pelo simulador e pelos testes). */
    public synchronized void aquecer(String sensorId, Iterable<Double> valoresNormais) {
        Deque<Double> janela = janelas.computeIfAbsent(sensorId, k -> new ArrayDeque<>());
        for (Double v : valoresNormais) {
            janela.addLast(v);
            if (janela.size() > JANELA) janela.removeFirst();
        }
    }

    private static double limitar(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
