package br.com.fiap.urbanrisk.dto;

import br.com.fiap.urbanrisk.model.CicloVidaEvento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ação manual de agente/operador sobre um evento. "perfil" identifica o papel, nunca a pessoa (LGPD). */
public record AcaoAgenteRequest(
        @NotNull(message = "Ação é obrigatória") CicloVidaEvento.Acao acao,
        @Size(max = 300) String motivo,
        @Size(max = 60) String perfil
) {}
