package com.atech.curso.m4.api;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Datos para crear o modificar una reserva")
public record ReservaRequest(
        @Schema(example = "1") @NotNull Long salaId,
        @Schema(example = "ana@atech.es") @NotBlank String usuario,
        @Schema(example = "2030-01-10T09:00:00") @NotNull @Future LocalDateTime inicio,
        @Schema(example = "2030-01-10T11:00:00") @NotNull @Future LocalDateTime fin) {
}
