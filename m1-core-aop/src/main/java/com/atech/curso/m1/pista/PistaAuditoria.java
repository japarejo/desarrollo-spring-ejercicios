package com.atech.curso.m1.pista;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EJ 1.7 - Marca una operación de negocio que debe dejar <b>pista de auditoría</b>.
 *
 * <p>No confundir con {@code @Auditado} (EJ 1.4), que <em>mide</em> la ejecución: aquí interesa
 * el <b>rastro funcional</b> (quién hizo qué, sobre qué entidad y con qué resultado), que es lo
 * que piden auditorías y normativas. Es el ejemplo de manual de un <em>interés transversal</em>:
 * aparece en decenas de servicios y no tiene nada que ver con su lógica de negocio.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PistaAuditoria {

    /** Acción registrada en el libro de auditoría, p. ej. {@code REEMBOLSAR}. */
    String accion();

    /** Tipo de entidad afectada, p. ej. {@code Pedido}. */
    String entidad();
}
