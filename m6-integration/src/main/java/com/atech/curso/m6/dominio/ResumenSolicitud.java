package com.atech.curso.m6.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Resultado del agregador: todas las líneas de una solicitud con su importe total. */
public record ResumenSolicitud(String solicitudId, int lineas, BigDecimal total, List<LineaTarificada> detalle) {

    public static ResumenSolicitud de(List<LineaTarificada> detalle) {
        BigDecimal total = detalle.stream().map(LineaTarificada::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenSolicitud(detalle.get(0).linea().solicitudId(), detalle.size(), total, List.copyOf(detalle));
    }
}
