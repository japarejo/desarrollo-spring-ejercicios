package com.atech.curso.xml.reservas;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.NoSuchElementException;

import com.atech.curso.xml.dominio.RepositorioSalas;
import com.atech.curso.xml.dominio.Sala;
import com.atech.curso.xml.tarifas.Tarifa;

/**
 * Servicio con <b>inyección por constructor</b> y tres colaboradores: un repositorio, una
 * estrategia de precio y un reloj. Es la clase que conviene mirar al comparar {@code beans.xml}
 * con {@code ConfiguracionJava}: el código es idéntico, cambia solo quién lo ensambla.
 */
public class ServicioReservas {

    private final RepositorioSalas repositorio;
    private final Tarifa tarifa;
    private final Clock clock;

    public ServicioReservas(RepositorioSalas repositorio, Tarifa tarifa, Clock clock) {
        this.repositorio = repositorio;
        this.tarifa = tarifa;
        this.clock = clock;
    }

    public Presupuesto presupuestar(String idSala, int horas) {
        if (horas <= 0) {
            throw new IllegalArgumentException("Las horas deben ser positivas");
        }
        Sala sala = repositorio.porId(idSala)
                .orElseThrow(() -> new NoSuchElementException("No existe la sala " + idSala));
        BigDecimal importe = tarifa.importe(sala, horas);
        return new Presupuesto(sala.id(), sala.nombre(), horas, importe, tarifa.nombre(), clock.instant());
    }

    public Tarifa tarifa() {
        return tarifa;
    }
}
