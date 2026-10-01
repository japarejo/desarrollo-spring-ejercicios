package com.atech.curso.m4.dominio;

/**
 * EJ 4.5 - Cada tipo de sala impone su propio aforo máximo. Lo usa la validación entre campos
 * {@code @CapacidadSegunTipo} del formulario.
 * <p>
 * EJ 4.6 - El nombre visible no está aquí: depende del idioma y sale de messages.properties
 * ({@code tipo.REUNIONES}...).
 */
public enum TipoSala {

    REUNIONES(20),
    FORMACION(40),
    AUDITORIO(500);

    private final int capacidadMaxima;

    TipoSala(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    /**
     * EJ 4.5 - La regla de negocio, escrita una sola vez y en el dominio. La usan el servicio (que la hace
     * cumplir siempre) y el validador del formulario (que la convierte en un error junto al campo).
     */
    public boolean admite(int capacidad) {
        return capacidad <= capacidadMaxima;
    }
}
