package com.atech.curso.m7.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * EJ 7.2 - Seguridad a nivel de método. Almacén en memoria para centrarnos en la seguridad.
 */
@Service
public class ReservaService {

    private final Map<Long, Reserva> reservas = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    public ReservaService() {
        crearInterna("Turing", "ana", LocalDateTime.of(2030, 1, 10, 9, 0));
        crearInterna("Hopper", "admin", LocalDateTime.of(2030, 1, 10, 10, 0));
        crearInterna("Lovelace", "ana", LocalDateTime.of(2030, 1, 11, 9, 0));
        crearInterna("Turing", "luis", LocalDateTime.of(2030, 1, 12, 9, 0));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Reserva> listar() {
        return reservas.values().stream().sorted(Comparator.comparingLong(Reserva::id)).toList();
    }

    /** Devuelve todas, pero Spring Security filtra las que no son del usuario (la lista debe ser mutable). */
    @PreAuthorize("isAuthenticated()")
    @PostFilter("filterObject.propietario() == authentication.name")
    public List<Reserva> mias() {
        return new ArrayList<>(listar());
    }

    @PreAuthorize("hasRole('USER')")
    public Reserva crear(String sala, LocalDateTime inicio, String propietario) {
        return crearInterna(sala, propietario, inicio);
    }

    /** Sólo el administrador o el propietario pueden cancelar. Se invoca un bean desde SpEL. */
    @PreAuthorize("hasRole('ADMIN') or @reservaService.esPropietario(#id, authentication.name)")
    public void cancelar(long id) {
        if (reservas.remove(id) == null) {
            throw new ReservaNoEncontradaException(id);
        }
    }

    public boolean esPropietario(long id, String usuario) {
        Reserva r = reservas.get(id);
        return r != null && r.propietario().equals(usuario);
    }

    private Reserva crearInterna(String sala, String propietario, LocalDateTime inicio) {
        Reserva r = new Reserva(secuencia.incrementAndGet(), sala, propietario, inicio);
        reservas.put(r.id(), r);
        return r;
    }
}
