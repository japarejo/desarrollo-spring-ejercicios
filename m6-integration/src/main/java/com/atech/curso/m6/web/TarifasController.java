package com.atech.curso.m6.web;

import java.math.BigDecimal;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;

import io.swagger.v3.oas.annotations.Hidden;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula el servicio externo de tarifas (en el mismo proceso para simplificar la práctica): las reservas de
 * jornada se cobran a 9 EUR/hora en lugar de 10. Oculto en Swagger UI: lo llama el flujo, no las personas.
 */
@Hidden
@RestController
public class TarifasController {

    private static final BigDecimal PRECIO_HORA_LARGA = new BigDecimal("9");

    @PostMapping("/tarifas")
    public LineaTarificada tarificar(@RequestBody LineaReserva linea) {
        return new LineaTarificada(linea, PRECIO_HORA_LARGA.multiply(BigDecimal.valueOf(linea.horas())),
                "servicio-tarifas");
    }
}
