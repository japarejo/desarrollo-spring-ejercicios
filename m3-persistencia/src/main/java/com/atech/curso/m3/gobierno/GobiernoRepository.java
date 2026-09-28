package com.atech.curso.m3.gobierno;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GobiernoRepository extends JpaRepository<Gobierno, Long> {

    /** Consulta derivada: el gobierno que está en el poder (sin cargar sus nombramientos). */
    Optional<Gobierno> findByVigenteTrue();

    /** El gobierno vigente con sus nombramientos y órganos en una sola consulta (EJ 3.4). */
    @EntityGraph(attributePaths = { "nombramientos", "nombramientos.organo" })
    @Query("select g from Gobierno g where g.vigente = true")
    Optional<Gobierno> buscarVigenteConNombramientos();

    @Query("select coalesce(max(g.legislatura), 0) from Gobierno g")
    int ultimaLegislatura();

    /** Proyección DTO: cada gobierno con el número de cargos que llegó a tener. */
    @Query("""
            select new com.atech.curso.m3.gobierno.ResumenGobierno(
                   g.legislatura, g.tomaPosesion, g.cese, g.vigente, count(n))
            from Gobierno g left join g.nombramientos n
            group by g.id, g.legislatura, g.tomaPosesion, g.cese, g.vigente
            order by g.legislatura
            """)
    List<ResumenGobierno> historico();
}
