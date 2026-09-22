package com.atech.curso.m1.precios;

import java.math.BigDecimal;

/** EJ 1.2 (ampliación) - Varias implementaciones: @Primary y @Qualifier. */
public interface CalculadoraDescuento {

    BigDecimal aplicar(BigDecimal importe);
}
