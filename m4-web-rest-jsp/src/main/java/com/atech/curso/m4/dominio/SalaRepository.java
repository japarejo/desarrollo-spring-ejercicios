package com.atech.curso.m4.dominio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // EJ 4.5 - El equipamiento viaja en la misma consulta (JOIN): la JSP lo pinta sin LazyInitializationException
    @EntityGraph(attributePaths = "equipamiento")
    List<Sala> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "equipamiento")
    List<Sala> findByTipoOrderByNombreAsc(TipoSala tipo);

    @Override
    @EntityGraph(attributePaths = "equipamiento")
    Optional<Sala> findById(Long id);
}
