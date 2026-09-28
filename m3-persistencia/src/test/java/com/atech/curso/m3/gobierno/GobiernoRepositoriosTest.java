package com.atech.curso.m3.gobierno;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** EJ 3.7 - Los repositorios del gobierno sobre los datos de la migración V3. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class GobiernoRepositoriosTest {

    @Autowired
    GobiernoRepository gobiernos;

    @Autowired
    OrganoRepository organos;

    @Autowired
    NombramientoRepository nombramientos;

    @Autowired
    TestEntityManager tem;

    @Autowired
    EntityManagerFactory emf;

    @Test
    void laMigracionCreaLosOrganosEnOrdenProtocolario() {
        assertThat(organos.findAllByOrderByOrdenAsc())
                .hasSize(12)
                .first().extracting(Organo::getTipo).isEqualTo(TipoOrgano.PRESIDENCIA);
        assertThat(organos.countByTipo(TipoOrgano.MINISTERIO)).isEqualTo(7);
    }

    @Test
    void elGobiernoVigenteSeCargaConSusNombramientosEnUnaSolaConsulta() {
        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        tem.clear();
        estadisticas.clear();

        Gobierno vigente = gobiernos.buscarVigenteConNombramientos().orElseThrow();
        vigente.getNombramientos().forEach(n -> n.getOrgano().getNombre());

        assertThat(vigente.getLegislatura()).isEqualTo(1);
        assertThat(vigente.getNombramientos()).extracting(Nombramiento::getTitular)
                .startsWith("Vengador Holístico", "Aguja Dinámico");
        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void historicoConProyeccionDto() {
        assertThat(gobiernos.historico()).singleElement().satisfies(r -> {
            assertThat(r.legislatura()).isEqualTo(1);
            assertThat(r.vigente()).isTrue();
            assertThat(r.cargos()).isEqualTo(12);
        });
        assertThat(gobiernos.ultimaLegislatura()).isEqualTo(1);
    }

    @Test
    void trayectoriaConProyeccionPorInterfazYAlias() {
        assertThat(nombramientos.trayectoria("holístico")).singleElement().satisfies(t -> {
            assertThat(t.getTitular()).isEqualTo("Vengador Holístico");
            assertThat(t.getOrgano()).isEqualTo("Presidencia del Gobierno");
            assertThat(t.getLegislatura()).isEqualTo(1);
        });
    }
}
