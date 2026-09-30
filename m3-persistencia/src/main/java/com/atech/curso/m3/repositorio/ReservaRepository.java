package com.atech.curso.m3.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    /** EJ 3.2 - Consulta derivada con proyección por interfaz. */
    List<ReservaResumen> findByUsuarioEmailOrderByInicioAsc(String email);

    /** EJ 3.4 - Provoca N+1 si después se navega a sala/usuario (relaciones LAZY). */    
    List<Reserva> findByEstado(EstadoReserva estado);    


    /** EJ 3.4 - Solución: un único SELECT con JOIN gracias al grafo de entidad. */
    @EntityGraph(attributePaths = { "sala", "usuario" })
    @Query("select r from Reserva r where r.estado = :estado")
    List<Reserva> buscarConSalaYUsuario(@Param("estado") EstadoReserva estado);

    /**
     * EJ 3.4 - El grafo también se puede poner en un método heredado redeclarándolo. Así la búsqueda con
     * Specifications trae sala y usuario, y se pueden leer fuera de la transacción (lo usa la consola).
     */
    @Override
    @EntityGraph(attributePaths = { "sala", "usuario" })
    Page<Reserva> findAll(Specification<Reserva> spec, Pageable pageable);

    /** EJ 3.2 - JPQL con parámetros con nombre: ¿hay solape en la sala? */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.sala.id = :salaId and r.estado = :estado
              and r.inicio < :fin and r.fin > :inicio
            """)
    boolean existeSolape(@Param("salaId") Long salaId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin, @Param("estado") EstadoReserva estado);

    /** EJ 3.2 - Proyección DTO con constructor expression. */
    @Query("""
            select new com.atech.curso.m3.repositorio.OcupacionSala(r.sala.nombre, count(r))
            from Reserva r where r.estado = com.atech.curso.m3.dominio.EstadoReserva.CONFIRMADA
            group by r.sala.nombre order by count(r) desc, r.sala.nombre
            """)
    List<OcupacionSala> ocupacionPorSala();
}
