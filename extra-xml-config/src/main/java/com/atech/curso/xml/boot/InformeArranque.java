package com.atech.curso.xml.boot;

import com.atech.curso.xml.reservas.Presupuesto;
import com.atech.curso.xml.reservas.ServicioReservas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bean <b>anotado</b> que recibe por constructor un bean <b>declarado en XML</b>.
 *
 * <p>Ésta es la idea que conviene que quede clara: para el código cliente no hay ninguna
 * diferencia. El estilo de configuración es un detalle del ensamblado, no del diseño.
 */
@Component
@ConditionalOnProperty(name = "atech.demo.enabled", havingValue = "true", matchIfMissing = true)
public class InformeArranque implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InformeArranque.class);

    private final ServicioReservas servicio;

    public InformeArranque(ServicioReservas servicio) {
        this.servicio = servicio;
    }

    /** Visible para el test: permite comprobar qué bean se ha inyectado. */
    ServicioReservas servicio() {
        return servicio;
    }

    @Override
    public void run(String... args) {
        Presupuesto presupuesto = servicio.presupuestar("S-1", 2);
        log.info("Presupuesto para {} ({} h): {} EUR con tarifa {}",
                presupuesto.nombreSala(), presupuesto.horas(), presupuesto.importe(), presupuesto.tarifa());
    }
}
