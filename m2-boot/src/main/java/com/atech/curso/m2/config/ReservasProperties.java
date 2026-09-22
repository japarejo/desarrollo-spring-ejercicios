package com.atech.curso.m2.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * EJ 2.1 - Propiedades tipadas, inmutables (record) y validadas al arrancar.
 * El procesador de configuración genera los metadatos para autocompletar en el IDE.
 *
 * @param salas salas que se pueden reservar
 * @param duracionMaxima duración máxima de una reserva (admite 90m, 4h...)
 * @param horaApertura primera hora reservable (0-23)
 * @param horaCierre hora de cierre (1-24)
 * @param notificaciones configuración anidada
 */
@Validated
@ConfigurationProperties("atech.reservas")
public record ReservasProperties(
        @NotEmpty List<String> salas,
        @DefaultValue("4h") @NotNull Duration duracionMaxima,
        @DefaultValue("8") @Min(0) @Max(23) int horaApertura,
        @DefaultValue("20") @Min(1) @Max(24) int horaCierre,
        @DefaultValue @Valid Notificaciones notificaciones) {

    public record Notificaciones(
            @DefaultValue("false") boolean habilitadas,
            @DefaultValue("reservas@atech.es") @Email String remitente) {
    }

    public boolean salaPermitida(String sala) {
        return salas.stream().anyMatch(s -> s.equalsIgnoreCase(sala));
    }
}
