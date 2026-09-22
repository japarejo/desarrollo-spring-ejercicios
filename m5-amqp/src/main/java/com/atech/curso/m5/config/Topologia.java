package com.atech.curso.m5.config;

/** Nombres de la topología AMQP (compartidos por configuración, productor, consumidores y tests). */
public final class Topologia {

    public static final String EXCHANGE = "reservas.exchange";
    public static final String DLX = "reservas.dlx";

    public static final String COLA_FACTURACION = "reservas.facturacion";
    public static final String COLA_NOTIFICACIONES = "reservas.notificaciones";
    public static final String COLA_AUDITORIA = "reservas.auditoria";
    /** Cola "parking lot" para los mensajes que agotan los reintentos (RepublishMessageRecoverer). */
    public static final String COLA_ERRORES = "reservas.errores";

    public static final String RK_CONFIRMADA = "reserva.confirmada";
    public static final String RK_CANCELADA = "reserva.cancelada";
    public static final String RK_ERROR = "error";

    private Topologia() {
    }

    public static String dlq(String cola) {
        return cola + ".dlq";
    }
}
