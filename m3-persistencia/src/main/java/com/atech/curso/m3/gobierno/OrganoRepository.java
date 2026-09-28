package com.atech.curso.m3.gobierno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganoRepository extends JpaRepository<Organo, Long> {

    List<Organo> findAllByOrderByOrdenAsc();

    long countByTipo(TipoOrgano tipo);
}
