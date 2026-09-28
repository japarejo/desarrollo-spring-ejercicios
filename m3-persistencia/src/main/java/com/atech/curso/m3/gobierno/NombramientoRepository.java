package com.atech.curso.m3.gobierno;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NombramientoRepository extends JpaRepository<Nombramiento, Long> {

    /** Proyección por interfaz rellenada con los alias («as ...») de una consulta JPQL. */
    interface Trayectoria {
        String getTitular();

        String getOrgano();

        int getLegislatura();
    }

    /** La trayectoria de un superhéroe en todos los gobiernos (búsqueda parcial, sin distinguir mayúsculas). */
    @Query("""
            select n.titular as titular, n.organo.nombre as organo, n.gobierno.legislatura as legislatura
            from Nombramiento n
            where lower(n.titular) like lower(concat('%', :texto, '%'))
            order by n.gobierno.legislatura, n.organo.orden
            """)
    List<Trayectoria> trayectoria(@Param("texto") String texto);

    /** Consulta derivada que navega por la relación con el gobierno. */
    long countByGobiernoLegislatura(int legislatura);

    @Query("select n.titular from Nombramiento n where n.gobierno.legislatura = :legislatura order by n.id")
    List<String> titularesDe(@Param("legislatura") int legislatura);
}
