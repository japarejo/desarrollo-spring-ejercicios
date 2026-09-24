package com.atech.curso.xml.reservas;

import java.math.BigDecimal;
import java.time.Instant;

public record Presupuesto(String idSala, String nombreSala, int horas, BigDecimal importe,
        String tarifa, Instant momento) {
}
