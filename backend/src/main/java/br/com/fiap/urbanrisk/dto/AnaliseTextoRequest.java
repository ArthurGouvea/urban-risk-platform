package br.com.fiap.urbanrisk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnaliseTextoRequest(@NotBlank @Size(max = 1100) String texto, String categoria) {}
