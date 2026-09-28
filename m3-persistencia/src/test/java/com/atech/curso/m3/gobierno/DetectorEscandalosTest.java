package com.atech.curso.m3.gobierno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** EJ 3.7 - Cada escándalo aparece con la probabilidad configurada. */
class DetectorEscandalosTest {

    static final Organo HACIENDA = new Organo("Ministerio de Hacienda y Cuadre de Cuentas", TipoOrgano.MINISTERIO, 4);

    @Test
    void conProbabilidadCeroNuncaHayEscandalo() {
        var detector = new DetectorEscandalos(new Random(), ProbabilidadesEscandalo.NINGUNA);

        for (int i = 0; i < 1_000; i++) {
            assertThatCode(() -> detector.investigar("Vengador Holístico", HACIENDA)).doesNotThrowAnyException();
        }
    }

    @Test
    void conProbabilidadUnoSiempreEstallaEseEscandalo() {
        var casoplon = new DetectorEscandalos(new Random(), new ProbabilidadesEscandalo(1, 0, 0));
        var cutreMaster = new DetectorEscandalos(new Random(), new ProbabilidadesEscandalo(0, 1, 0));
        var joyas = new DetectorEscandalos(new Random(), new ProbabilidadesEscandalo(0, 0, 1));

        assertThatThrownBy(() -> casoplon.investigar("Aguja Dinámico", HACIENDA))
                .isInstanceOf(CasoplonException.class)
                .hasMessageContaining("Aguja Dinámico")
                .hasMessageContaining("Hacienda");
        assertThatThrownBy(() -> cutreMaster.investigar("Aguja Dinámico", HACIENDA))
                .isInstanceOf(CutreMasterException.class);
        assertThatThrownBy(() -> joyas.investigar("Aguja Dinámico", HACIENDA))
                .isInstanceOf(JoyasOcultasException.class);
    }

    @Test
    void unChivatazoHaceEstallarUnEscandaloSeguroUnaSolaVez() {
        var detector = new DetectorEscandalos(new Random(), ProbabilidadesEscandalo.NINGUNA);
        var defensa = new Organo("Ministerio de Defensa contra Villanos", TipoOrgano.MINISTERIO, 5);
        detector.filtrarChivatazo(5);

        assertThatCode(() -> detector.investigar("Pulpo Premium", HACIENDA)).doesNotThrowAnyException();
        assertThatThrownBy(() -> detector.investigar("Pulpo Premium", defensa))
                .isInstanceOf(EscandaloException.class);
        // Se consume al usarlo: la siguiente vez vuelve a mandar el azar (aquí, probabilidad 0)
        assertThatCode(() -> detector.investigar("Pulpo Premium", defensa)).doesNotThrowAnyException();
    }

    @Test
    void lasFrecuenciasSeAjustanALasProbabilidades() {
        var detector = new DetectorEscandalos(new Random(2030), new ProbabilidadesEscandalo(0.30, 0.20, 0.10));
        int tiradas = 100_000;
        Map<String, Integer> veces = new HashMap<>();

        for (int i = 0; i < tiradas; i++) {
            try {
                detector.investigar("Titán Presupuestario", HACIENDA);
                veces.merge("limpio", 1, Integer::sum);
            } catch (EscandaloException e) {
                veces.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            }
        }

        assertThat(veces.get("CasoplonException") / (double) tiradas).isCloseTo(0.30, within(0.01));
        assertThat(veces.get("CutreMasterException") / (double) tiradas).isCloseTo(0.20, within(0.01));
        assertThat(veces.get("JoyasOcultasException") / (double) tiradas).isCloseTo(0.10, within(0.01));
        assertThat(veces.get("limpio") / (double) tiradas).isCloseTo(0.40, within(0.01));
    }

    @Test
    void probabilidadDeCompletarUnGobierno() {
        var detector = new DetectorEscandalos(new Random(), new ProbabilidadesEscandalo(0.03, 0.02, 0.01));

        // 12 nombramientos seguidos sin escándalo: 0,94 elevado a 12
        assertThat(detector.probabilidadDeExito(12)).isCloseTo(0.476, within(0.001));
    }

    @Test
    void lasProbabilidadesSeValidan() {
        assertThatThrownBy(() -> new ProbabilidadesEscandalo(1.5, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProbabilidadesEscandalo(0.5, 0.4, 0.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sumar más de 1");
    }
}
