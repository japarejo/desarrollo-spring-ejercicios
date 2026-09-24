package com.atech.curso.xml.tarifas;

import java.math.BigDecimal;

import com.atech.curso.xml.dominio.Sala;

/**
 * Se configura por <b>setter</b>: en XML con {@code <property name="precioHora" .../>} y en Java
 * llamando al setter dentro del método {@code @Bean}.
 */
public class TarifaPorHora implements Tarifa {

    private BigDecimal precioHora = BigDecimal.ZERO;

    public void setPrecioHora(BigDecimal precioHora) {
        this.precioHora = precioHora;
    }

    public BigDecimal getPrecioHora() {
        return precioHora;
    }

    @Override
    public String nombre() {
        return "POR_HORA";
    }

    @Override
    public BigDecimal importe(Sala sala, int horas) {
        return precioHora.multiply(BigDecimal.valueOf(horas));
    }
}
