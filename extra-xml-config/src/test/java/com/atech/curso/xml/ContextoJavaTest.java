package com.atech.curso.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.atech.curso.xml.configuracionjava.ConfiguracionJava;
import com.atech.curso.xml.dominio.RepositorioSalasEnMemoria;
import com.atech.curso.xml.reservas.BorradorReserva;
import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;
import com.atech.curso.xml.tarifas.Tarifa;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Las mismas comprobaciones que {@link ContextoXmlTest}, pero con configuración Java. */
class ContextoJavaTest {

    @Test
    void elContenedorEnsamblaElMismoGrafoConAnotaciones() {
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {
            ServicioReservas servicio = contexto.getBean("servicioReservas", ServicioReservas.class);

            Presupuesto presupuesto = servicio.presupuestar("S-2", 3);

            assertThat(presupuesto.nombreSala()).isEqualTo("Lovelace");
            assertThat(presupuesto.importe()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(presupuesto.tarifa()).isEqualTo("POR_HORA");
        }
    }

    @Test
    void primaryDecideCualDeLasDosTarifasSeInyecta() {
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {
            assertThat(contexto.getBeanNamesForType(Tarifa.class))
                    .containsExactlyInAnyOrder("tarifaPorHora", "tarifaPlana");
            assertThat(contexto.getBean(Tarifa.class).nombre()).isEqualTo("POR_HORA");
        }
    }

    @Test
    void singletonDevuelveLaMismaInstanciaYPrototypeUnaNueva() {
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {
            assertThat(contexto.getBean(ServicioReservas.class))
                    .isSameAs(contexto.getBean(ServicioReservas.class));
            assertThat(contexto.getBean(BorradorReserva.class))
                    .isNotSameAs(contexto.getBean(BorradorReserva.class));
        }
    }

    @Test
    void initMethodYDestroyMethodDeclaradosEnLaAnotacionBean() {
        RepositorioSalasEnMemoria repositorio;
        try (var contexto = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {
            repositorio = contexto.getBean("repositorioSalas", RepositorioSalasEnMemoria.class);
            assertThat(repositorio.iniciado()).isTrue();
        }
        assertThat(repositorio.cerrado()).isTrue();
    }
}
