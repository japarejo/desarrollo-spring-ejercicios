package com.atech.curso.m4.api;

import java.time.LocalDateTime;

import com.atech.curso.m4.dominio.Reserva;

public record ReservaResponse(Long id, Long salaId, String sala, String usuario, LocalDateTime inicio,
        LocalDateTime fin) {

    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getSala().getId(), r.getSala().getNombre(), r.getUsuario(),
                r.getInicio(), r.getFin());
    }
}
