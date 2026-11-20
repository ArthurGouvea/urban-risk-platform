package br.com.fiap.urbanrisk.ia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modelo logístico explicável que estima a probabilidade de um evento ser de risco alto
 * e a converte em score de prioridade (0–100).
 *
 * <pre>
 * logit = b0 + w1·severidade + w2·confiança + w3·urgência(NLP) + w4·anomalia(sensor)
 *            + w5·correlação + w6·recência
 * probabilidade = 1 / (1 + e^-logit)        score = round(100 · probabilidade)
 * </pre>
 *
 * Cada termo w·x é devolvido como "fator" para que o morador/agente veja POR QUE o alerta
 * recebeu aquela prioridade (IA explicável / transparência).
 */
public class MotorPriorizacao {

    public record Entrada(String categoria, double confianca, double urgenciaTexto,
                          double intensidadeAnomalia, int eventosCorrelacionados,
                          double minutosDesdeEvento) {}

    public record Resultado(int score, double probabilidade, String nivelRisco, List<String> fatores) {}

    public static final double INTERCEPTO = -4.2;
    public static final Map<String, Double> PESOS = new LinkedHashMap<>();
    static {
        PESOS.put("severidade", 2.2);
        PESOS.put("confianca", 2.0);
        PESOS.put("urgencia", 1.6);
        PESOS.put("anomalia", 1.8);
        PESOS.put("correlacao", 1.2);
        PESOS.put("recencia", 1.0);
    }

    /** Meia-vida (em minutos) do fator de recência. */
    public static final double MEIA_VIDA_MIN = 120;

    public static double severidade(String categoria) {
        if (categoria == null) return 0.4;
        return switch (categoria.toUpperCase(Locale.ROOT)) {
            case "ALAGAMENTO" -> 0.9;
            case "ILHA_DE_CALOR" -> 0.75;
            case "FALTA_ENERGIA" -> 0.7;
            case "FALTA_AGUA" -> 0.6;
            case "TRANSPORTE" -> 0.55;
            default -> 0.4;
        };
    }

    public Resultado avaliar(Entrada e) {
        double sev = severidade(e.categoria());
        double conf = limitar(e.confianca());
        double urg = limitar(e.urgenciaTexto());
        double anom = limitar(e.intensidadeAnomalia());
        double corr = limitar(e.eventosCorrelacionados() / 3.0);
        double rec = Math.pow(0.5, Math.max(0, e.minutosDesdeEvento()) / MEIA_VIDA_MIN);

        Map<String, Double> contrib = new LinkedHashMap<>();
        contrib.put("Gravidade da categoria", PESOS.get("severidade") * sev);
        contrib.put(String.format(Locale.ROOT, "Confiança da fonte (%.0f%%)", conf * 100), PESOS.get("confianca") * conf);
        contrib.put(String.format(Locale.ROOT, "Urgência detectada no texto (%.0f%%)", urg * 100), PESOS.get("urgencia") * urg);
        contrib.put(String.format(Locale.ROOT, "Anomalia de sensor (intensidade %.0f%%)", anom * 100), PESOS.get("anomalia") * anom);
        contrib.put(e.eventosCorrelacionados() + " evento(s) correlacionado(s) no bairro", PESOS.get("correlacao") * corr);
        contrib.put(String.format(Locale.ROOT, "Recência (%.0f min atrás)", Math.max(0, e.minutosDesdeEvento())), PESOS.get("recencia") * rec);

        double logit = INTERCEPTO + contrib.values().stream().mapToDouble(Double::doubleValue).sum();
        double p = 1.0 / (1.0 + Math.exp(-logit));
        int score = (int) Math.round(p * 100);

        List<String> fatores = new ArrayList<>();
        fatores.add(String.format(Locale.ROOT, "Probabilidade estimada de risco alto: %.0f%%", p * 100));
        contrib.entrySet().stream()
                .filter(en -> en.getValue() > 0.05)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(en -> fatores.add(String.format(Locale.ROOT, "%s: +%.2f", en.getKey(), en.getValue())));

        return new Resultado(score, p, nivel(score), fatores);
    }

    public static String nivel(int score) {
        if (score >= 85) return "CRITICO";
        if (score >= 70) return "ALTO";
        if (score >= 45) return "MODERADO";
        return "BAIXO";
    }

    private static double limitar(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
