package com.atech.curso.m6.dominio;

import java.time.LocalDate;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/** Una línea de una solicitud: una sala durante unas horas de un día. */
public record LineaReserva(
        @Schema(description = "No hace falta enviarlo: se copia del id de la solicitud", accessMode = Schema.AccessMode.READ_ONLY)
        String solicitudId,
        String sala,
        LocalDate fecha,
        int horas) {

    /** La misma línea, marcada con el id de su solicitud. */
    public LineaReserva conSolicitud(String id) {
        return new LineaReserva(id, sala, fecha, horas);
    }

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

    /** Convierte una línea del CSV: {@code solicitud,sala,fecha,horas} (p. ej. {@code S-200,Turing,2030-03-05,3}). */
    public static LineaReserva desdeCsv(String linea) {
        String[] campos = linea.split(",");
        if (campos.length != 4) {
            throw new IllegalArgumentException("Línea de CSV con " + campos.length + " campos (se esperaban 4): " + linea);
        }
        return new LineaReserva(campos[0].trim(), campos[1].trim(), LocalDate.parse(campos[2].trim()),
                Integer.parseInt(campos[3].trim()));
    }
}
