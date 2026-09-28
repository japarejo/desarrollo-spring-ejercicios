package com.atech.curso.m3.gobierno;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** EJ 3.7 - Test unitario puro: el generador no necesita Spring. */
class GeneradorNombresHeroicosTest {

    @Test
    void componeUnSustantivoYUnAdjetivo() {
        var generador = new GeneradorNombresHeroicos(new Random());

        String[] partes = generador.generar().split(" ");

        assertThat(partes).hasSize(2);
        assertThat(GeneradorNombresHeroicos.SUSTANTIVOS).contains(partes[0]);
        assertThat(GeneradorNombresHeroicos.ADJETIVOS).contains(partes[1]);
    }

    @Test
    void conLaMismaSemillaSaleLaMismaSecuencia() {
        var uno = new GeneradorNombresHeroicos(new Random(42));
        var otro = new GeneradorNombresHeroicos(new Random(42));

        for (int i = 0; i < 20; i++) {
            assertThat(uno.generar()).isEqualTo(otro.generar());
        }
    }

    @Test
    void enUnMismoGobiernoNoSeRepitenNombres() {
        var generador = new GeneradorNombresHeroicos(new Random(7));
        Set<String> usados = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            assertThat(usados.add(generador.generarDistinto(usados))).isTrue();
        }
    }
}
