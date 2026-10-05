package com.atech.curso.m6.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Resultado del agregador: todas las líneas de una solicitud con su importe total. El origen (web,
 * base-de-datos o fichero) no viaja en la carga útil: se lee de la cabecera que pone cada entrada.
 */
public record ResumenSolicitud(String solicitudId, String origen, int lineas, BigDecimal total,
        List<LineaTarificada> detalle) {

    private static final Locale ES = Locale.forLanguageTag("es-ES");

    public static ResumenSolicitud de(List<LineaTarificada> detalle, String origen) {
        BigDecimal total = detalle.stream().map(LineaTarificada::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenSolicitud(detalle.get(0).linea().solicitudId(), origen, detalle.size(), total,
                List.copyOf(detalle));
    }

    /** Texto del justificante que se deja en el buzón de salida para el departamento de administración. */
    public String justificante() {
        StringBuilder texto = new StringBuilder()
                .append("JUSTIFICANTE DE RESERVA · solicitud ").append(solicitudId)
                .append(" · origen: ").append(origen).append("\n\n")
                .append(String.format(ES, "%-10s %-10s %5s %9s  %s%n", "Sala", "Fecha", "Horas", "Importe", "Tarifa"));
        for (LineaTarificada l : detalle) {
            texto.append(String.format(ES, "%-10s %-10s %5d %9.2f  %s%n", l.linea().sala(), l.linea().fecha(),
                    l.linea().horas(), l.precio(), l.origenTarifa()));
        }
        return texto.append(String.format(ES, "%nTOTAL %31.2f EUR%n", total)).toString();
    }
}
