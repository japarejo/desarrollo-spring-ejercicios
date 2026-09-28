package com.atech.curso.m3.gobierno;

import java.time.LocalDateTime;

/** EJ 3.7 - Proyección DTO para el histórico de gobiernos. */
public record ResumenGobierno(int legislatura, LocalDateTime tomaPosesion, LocalDateTime cese, boolean vigente,
        long cargos) {
}
