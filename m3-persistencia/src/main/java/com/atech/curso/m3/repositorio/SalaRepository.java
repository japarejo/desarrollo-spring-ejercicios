package com.atech.curso.m3.repositorio;

import java.util.List;
import java.util.Optional;

import com.atech.curso.m3.dominio.Sala;

import org.springframework.data.jpa.repository.JpaRepository;

/** EJ 3.2 - Consultas derivadas del nombre del método (incluida una propiedad embebida). */
public interface SalaRepository extends JpaRepository<Sala, Long> {

    Optional<Sala> findByNombre(String nombre);

    List<Sala> findByCapacidadGreaterThanEqualOrderByCapacidadAsc(int capacidadMinima);

    List<Sala> findByDireccionCiudadIgnoreCase(String ciudad);
}
