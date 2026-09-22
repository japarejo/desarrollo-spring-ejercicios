package com.atech.curso.m3.repositorio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;

import org.springframework.data.jpa.domain.Specification;

/** EJ 3.3 - Criterios reutilizables y combinables para búsquedas dinámicas. */
public final class ReservaSpecs {

    private ReservaSpecs() {
    }

    public record Filtro(String sala, String ciudad, EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta) {
    }

    public static Specification<Reserva> deSala(String nombre) {
        return (root, query, cb) -> cb.equal(root.get("sala").get("nombre"), nombre);
    }

    public static Specification<Reserva> enCiudad(String ciudad) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("sala").get("direccion").<String>get("ciudad")),
                ciudad.toLowerCase());
    }

    public static Specification<Reserva> enEstado(EstadoReserva estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Reserva> desde(LocalDateTime desde) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("inicio"), desde);
    }

    public static Specification<Reserva> hasta(LocalDateTime hasta) {
        return (root, query, cb) -> cb.lessThan(root.<LocalDateTime>get("inicio"), hasta);
    }

    /** Sólo se añaden los criterios informados. */
    public static Specification<Reserva> de(Filtro f) {
        List<Specification<Reserva>> specs = new ArrayList<>();
        if (f.sala() != null) {
            specs.add(deSala(f.sala()));
        }
        if (f.ciudad() != null) {
            specs.add(enCiudad(f.ciudad()));
        }
        if (f.estado() != null) {
            specs.add(enEstado(f.estado()));
        }
        if (f.desde() != null) {
            specs.add(desde(f.desde()));
        }
        if (f.hasta() != null) {
            specs.add(hasta(f.hasta()));
        }
        return Specification.allOf(specs);
    }
}
