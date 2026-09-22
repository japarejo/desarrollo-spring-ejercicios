package com.atech.curso.m3;

import static org.assertj.core.api.Assertions.assertThat;

import com.atech.curso.m3.repositorio.ReservaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * EJ 3.6 - Las mismas migraciones y consultas contra un PostgreSQL real en Docker.
 * Si no hay Docker disponible el test se omite (no falla).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    ReservaRepository reservas;

    @Test
    void flywayYConsultasFuncionanEnPostgres() {
        assertThat(reservas.count()).isEqualTo(5);
        assertThat(reservas.ocupacionPorSala()).hasSize(3);
    }
}
