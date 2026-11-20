package br.com.fiap.urbanrisk.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados do ciclo de vida de um evento de risco (ver Diagrama de Estados na documentação).
 *
 * <pre>
 * REPORTADO ──► EM_VALIDACAO ──► CONFIRMADO ──► ALERTA_EMITIDO ──► EM_MONITORAMENTO ──► ENCERRADO
 *     │              │               │               ▲   │               │
 *     └──► CONFIRMADO └──► DESCARTADO └──► ENCERRADO │   └──► ENCERRADO   └──► ALERTA_EMITIDO (reincidência)
 *     └──► DESCARTADO     (DESCARTADO)               └── (reemissão)
 * </pre>
 *
 * Qualquer transição fora desta tabela é rejeitada (HTTP 409 na API).
 */
public final class CicloVidaEvento {

    public static final String REPORTADO = "REPORTADO";
    public static final String EM_VALIDACAO = "EM_VALIDACAO";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String ALERTA_EMITIDO = "ALERTA_EMITIDO";
    public static final String EM_MONITORAMENTO = "EM_MONITORAMENTO";
    public static final String ENCERRADO = "ENCERRADO";
    public static final String DESCARTADO = "DESCARTADO";

    /** Estados finais: o evento sai da lista de alertas ativos. */
    public static final List<String> INATIVOS = List.of(ENCERRADO, DESCARTADO);

    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            REPORTADO, Set.of(EM_VALIDACAO, CONFIRMADO, DESCARTADO),
            EM_VALIDACAO, Set.of(CONFIRMADO, DESCARTADO),
            CONFIRMADO, Set.of(ALERTA_EMITIDO, ENCERRADO, DESCARTADO),
            ALERTA_EMITIDO, Set.of(EM_MONITORAMENTO, ENCERRADO),
            EM_MONITORAMENTO, Set.of(ALERTA_EMITIDO, ENCERRADO),
            ENCERRADO, Set.of(),
            DESCARTADO, Set.of()
    );

    /** Ações manuais que um agente da Defesa Civil ou operador de serviço pode executar. */
    public enum Acao {
        CONFIRMAR(CONFIRMADO, "Validado em campo"),
        DESCARTAR(DESCARTADO, "Descartado após verificação"),
        EMITIR_ALERTA(ALERTA_EMITIDO, "Alerta emitido manualmente"),
        MONITORAR(EM_MONITORAMENTO, "Situação sob controle, em monitoramento"),
        ENCERRAR(ENCERRADO, "Ocorrência encerrada");

        public final String destino;
        public final String motivoPadrao;

        Acao(String destino, String motivoPadrao) {
            this.destino = destino;
            this.motivoPadrao = motivoPadrao;
        }
    }

    private CicloVidaEvento() {}

    public static boolean podeTransitar(String de, String para) {
        if (de == null) return REPORTADO.equals(para);
        return TRANSICOES.getOrDefault(de, Set.of()).contains(para);
    }

    public static boolean ativo(String status) {
        return status != null && !INATIVOS.contains(status);
    }

    public static List<String> acoesDisponiveis(String status) {
        List<String> acoes = new ArrayList<>();
        for (Acao a : Acao.values()) {
            if (podeTransitar(status, a.destino)) acoes.add(a.name());
        }
        return acoes;
    }
}
