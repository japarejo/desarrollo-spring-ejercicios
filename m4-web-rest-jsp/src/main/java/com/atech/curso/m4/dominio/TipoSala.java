package com.atech.curso.m4.dominio;

/**
 * EJ 4.5 - Cada tipo de sala impone su propio aforo máximo. Lo usa la validación entre campos
 * {@code @CapacidadSegunTipo} del formulario.
 */
public enum TipoSala {

    REUNIONES("Reuniones", 20),
    FORMACION("Formación", 40),
    AUDITORIO("Auditorio", 500);

    private final String descripcion;
    private final int capacidadMaxima;

    TipoSala(String descripcion, int capacidadMaxima) {
        this.descripcion = descripcion;
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }
}
