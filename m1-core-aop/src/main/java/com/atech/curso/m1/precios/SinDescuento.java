package com.atech.curso.m1.precios;

import java.math.BigDecimal;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Implementación por defecto cuando se inyecta CalculadoraDescuento sin cualificar. */
@Component
@Primary
public class SinDescuento implements CalculadoraDescuento {

    @Override
    public BigDecimal aplicar(BigDecimal importe) {
        return importe;
    }
}
