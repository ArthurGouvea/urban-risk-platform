package br.com.fiap.urbanrisk.ia;

import br.com.fiap.urbanrisk.ia.DetectorAnomalias.Sentido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorAnomaliasTest {

    private DetectorAnomalias detector;

    @BeforeEach
    void setUp() {
        detector = new DetectorAnomalias();
        Random r = new Random(42);
        List<Double> chuva = new ArrayList<>();
        List<Double> temp = new ArrayList<>();
        List<Double> tensao = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            chuva.add(2 + r.nextGaussian());
            temp.add(28.5 + r.nextGaussian() * 0.7);
            tensao.add(127 + r.nextGaussian() * 1.5);
        }
        detector.aquecer("PLUV", chuva);
        detector.aquecer("TERM", temp);
        detector.aquecer("TENS", tensao);
    }

    @Test
    void leituraNormalNaoEhAnomalia() {
        assertFalse(detector.avaliar("PLUV", 2.5, Sentido.ALTA, 30.0, 1.0).anomalia());
    }

    @Test
    void limiarFisicoExcedidoGeraAnomaliaComIntensidadeMaxima() {
        var r = detector.avaliar("PLUV", 60, Sentido.ALTA, 30.0, 1.0);
        assertTrue(r.anomalia());
        assertTrue(r.limiarExcedido());
        assertEquals(1.0, r.intensidade(), 1e-9);
    }

    @Test
    void zScoreAltoGeraAnomaliaMesmoSemLimiar() {
        var r = detector.avaliar("TERM", 33.5, Sentido.ALTA, 36.0, 0.8);
        assertTrue(r.anomalia());
        assertFalse(r.limiarExcedido());
        assertTrue(r.zScore() > DetectorAnomalias.Z_LIMIAR);
    }

    @Test
    void respeitaSentidoDoRisco() {
        assertTrue(detector.avaliar("TENS", 80, Sentido.BAIXA, 100.0, 2.0).anomalia(), "queda de tensão é risco");
        assertFalse(detector.avaliar("TENS", 135, Sentido.BAIXA, 100.0, 2.0).anomalia(), "tensão alta não é o risco monitorado");
    }

    @Test
    void anomaliasNaoContaminamALinhaDeBase() {
        for (int i = 0; i < 20; i++) detector.avaliar("PLUV", 55, Sentido.ALTA, null, 1.0);
        assertTrue(detector.avaliar("PLUV", 55, Sentido.ALTA, null, 1.0).anomalia());
        assertFalse(detector.avaliar("PLUV", 2.2, Sentido.ALTA, null, 1.0).anomalia());
    }
}
