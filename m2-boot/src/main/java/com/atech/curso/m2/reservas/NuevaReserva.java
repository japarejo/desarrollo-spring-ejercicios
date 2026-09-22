package com.atech.curso.m2.reservas;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NuevaReserva(
        @NotBlank String sala,
        @NotBlank String usuario,
        @NotNull LocalDate fecha,
        @NotNull LocalTime inicio,
        @NotNull LocalTime fin) {
}
