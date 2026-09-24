package com.atech.curso.xml.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * El camino de migración: Spring Boot con {@code @ImportResource("classpath:beans.xml")}.
 *
 * <p>Los beans del XML quedan registrados en el mismo contenedor que los anotados y se inyectan
 * entre sí con naturalidad.
 */
@SpringBootTest(properties = "atech.demo.enabled=true")
class ImportResourceTest {

    @Autowired
    ApplicationContext contexto;

    @Autowired
    ServicioReservas servicio;

    @Test
    void losBeansDelXmlEstanDisponiblesEnElContenedorDeBoot() {
        assertThat(contexto.containsBean("servicioReservas")).isTrue();
        assertThat(contexto.containsBean("repositorioSalas")).isTrue();

        Presupuesto presupuesto = servicio.presupuestar("S-3", 2);

        assertThat(presupuesto.nombreSala()).isEqualTo("Hopper");
        assertThat(presupuesto.importe()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void unBeanAnotadoRecibePorConstructorUnBeanDeclaradoEnXml() {
        // InformeArranque es un @Component; su colaborador viene del XML. Que el contexto
        // arranque ya demuestra que la inyección cruzada funciona.
        InformeArranque informe = contexto.getBean(InformeArranque.class);

        assertThat(informe.servicio())
                .as("el bean anotado recibe exactamente el bean declarado en beans.xml")
                .isSameAs(contexto.getBean("servicioReservas"));
    }
}
