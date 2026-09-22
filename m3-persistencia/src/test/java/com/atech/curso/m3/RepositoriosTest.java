package com.atech.curso.m3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.dominio.Sala;
import com.atech.curso.m3.repositorio.OcupacionSala;
import com.atech.curso.m3.repositorio.ReservaRepository;
import com.atech.curso.m3.repositorio.ReservaResumen;
import com.atech.curso.m3.repositorio.ReservaSpecs;
import com.atech.curso.m3.repositorio.SalaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** EJ 3.2 y 3.3 - Slice de JPA sobre H2 con el esquema y los datos de Flyway. */
@DataJpaTest
class RepositoriosTest {

    @Autowired
    SalaRepository salas;

    @Autowired
    ReservaRepository reservas;

    @Test
    void consultasDerivadas() {
        assertThat(salas.findByNombre("Turing")).isPresent();
        assertThat(salas.findByCapacidadGreaterThanEqualOrderByCapacidadAsc(10))
                .extracting(Sala::getNombre).containsExactly("Turing", "Hopper");
        assertThat(salas.findByDireccionCiudadIgnoreCase("sevilla")).hasSize(2);
    }

    @Test
    void elRecordEmbebidoSeCargaCorrectamente() {
        Sala hopper = salas.findByNombre("Hopper").orElseThrow();
        assertThat(hopper.getDireccion().ciudad()).isEqualTo("Madrid");
        assertThat(hopper.getDireccion().codigoPostal()).isEqualTo("28014");
    }

    @Test
    void proyeccionPorInterfaz() {
        assertThat(reservas.findByUsuarioEmailOrderByInicioAsc("ana@atech.es"))
                .extracting(r -> r.getSala().getNombre())
                .containsExactly("Turing", "Lovelace");
    }

    @Test
    void proyeccionDtoConJpql() {
        assertThat(reservas.ocupacionPorSala()).containsExactly(
                new OcupacionSala("Turing", 2),
                new OcupacionSala("Hopper", 1),
                new OcupacionSala("Lovelace", 1));
    }

    @Test
    void detectaSolapes() {
        Long turing = salas.findByNombre("Turing").orElseThrow().getId();
        assertThat(reservas.existeSolape(turing, LocalDateTime.parse("2030-01-10T10:00"),
                LocalDateTime.parse("2030-01-10T10:30"), EstadoReserva.CONFIRMADA)).isTrue();
        assertThat(reservas.existeSolape(turing, LocalDateTime.parse("2030-01-10T11:00"),
                LocalDateTime.parse("2030-01-10T12:00"), EstadoReserva.CONFIRMADA)).isFalse();
    }

    @Test
    void busquedaDinamicaPaginadaConSpecifications() {
        var filtro = new ReservaSpecs.Filtro(null, "Sevilla", EstadoReserva.CONFIRMADA, null, null);

        Page<Reserva> pagina = reservas.findAll(ReservaSpecs.de(filtro),
                PageRequest.of(0, 2, Sort.by("inicio").descending()));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).getInicio()).isEqualTo(LocalDateTime.parse("2030-01-11T10:00"));
    }

    @Test
    void sinFiltrosDevuelveTodo() {
        var todo = new ReservaSpecs.Filtro(null, null, null, null, null);
        assertThat(reservas.count(ReservaSpecs.de(todo))).isEqualTo(5);
    }
}
