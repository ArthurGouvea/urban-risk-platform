package br.com.fiap.urbanrisk.ia;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o nível de confiança de cada dado a partir da origem e de evidências cruzadas.
 * <ul>
 *   <li>SENSOR_IOT: telemetria automática (alta), reforçada quando um limiar físico é excedido;</li>
 *   <li>BASE_PUBLICA: boletins oficiais (Defesa Civil, concessionárias) – a mais alta;</li>
 *   <li>REGISTRO_COMUNITARIO: começa em 50% e sobe com relatos parecidos, coerência do texto
 *       (NLP) e corroboração por sensor no mesmo bairro/categoria.</li>
 *   <li>Validação em campo por agente eleva qualquer origem a, no mínimo, 90%.</li>
 * </ul>
 */
public class AvaliadorConfianca {

    public record Evidencias(String origem, double coerenciaTexto, boolean textoTemEvidencia,
                             long relatosSimilares, long sensoresCorroborando, boolean limiarFisicoExcedido,
                             boolean validadoPorAgente) {}

    public record Confianca(int percentual, String nivel, List<String> justificativas) {}

    public Confianca avaliar(Evidencias ev) {
        List<String> just = new ArrayList<>();
        double c;
        switch (ev.origem()) {
            case "SENSOR_IOT" -> {
                c = 0.90;
                just.add("Origem: sensor IoT com telemetria automática (base 90%)");
                if (ev.limiarFisicoExcedido()) {
                    c += 0.05;
                    just.add("Limiar físico de segurança excedido (+5%)");
                }
            }
            case "BASE_PUBLICA" -> {
                c = 0.95;
                just.add("Origem: base pública oficial (base 95%)");
            }
            default -> {
                c = 0.50;
                just.add("Origem: registro comunitário (base 50%)");
                long similares = Math.min(3, ev.relatosSimilares());
                if (similares > 0) {
                    c += 0.10 * similares;
                    just.add(String.format("%d relato(s) semelhante(s) nas últimas 2h (+%d%%)", similares, similares * 10));
                }
                if (ev.sensoresCorroborando() > 0) {
                    c += 0.20;
                    just.add("Corroborado por anomalia de sensor no mesmo bairro (+20%)");
                }
                if (ev.textoTemEvidencia()) {
                    if (ev.coerenciaTexto() >= 0.6) {
                        c += 0.10;
                        just.add("Texto coerente com a categoria segundo a IA (+10%)");
                    } else if (ev.coerenciaTexto() < 0.25) {
                        c -= 0.15;
                        just.add("Texto pouco coerente com a categoria escolhida (-15%)");
                    }
                }
            }
        }
        if (ev.validadoPorAgente() && c < 0.90) {
            c = 0.90;
            just.add("Validado em campo por agente da Defesa Civil/operador (mínimo 90%)");
        }
        c = Math.max(0.30, Math.min(0.98, c));
        int pct = (int) Math.round(c * 100);
        String nivel = pct >= 80 ? "ALTA" : pct >= 60 ? "MEDIA" : "BAIXA";
        return new Confianca(pct, nivel, just);
    }
}
