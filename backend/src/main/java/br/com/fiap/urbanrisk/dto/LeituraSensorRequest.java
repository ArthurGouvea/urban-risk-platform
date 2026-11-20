package br.com.fiap.urbanrisk.dto;

import br.com.fiap.urbanrisk.model.TipoSensor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Leitura enviada por um sensor IoT real (ou pelo simulador). */
public record LeituraSensorRequest(
        @NotBlank @Size(max = 60) String sensorId,
        @NotNull TipoSensor tipo,
        @NotBlank @Size(max = 80) String bairro,
        Double latitude,
        Double longitude,
        @NotNull Double valor
) {}
