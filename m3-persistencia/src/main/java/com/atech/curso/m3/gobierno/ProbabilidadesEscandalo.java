package com.atech.curso.m3.gobierno;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * EJ 3.7 - Probabilidad de que estalle cada escándalo <b>en cada nombramiento</b> (entre 0 y 1).
 * Se configuran en {@code application.yml} con el prefijo {@code gobierno.escandalos}.
 */
@ConfigurationProperties("gobierno.escandalos")
public record ProbabilidadesEscandalo(
        @DefaultValue("0.03") double casoplon,
        @DefaultValue("0.02") double cutreMaster,
        @DefaultValue("0.01") double joyasOcultas) {

    public ProbabilidadesEscandalo {
        comprobar("casoplon", casoplon);
        comprobar("cutre-master", cutreMaster);
        comprobar("joyas-ocultas", joyasOcultas);
        if (casoplon + cutreMaster + joyasOcultas > 1.0) {
            throw new IllegalArgumentException("Las probabilidades de escándalo no pueden sumar más de 1");
        }
    }

    public static final ProbabilidadesEscandalo NINGUNA = new ProbabilidadesEscandalo(0, 0, 0);

    /** Probabilidad de que un nombramiento cualquiera acabe en escándalo. */
    public double total() {
        return casoplon + cutreMaster + joyasOcultas;
    }

    private static void comprobar(String nombre, double valor) {
        if (valor < 0 || valor > 1) {
            throw new IllegalArgumentException("La probabilidad de " + nombre + " debe estar entre 0 y 1: " + valor);
        }
    }
}
