package com.atech.curso.m2.reservas;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.atech.curso.m2.config.ReservasProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** EJ 2.1/2.4 - Usa las propiedades tipadas y publica una métrica propia. */
@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository repositorio;
    private final ReservasProperties propiedades;
    private final Clock clock;
    private final Counter creadas;

    public ReservaService(ReservaRepository repositorio, ReservasProperties propiedades, Clock clock,
            MeterRegistry registry) {
        this.repositorio = repositorio;
        this.propiedades = propiedades;
        this.clock = clock;
        this.creadas = Counter.builder("atech.reservas.creadas")
                .description("Reservas creadas desde el arranque")
                .register(registry);
    }

    public List<Reserva> listar() {
        return repositorio.findAll();
    }

    public Optional<Reserva> buscar(Long id) {
        return repositorio.findById(id);
    }

    @Transactional
    public Reserva crear(NuevaReserva nueva) {
        if (!propiedades.salaPermitida(nueva.sala())) {
            throw new ReservaInvalidaException("Sala desconocida: " + nueva.sala());
        }
        if (nueva.fecha().isBefore(LocalDate.now(clock))) {
            throw new ReservaInvalidaException("No se puede reservar en el pasado");
        }
        if (!nueva.fin().isAfter(nueva.inicio())) {
            throw new ReservaInvalidaException("La hora de fin debe ser posterior a la de inicio");
        }
        if (nueva.inicio().getHour() < propiedades.horaApertura()
                || nueva.fin().getHour() > propiedades.horaCierre()) {
            throw new ReservaInvalidaException("Fuera del horario de apertura");
        }
        if (Duration.between(nueva.inicio(), nueva.fin()).compareTo(propiedades.duracionMaxima()) > 0) {
            throw new ReservaInvalidaException("Duración máxima: " + propiedades.duracionMaxima());
        }
        Reserva reserva = repositorio.save(
                new Reserva(nueva.sala(), nueva.usuario(), nueva.fecha(), nueva.inicio(), nueva.fin()));
        creadas.increment();
        return reserva;
    }
}
