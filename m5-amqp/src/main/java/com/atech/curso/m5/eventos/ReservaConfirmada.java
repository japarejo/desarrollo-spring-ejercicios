package com.atech.curso.m5.eventos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** EJ 5.2 - Evento de integración: inmutable, autocontenido y serializado como JSON. */
public record ReservaConfirmada(
        @NotBlank String reservaId,
        @NotBlank String sala,
        @NotBlank String usuario,
        @NotNull LocalDateTime inicio,
        @NotNull LocalDateTime fin,
        @NotNull BigDecimal importe) {
}
