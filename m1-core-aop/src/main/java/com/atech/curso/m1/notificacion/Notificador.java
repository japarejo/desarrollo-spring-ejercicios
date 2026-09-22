package com.atech.curso.m1.notificacion;

import java.util.List;

/**
 * EJ 1.1 - Abstracción de un canal de notificación. El resto de la aplicación depende
 * de esta interfaz, nunca de las implementaciones concretas (principio de inversión de dependencias).
 */
public interface Notificador {

    String canal();

    void notificar(String destinatario, String mensaje);

    /** Mensajes enviados (sólo con fines didácticos / de prueba). */
    List<String> enviados();

    void limpiar();
}
