package com.atech.curso.m3.repositorio;

import java.time.LocalDateTime;

/** EJ 3.2 - Proyección basada en interfaz (cerrada), con una proyección anidada. */
public interface ReservaResumen {

    Long getId();

    LocalDateTime getInicio();

    SalaNombre getSala();

    interface SalaNombre {
        String getNombre();
    }
}
