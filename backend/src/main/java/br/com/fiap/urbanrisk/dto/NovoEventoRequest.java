package br.com.fiap.urbanrisk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada para registro de evento por morador/agente (REGISTRO_COMUNITARIO) ou integração com
 * base pública (BASE_PUBLICA). Campos calculados (score, confiança, status...) NÃO são aceitos
 * do cliente: são sempre definidos pelo motor de IA no servidor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NovoEventoRequest(
        @NotBlank(message = "Título é obrigatório") @Size(max = 120) String titulo,
        @NotBlank(message = "Descrição é obrigatória") @Size(max = 1000) String descricao,
        @NotBlank(message = "Categoria é obrigatória")
        @Pattern(regexp = "ALAGAMENTO|ILHA_DE_CALOR|FALTA_ENERGIA|FALTA_AGUA|TRANSPORTE",
                message = "Categoria deve ser ALAGAMENTO, ILHA_DE_CALOR, FALTA_ENERGIA, FALTA_AGUA ou TRANSPORTE")
        String categoria,
        @NotBlank(message = "Bairro é obrigatório") @Size(max = 80) String bairro,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Pattern(regexp = "REGISTRO_COMUNITARIO|BASE_PUBLICA",
                message = "origemDado deve ser REGISTRO_COMUNITARIO ou BASE_PUBLICA (sensores usam /api/sensores/leituras)")
        String origemDado,
        /** Identificador aleatório da instalação do app. É pseudonimizado (hash) e nunca persistido. */
        @Size(max = 100) String dispositivoId
) {}
