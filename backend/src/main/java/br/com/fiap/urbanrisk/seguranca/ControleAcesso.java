package br.com.fiap.urbanrisk.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Controle de acesso simples para ações privilegiadas (agente da Defesa Civil / operador de serviço):
 * mudar o estado de um evento e publicar boletins de BASE_PUBLICA exigem o cabeçalho
 * {@code X-Token-Agente}. Comparação em tempo constante. Em produção, substituir por OAuth2/JWT.
 */
@Component
public class ControleAcesso {

    public static final String CABECALHO = "X-Token-Agente";

    private final byte[] tokenEsperado;

    public ControleAcesso(@Value("${urbanrisk.seguranca.token-agente:defesa-civil-2026}") String token) {
        this.tokenEsperado = token.getBytes(StandardCharsets.UTF_8);
    }

    public void exigirAgente(String tokenRecebido) {
        if (tokenRecebido == null
                || !MessageDigest.isEqual(tokenEsperado, tokenRecebido.getBytes(StandardCharsets.UTF_8))) {
            throw new AcessoNegadoException("Ação permitida apenas para agentes/operadores autorizados (cabeçalho "
                    + CABECALHO + ")");
        }
    }
}
