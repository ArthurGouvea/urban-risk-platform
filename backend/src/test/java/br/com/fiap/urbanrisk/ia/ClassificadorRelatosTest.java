package br.com.fiap.urbanrisk.ia;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificadorRelatosTest {

    private static ClassificadorRelatos classificador;

    @BeforeAll
    static void treinar() throws Exception {
        try (InputStream csv = ClassificadorRelatosTest.class.getResourceAsStream("/ia/corpus-relatos.csv")) {
            assertNotNull(csv, "corpus deve existir no classpath");
            classificador = ClassificadorRelatos.deCsv(csv);
        }
    }

    @Test
    void deveClassificarCategoriaDeTextosNovos() {
        assertEquals("ALAGAMENTO", classificador.analisar("Minha rua está alagada e a água entrou na garagem", null).categoriaSugerida());
        assertEquals("FALTA_ENERGIA", classificador.analisar("Fio solto no poste soltando faíscas e sem luz", null).categoriaSugerida());
        assertEquals("ILHA_DE_CALOR", classificador.analisar("Calor muito forte na praça sem sombra", null).categoriaSugerida());
        assertEquals("FALTA_AGUA", classificador.analisar("Torneira seca, sem abastecimento de água desde cedo", null).categoriaSugerida());
        assertEquals("TRANSPORTE", classificador.analisar("Metrô parado na estação e ônibus lotados", null).categoriaSugerida());
    }

    @Test
    void deveDetectarUrgencia() {
        double alta = classificador.analisar("Idoso passou mal com o calor, precisa de socorro", null).probabilidadeUrgencia();
        double baixa = classificador.analisar("Pequena poça na calçada", null).probabilidadeUrgencia();
        assertTrue(alta > 0.5, "urgência alta esperada, obtido " + alta);
        assertTrue(baixa < 0.5, "urgência baixa esperada, obtido " + baixa);
    }

    @Test
    void deveMedirCoerenciaComCategoriaInformada() {
        var coerente = classificador.analisar("Rua alagada com enchente", "ALAGAMENTO");
        var incoerente = classificador.analisar("Rua alagada com enchente", "FALTA_ENERGIA");
        assertTrue(coerente.coerenciaCategoriaInformada() > 0.6);
        assertTrue(incoerente.coerenciaCategoriaInformada() < 0.25);
    }

    @Test
    void textoSemPalavrasConhecidasNaoTemEvidencia() {
        var analise = classificador.analisar("xyzw qwerty", "ALAGAMENTO");
        assertFalse(analise.temEvidencia());
        assertEquals(0.5, analise.coerenciaCategoriaInformada(), 1e-9);
    }

    @Test
    void acuraciaDaValidacaoCruzadaDeveSerAceitavel() {
        assertTrue(classificador.getAcuraciaCategoria() >= 0.75, "acurácia categoria = " + classificador.getAcuraciaCategoria());
        assertTrue(classificador.getAcuraciaUrgencia() >= 0.65, "acurácia urgência = " + classificador.getAcuraciaUrgencia());
    }
}
