package br.com.fiap.urbanrisk.privacidade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnonimizadorLgpdTest {

    private final AnonimizadorLgpd anonimizador = new AnonimizadorLgpd("salt-teste");

    @Test
    void pseudonimoEhDeterministicoENaoRevelaOId() {
        String p1 = anonimizador.pseudonimizar("device-123");
        assertEquals(p1, anonimizador.pseudonimizar("device-123"));
        assertNotEquals(p1, anonimizador.pseudonimizar("device-456"));
        assertTrue(p1.startsWith("USR-"));
        assertEquals(12, p1.length());
        assertFalse(p1.contains("123"));
    }

    @Test
    void mascaraDadosPessoaisNoTexto() {
        String texto = "Me liga (11) 98765-4321 ou joao@email.com, CPF 123.456.789-09, carro ABC1D23";
        String limpo = anonimizador.mascararDadosPessoais(texto);
        assertFalse(limpo.contains("98765"));
        assertFalse(limpo.contains("joao@email.com"));
        assertFalse(limpo.contains("123.456.789-09"));
        assertFalse(limpo.contains("ABC1D23"));
        assertTrue(limpo.startsWith("Me liga"));
    }

    @Test
    void naoAlteraTextoSemDadosPessoais() {
        String texto = "Rua alagada com 40 cm de água desde as 14h";
        assertEquals(texto, anonimizador.mascararDadosPessoais(texto));
    }

    @Test
    void generalizaCoordenadas() {
        assertEquals(-23.561, anonimizador.generalizarCoordenada(-23.561437).doubleValue(), 1e-9);
    }
}
