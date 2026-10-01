package com.atech.curso.m4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.atech.curso.m4.dominio.Equipo;
import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.SalaRepository;
import com.atech.curso.m4.dominio.TipoSala;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** EJ 4.5 - Filtro por tipo y carga del equipamiento con @EntityGraph, sobre los datos de data.sql. */
@DataJpaTest
class SalaRepositoryTest {

    @Autowired
    SalaRepository salas;

    @Test
    void filtraPorTipoYTraeElEquipamientoEnLaMismaConsulta() {
        List<Sala> auditorios = salas.findByTipoOrderByNombreAsc(TipoSala.AUDITORIO);

        assertThat(auditorios).extracting(Sala::getNombre).containsExactly("Berners-Lee");
        Sala sala = auditorios.getFirst();
        assertThat(Hibernate.isInitialized(sala.getEquipamiento())).isTrue();
        assertThat(sala.getEquipamiento()).extracting(Equipo::getNombre)
            .containsExactly("Pantalla", "Videoconferencia");
    }
}
