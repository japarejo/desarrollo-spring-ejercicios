package com.atech.curso.xml.demo;

import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * El contenedor de Spring <b>sin Spring Boot</b>: aquí se ve que {@code ApplicationContext} es un
 * objeto normal que se crea, se usa y se cierra.
 *
 * <p>Es el modo en que arrancaban las aplicaciones Spring antes de Boot, y sigue siendo útil para
 * entender qué hace realmente {@code SpringApplication.run(...)} por debajo.
 *
 * <p>Ejecutar con: {@code ./mvnw -pl extra-xml-config exec:java -Dexec.mainClass=com.atech.curso.xml.demo.DemoXml}
 */
public final class DemoXml {

    private DemoXml() {
    }

    public static void main(String[] args) {
        // try-with-resources: al salir se llaman los destroy-method declarados en el XML
        try (ClassPathXmlApplicationContext contexto = new ClassPathXmlApplicationContext("beans.xml")) {
            System.out.println("Beans declarados: " + String.join(", ", contexto.getBeanDefinitionNames()));

            ServicioReservas servicio = contexto.getBean(ServicioReservas.class);
            Presupuesto presupuesto = servicio.presupuestar("S-2", 3);
            System.out.printf("Presupuesto: %s (%s) %d h -> %s EUR [tarifa %s]%n",
                    presupuesto.nombreSala(), presupuesto.idSala(), presupuesto.horas(),
                    presupuesto.importe(), presupuesto.tarifa());
        }
    }
}
