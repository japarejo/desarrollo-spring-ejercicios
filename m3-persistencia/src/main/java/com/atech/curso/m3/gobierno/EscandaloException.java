package com.atech.curso.m3.gobierno;

/**
 * EJ 3.7 - Un escándalo que tumba un nombramiento.
 * <p>
 * Es una excepción <b>comprobada</b> (extiende {@link Exception}) a propósito: Spring sólo hace rollback
 * automático con las {@link RuntimeException} y los {@link Error}. Para que una comprobada deshaga la
 * transacción hay que pedirlo con {@code @Transactional(rollbackFor = EscandaloException.class)}.
 */
public abstract class EscandaloException extends Exception {

    private final String titular;
    private final String organo;

    protected EscandaloException(String titular, String organo, String mensaje) {
        super(mensaje);
        this.titular = titular;
        this.organo = organo;
    }

    public String getTitular() {
        return titular;
    }

    public String getOrgano() {
        return organo;
    }
}
