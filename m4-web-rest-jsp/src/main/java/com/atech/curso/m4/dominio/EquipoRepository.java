package com.atech.curso.m4.dominio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findAllByOrderByNombreAsc();
}
