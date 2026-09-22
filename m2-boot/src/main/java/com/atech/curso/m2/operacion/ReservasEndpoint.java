package com.atech.curso.m2.operacion;

import java.util.LinkedHashMap;
import java.util.Map;

import com.atech.curso.m2.config.ReservasProperties;
import com.atech.curso.m2.reservas.ReservaRepository;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/** EJ 2.4 - Endpoint de Actuator propio: GET /actuator/reservas. */
@Component
@Endpoint(id = "reservas")
public class ReservasEndpoint {

    private final ReservaRepository repositorio;
    private final ReservasProperties propiedades;

    public ReservasEndpoint(ReservaRepository repositorio, ReservasProperties propiedades) {
        this.repositorio = repositorio;
        this.propiedades = propiedades;
    }

    @ReadOperation
    public Map<String, Object> resumen() {
        Map<String, Long> porSala = new LinkedHashMap<>();
        propiedades.salas().forEach(s -> porSala.put(s, repositorio.countBySala(s)));
        return Map.of("total", repositorio.count(), "porSala", porSala);
    }
}
