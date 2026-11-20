package br.com.fiap.urbanrisk.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ResumoPainelResponse(
        long alertasAtivos,
        Map<String, Long> porNivel,
        Map<String, Long> porCategoria,
        Map<String, Long> porOrigem,
        long leiturasUltimos5Min,
        LocalDateTime ultimaLeituraSensor,
        LocalDateTime geradoEm
) {}
