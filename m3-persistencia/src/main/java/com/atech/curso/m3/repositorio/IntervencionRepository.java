package com.atech.curso.m3.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atech.curso.m3.dominio.Intervencion;

public interface IntervencionRepository extends JpaRepository<Intervencion, Long> {
    
    public List<Intervencion> findByReservaIdOrderByFechaHora(Long reservaId);
}
