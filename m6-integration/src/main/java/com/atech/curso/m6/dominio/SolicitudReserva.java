package com.atech.curso.m6.dominio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mensaje de entrada: una solicitud con varias líneas (se divide con un splitter). */
public record SolicitudReserva(String id, List<LineaReserva> lineas) {

    /** Cada línea queda marcada con el id de su solicitud, venga de la entrada que venga. */
    public SolicitudReserva {
        lineas = lineas == null ? List.of() : lineas.stream().map(l -> l.conSolicitud(id)).toList();
    }

    /** Agrupa líneas sueltas (filas de la tabla o del CSV) en solicitudes, respetando el orden de llegada. */
    public static List<SolicitudReserva> agrupar(List<LineaReserva> lineas) {
        Map<String, List<LineaReserva>> porSolicitud = new LinkedHashMap<>();
        for (LineaReserva linea : lineas) {
            porSolicitud.computeIfAbsent(linea.solicitudId(), k -> new ArrayList<>()).add(linea);
        }
        return porSolicitud.entrySet().stream()
                .map(e -> new SolicitudReserva(e.getKey(), e.getValue()))
                .toList();
    }
}
