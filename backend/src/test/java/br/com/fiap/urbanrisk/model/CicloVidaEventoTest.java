package br.com.fiap.urbanrisk.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.fiap.urbanrisk.model.CicloVidaEvento.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CicloVidaEventoTest {

    @Test
    void fluxoPrincipalEhValido() {
        assertTrue(podeTransitar(null, REPORTADO));
        assertTrue(podeTransitar(REPORTADO, EM_VALIDACAO));
        assertTrue(podeTransitar(EM_VALIDACAO, CONFIRMADO));
        assertTrue(podeTransitar(CONFIRMADO, ALERTA_EMITIDO));
        assertTrue(podeTransitar(ALERTA_EMITIDO, EM_MONITORAMENTO));
        assertTrue(podeTransitar(EM_MONITORAMENTO, ENCERRADO));
    }

    @Test
    void reincidenciaEDescarteSaoValidos() {
        assertTrue(podeTransitar(EM_MONITORAMENTO, ALERTA_EMITIDO));
        assertTrue(podeTransitar(EM_VALIDACAO, DESCARTADO));
        assertTrue(podeTransitar(REPORTADO, DESCARTADO));
    }

    @Test
    void transicoesInvalidasSaoRejeitadas() {
        assertFalse(podeTransitar(null, CONFIRMADO));
        assertFalse(podeTransitar(EM_VALIDACAO, ALERTA_EMITIDO), "não pode emitir alerta sem confirmar");
        assertFalse(podeTransitar(ALERTA_EMITIDO, DESCARTADO), "alerta emitido não é descartado, é encerrado");
        assertFalse(podeTransitar(ENCERRADO, ALERTA_EMITIDO), "estado final");
        assertFalse(podeTransitar(DESCARTADO, CONFIRMADO), "estado final");
    }

    @Test
    void acoesDisponiveisDependemDoEstado() {
        assertEquals(List.of("CONFIRMAR", "DESCARTAR"), acoesDisponiveis(EM_VALIDACAO));
        assertEquals(List.of("EMITIR_ALERTA", "ENCERRAR"), acoesDisponiveis(EM_MONITORAMENTO));
        assertTrue(acoesDisponiveis(ENCERRADO).isEmpty());
        assertFalse(ativo(DESCARTADO));
        assertTrue(ativo(EM_MONITORAMENTO));
    }
}
