package com.atech.curso.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.atech.curso.xml.dominio.RepositorioSalasEnMemoria;
import com.atech.curso.xml.reservas.BorradorReserva;
import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/** El contenedor configurado con {@code beans.xml}, sin Spring Boot de por medio. */
class ContextoXmlTest {

    @Test
    void elContenedorEnsamblaElGrafoDeclaradoEnElXml() {
        try (var contexto = new ClassPathXmlApplicationContext("beans.xml")) {
            ServicioReservas servicio = contexto.getBean("servicioReservas", ServicioReservas.class);

            Presupuesto presupuesto = servicio.presupuestar("S-2", 3);

            assertThat(presupuesto.nombreSala()).isEqualTo("Lovelace");
            // 25.00 EUR/h (inyectado con <property>) x 3 h
            assertThat(presupuesto.importe()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(presupuesto.tarifa()).isEqualTo("POR_HORA");
        }
    }

    @Test
    void primaryDecideCualDeLasDosTarifasSeInyecta() {
        try (var contexto = new ClassPathXmlApplicationContext("beans.xml")) {
            // Hay dos beans de tipo Tarifa declarados...
            assertThat(contexto.getBeanNamesForType(com.atech.curso.xml.tarifas.Tarifa.class))
                    .containsExactlyInAnyOrder("tarifaPorHora", "tarifaPlana");
            // ...y primary="true" resuelve la ambigüedad al pedir por tipo
            assertThat(contexto.getBean(com.atech.curso.xml.tarifas.Tarifa.class).nombre()).isEqualTo("POR_HORA");
        }
    }

    @Test
    void singletonDevuelveLaMismaInstanciaYPrototypeUnaNueva() {
        try (var contexto = new ClassPathXmlApplicationContext("beans.xml")) {
            assertThat(contexto.getBean(ServicioReservas.class))
                    .isSameAs(contexto.getBean(ServicioReservas.class));

            BorradorReserva uno = contexto.getBean(BorradorReserva.class);
            BorradorReserva otro = contexto.getBean(BorradorReserva.class);
            assertThat(uno).isNotSameAs(otro);
        }
    }

    @Test
    void initMethodYDestroyMethodSeInvocanAunqueLaClaseNoEsteAnotada() {
        RepositorioSalasEnMemoria repositorio;
        try (var contexto = new ClassPathXmlApplicationContext("beans.xml")) {
            repositorio = contexto.getBean("repositorioSalas", RepositorioSalasEnMemoria.class);
            assertThat(repositorio.iniciado()).as("init-method").isTrue();
            assertThat(repositorio.cerrado()).isFalse();
        }
        assertThat(repositorio.cerrado()).as("destroy-method al cerrar el contexto").isTrue();
    }
}
