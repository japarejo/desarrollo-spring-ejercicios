package com.atech.curso.xml.tarifas;

import java.math.BigDecimal;

import com.atech.curso.xml.dominio.Sala;

/** Estrategia de precio. Hay dos implementaciones: el contenedor decide cuál se inyecta. */
public interface Tarifa {

    String nombre();

    BigDecimal importe(Sala sala, int horas);
}
