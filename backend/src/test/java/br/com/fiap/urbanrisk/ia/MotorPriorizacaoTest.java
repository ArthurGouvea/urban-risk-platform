package br.com.fiap.urbanrisk.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorPriorizacaoTest {

    private final MotorPriorizacao motor = new MotorPriorizacao();

    @Test
    void anomaliaForteDeSensorEhCritica() {
        var r = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.95, 0.5, 1.0, 0, 0));
        assertEquals("CRITICO", r.nivelRisco());
        assertTrue(r.score() >= 85);
    }

    @Test
    void relatoIsoladoEVagoTemPrioridadeBaixa() {
        var r = motor.avaliar(new MotorPriorizacao.Entrada("FALTA_ENERGIA", 0.45, 0.2, 0, 0, 0));
        assertTrue(r.score() < 45, "score = " + r.score());
        assertEquals("BAIXO", r.nivelRisco());
    }

    @Test
    void maisConfiancaECorrelacaoAumentamOScore() {
        var base = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.5, 0.5, 0, 0, 0));
        var maisConfianca = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.9, 0.5, 0, 0, 0));
        var correlacionado = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.5, 0.5, 0, 3, 0));
        assertTrue(maisConfianca.score() > base.score());
        assertTrue(correlacionado.score() > base.score());
    }

    @Test
    void eventosAntigosPerdemPrioridade() {
        var recente = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.8, 0.6, 0.5, 1, 5));
        var antigo = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.8, 0.6, 0.5, 1, 360));
        assertTrue(antigo.score() < recente.score());
    }

    @Test
    void retornaFatoresExplicaveis() {
        var r = motor.avaliar(new MotorPriorizacao.Entrada("ALAGAMENTO", 0.9, 0.8, 0.7, 2, 10));
        assertTrue(r.fatores().get(0).startsWith("Probabilidade estimada"));
        assertTrue(r.fatores().size() >= 4);
    }

    @Test
    void faixasDeNivel() {
        assertEquals("CRITICO", MotorPriorizacao.nivel(85));
        assertEquals("ALTO", MotorPriorizacao.nivel(70));
        assertEquals("MODERADO", MotorPriorizacao.nivel(45));
        assertEquals("BAIXO", MotorPriorizacao.nivel(44));
    }
}
