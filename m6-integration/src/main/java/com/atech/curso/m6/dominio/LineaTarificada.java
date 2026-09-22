package com.atech.curso.m6.dominio;

import java.math.BigDecimal;

public record LineaTarificada(LineaReserva linea, BigDecimal precio, String origenTarifa) {

    private static final BigDecimal PRECIO_HORA = new BigDecimal("10");

    /** Tarifa local para reservas cortas. */
    public static LineaTarificada estandar(LineaReserva linea) {
        return new LineaTarificada(linea, PRECIO_HORA.multiply(BigDecimal.valueOf(linea.horas())), "estandar");
    }
}
