package com.atech.curso.m4.dominio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Sala> findAllByOrderByNombreAsc();
}
