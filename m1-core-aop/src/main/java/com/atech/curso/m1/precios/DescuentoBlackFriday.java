package com.atech.curso.m1.precios;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Se obtiene con @Qualifier("blackFriday"). */
@Component
@Qualifier("blackFriday")
public class DescuentoBlackFriday implements CalculadoraDescuento {

    private static final BigDecimal FACTOR = new BigDecimal("0.80");

    @Override
    public BigDecimal aplicar(BigDecimal importe) {
        return importe.multiply(FACTOR).setScale(2, RoundingMode.HALF_UP);
    }
}
