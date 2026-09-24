package com.atech.curso.m1.pista;

import java.time.Instant;
import java.util.Map;

/**
 * EJ 1.7 - Una entrada del libro de auditoría: quién, qué, sobre qué y con qué resultado.
 *
 * @param momento    cuándo ocurrió (viene del {@code Clock} inyectado, así es determinista en tests)
 * @param usuario    quién la ejecutó
 * @param accion     qué hizo, p. ej. {@code REEMBOLSAR}
 * @param entidad    tipo de entidad afectada, p. ej. {@code Pedido}
 * @param idEntidad  identificador de la entidad afectada
 * @param datos      resto de argumentos, con los sensibles ya enmascarados
 * @param resultado  {@code OK} o {@code ERROR}
 * @param detalle    motivo del fallo, o {@code null} si fue bien
 */
public record ApunteAuditoria(Instant momento, String usuario, String accion, String entidad,
        String idEntidad, Map<String, String> datos, String resultado, String detalle) {

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    public boolean correcto() {
        return OK.equals(resultado);
    }
}
