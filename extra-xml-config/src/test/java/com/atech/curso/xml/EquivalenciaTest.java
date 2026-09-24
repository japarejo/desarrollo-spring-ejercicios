package com.atech.curso.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.atech.curso.xml.configuracionjava.ConfiguracionJava;
import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * La moraleja del módulo: <b>XML y anotaciones son dos sintaxis para lo mismo</b>.
 *
 * <p>Los dos contextos declaran los mismos beans, con los mismos nombres, y se comportan igual.
 * Lo que cambia es cuándo se detectan los errores y cuánto ayuda el IDE, no el modelo de objetos.
 */
class EquivalenciaTest {

    @Test
    void ambasConfiguracionesDeclaranLosMismosBeans() {
        try (var xml = new ClassPathXmlApplicationContext("beans.xml");
                var java = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {

            assertThat(beansPropios(java)).containsExactlyInAnyOrderElementsOf(beansPropios(xml));
        }
    }

    @Test
    void ambasConfiguracionesProducenElMismoPresupuesto() {
        try (var xml = new ClassPathXmlApplicationContext("beans.xml");
                var java = new AnnotationConfigApplicationContext(ConfiguracionJava.class)) {

            Presupuesto conXml = xml.getBean(ServicioReservas.class).presupuestar("S-1", 4);
            Presupuesto conJava = java.getBean(ServicioReservas.class).presupuestar("S-1", 4);

            assertThat(conJava.idSala()).isEqualTo(conXml.idSala());
            assertThat(conJava.nombreSala()).isEqualTo(conXml.nombreSala());
            assertThat(conJava.horas()).isEqualTo(conXml.horas());
            assertThat(conJava.tarifa()).isEqualTo(conXml.tarifa());
            assertThat(conJava.importe()).isEqualByComparingTo(conXml.importe());
        }
    }

    /**
     * El contenedor basado en anotaciones registra además sus propios post-procesadores
     * ({@code internalConfigurationAnnotationProcessor}, {@code internalAutowiredAnnotationProcessor}...)
     * y la propia clase de configuración. El XML, al no llevar {@code <context:annotation-config/>},
     * solo contiene los beans declarados. Comparamos únicamente los beans de la aplicación.
     */
    private List<String> beansPropios(ApplicationContext contexto) {
        return Arrays.stream(((ConfigurableApplicationContext) contexto).getBeanDefinitionNames())
                .filter(nombre -> !nombre.startsWith("org.springframework"))
                .filter(nombre -> !ConfiguracionJava.class.equals(contexto.getType(nombre)))
                .toList();
    }
}
