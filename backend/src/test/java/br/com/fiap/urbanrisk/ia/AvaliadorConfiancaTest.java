package br.com.fiap.urbanrisk.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvaliadorConfiancaTest {

    private final AvaliadorConfianca avaliador = new AvaliadorConfianca();

    private AvaliadorConfianca.Confianca avaliar(String origem, double coerencia, boolean evidencia,
                                                 long similares, long sensores, boolean limiar) {
        return avaliador.avaliar(new AvaliadorConfianca.Evidencias(origem, coerencia, evidencia, similares, sensores, limiar, false));
    }

    @Test
    void fontesAutomaticasEOficiaisTemConfiancaAlta() {
        assertEquals(90, avaliar("SENSOR_IOT", 0.5, false, 0, 0, false).percentual());
        assertEquals(95, avaliar("SENSOR_IOT", 0.5, false, 0, 0, true).percentual());
        assertEquals(95, avaliar("BASE_PUBLICA", 0.5, false, 0, 0, false).percentual());
        assertEquals("ALTA", avaliar("BASE_PUBLICA", 0.5, false, 0, 0, false).nivel());
    }

    @Test
    void relatoComunitarioIsoladoFicaEmValidacao() {
        var c = avaliar("REGISTRO_COMUNITARIO", 0.5, false, 0, 0, false);
        assertEquals(50, c.percentual());
        assertEquals("BAIXA", c.nivel());
    }

    @Test
    void validacaoCruzadaAumentaConfianca() {
        var c = avaliar("REGISTRO_COMUNITARIO", 0.9, true, 2, 1, false);
        assertEquals(98, c.percentual()); // 50 + 20 + 20 + 10 = 100 -> limitado a 98
        assertTrue(c.justificativas().stream().anyMatch(j -> j.contains("sensor")));
    }

    @Test
    void textoIncoerenteReduzConfianca() {
        assertEquals(35, avaliar("REGISTRO_COMUNITARIO", 0.1, true, 0, 0, false).percentual());
    }

    @Test
    void validacaoPorAgenteGaranteNoMinimo90() {
        var c = avaliador.avaliar(new AvaliadorConfianca.Evidencias("REGISTRO_COMUNITARIO", 0.5, false, 0, 0, false, true));
        assertEquals(90, c.percentual());
        assertTrue(c.justificativas().stream().anyMatch(j -> j.contains("agente")));
    }
}
