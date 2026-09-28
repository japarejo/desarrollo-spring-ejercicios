package com.atech.curso.m3.gobierno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * EJ 3.7 - La alternancia es todo o nada.
 * <p>
 * Igual que {@code ReservaServiceTest}, el test <b>no</b> es {@code @Transactional}: así se ven los commits y
 * los rollbacks reales. El azar se sustituye por un mock del detector para decidir <b>en qué nombramiento</b>
 * estalla el escándalo; por defecto el mock no hace nada (ningún escándalo).
 * Los tests comparan con el estado anterior, así que no dependen del orden en que se ejecuten.
 */
@SpringBootTest
class GobiernoServiceTest {

    /** El quinto órgano en orden protocolario: Ministerio de Defensa contra Villanos. */
    static final int DEFENSA = 5;

    @Autowired
    GobiernoService servicio;

    @Autowired
    GobiernoRepository gobiernos;

    @Autowired
    NombramientoRepository nombramientos;

    @MockitoBean
    DetectorEscandalos detector;

    static Stream<Arguments> escandalos() {
        String titular = "Croqueta Termonuclear";
        String organo = "Ministerio de Defensa contra Villanos";
        return Stream.of(
                Arguments.of(named("Casoplón", new CasoplonException(titular, organo))),
                Arguments.of(named("CutreMaster", new CutreMasterException(titular, organo))),
                Arguments.of(named("Joyas ocultas", new JoyasOcultasException(titular, organo))));
    }

    private void estallaAlNombrarDefensa(EscandaloException escandalo) throws EscandaloException {
        doThrow(escandalo).when(detector).investigar(anyString(), argThat(o -> o.getOrden() == DEFENSA));
    }

    @Test
    void sinEscandalosSeRenuevaTodoElGobierno() throws EscandaloException {
        Gobierno saliente = servicio.vigente().orElseThrow();

        Gobierno entrante = servicio.alternancia();

        assertThat(entrante.getLegislatura()).isEqualTo(saliente.getLegislatura() + 1);
        assertThat(nombramientos.countByGobiernoLegislatura(entrante.getLegislatura()))
                .isEqualTo(servicio.numeroDeOrganos());
        assertThat(gobiernos.findById(saliente.getId())).get().satisfies(g -> {
            assertThat(g.isVigente()).isFalse();
            assertThat(g.getCese()).isNotNull();
        });
        assertThat(servicio.vigente()).get().extracting(Gobierno::getLegislatura)
                .isEqualTo(entrante.getLegislatura());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("escandalos")
    void unEscandaloAMitadDeshaceTodaLaAlternancia(EscandaloException escandalo) throws EscandaloException {
        estallaAlNombrarDefensa(escandalo);
        Gobierno antes = servicio.vigente().orElseThrow();
        List<String> titularesAntes = nombramientos.titularesDe(antes.getLegislatura());
        long gobiernosAntes = gobiernos.count();
        long nombramientosAntes = nombramientos.count();

        // Presidencia, las dos vicepresidencias y Hacienda llegan a insertarse; al nombrar Defensa, ¡escándalo!
        assertThatThrownBy(servicio::alternancia).isSameAs(escandalo);

        // Rollback de TODO: ni gobierno nuevo, ni nombramientos, ni cese del anterior
        assertThat(gobiernos.count()).as("gobiernos (no debe quedar el entrante)").isEqualTo(gobiernosAntes);
        assertThat(nombramientos.count()).as("nombramientos (no deben quedar los 4 primeros)")
                .isEqualTo(nombramientosAntes);
        Gobierno despues = servicio.vigente().orElseThrow();
        assertThat(despues.getLegislatura()).isEqualTo(antes.getLegislatura());
        assertThat(despues.getCese()).isNull();
        assertThat(nombramientos.titularesDe(despues.getLegislatura())).isEqualTo(titularesAntes);
    }

    @Test
    void sinRollbackForSeGuardaUnGobiernoAMedias() throws EscandaloException {
        estallaAlNombrarDefensa(new CasoplonException("Botijo Premium", "Ministerio de Defensa contra Villanos"));
        Gobierno antes = servicio.vigente().orElseThrow();

        assertThatThrownBy(servicio::alternanciaSinRollbackFor).isInstanceOf(CasoplonException.class);

        // La excepción es comprobada y no hay rollbackFor: Spring hace COMMIT de lo que hubiera
        Gobierno aMedias = servicio.vigente().orElseThrow();
        assertThat(aMedias.getLegislatura()).isEqualTo(antes.getLegislatura() + 1);
        assertThat(aMedias.getNombramientos()).hasSize(DEFENSA - 1);
        assertThat(gobiernos.findById(antes.getId())).get().extracting(Gobierno::isVigente).isEqualTo(false);
    }
}
