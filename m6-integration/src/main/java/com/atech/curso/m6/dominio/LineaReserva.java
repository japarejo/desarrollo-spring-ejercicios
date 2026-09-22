package com.atech.curso.m6.dominio;

import java.time.LocalDate;
import java.util.Map;

/** Una línea de una solicitud: una sala durante unas horas de un día. */
public record LineaReserva(String solicitudId, String sala, LocalDate fecha, int horas) {

    /** Convierte una fila de la tabla linea_pendiente (ColumnMapRowMapper, claves sin distinguir mayúsculas). */
    public static LineaReserva desdeFila(Map<String, Object> fila) {
        Object fecha = fila.get("fecha");
        LocalDate dia = fecha instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) fecha;
        return new LineaReserva(
                (String) fila.get("solicitud_id"),
                (String) fila.get("sala"),
                dia,
                ((Number) fila.get("horas")).intValue());
    }
}
