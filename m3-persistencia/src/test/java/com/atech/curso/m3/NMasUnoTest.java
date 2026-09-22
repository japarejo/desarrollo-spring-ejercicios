package com.atech.curso.m3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.repositorio.ReservaRepository;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** EJ 3.4 - Medir el problema N+1 con las estadísticas de Hibernate y resolverlo con @EntityGraph. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class NMasUnoTest {

    @Autowired
    ReservaRepository reservas;

    @Autowired
    TestEntityManager tem;

    @Autowired
    EntityManagerFactory emf;

    Statistics estadisticas;

    @BeforeEach
    void preparar() {
        estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        tem.clear();
        estadisticas.clear();
    }

    @Test
    void consultaDerivadaProvocaNMasUno() {
        List<Reserva> confirmadas = reservas.findByEstado(EstadoReserva.CONFIRMADA);
        confirmadas.forEach(r -> r.getSala().getNombre());

        // 1 consulta de reservas + 1 por cada sala distinta (Turing, Lovelace, Hopper)
        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    void entityGraphLoResuelveConUnaSolaConsulta() {
        List<Reserva> confirmadas = reservas.buscarConSalaYUsuario(EstadoReserva.CONFIRMADA);
        confirmadas.forEach(r -> {
            r.getSala().getNombre();
            r.getUsuario().getEmail();
        });

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }
}
