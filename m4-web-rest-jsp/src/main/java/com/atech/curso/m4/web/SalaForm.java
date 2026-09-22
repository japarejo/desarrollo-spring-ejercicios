package com.atech.curso.m4.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * EJ 4.1 - Objeto de formulario (JavaBean con getters/setters: lo necesitan las etiquetas form de Spring).
 * Los mensajes se resuelven desde messages.properties.
 */
public class SalaForm {

    private Long id;

    @NotBlank
    @Size(max = 50)
    private String nombre;

    @Min(1)
    @Max(500)
    private int capacidad = 10;

    private boolean proyector;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isProyector() {
        return proyector;
    }

    public void setProyector(boolean proyector) {
        this.proyector = proyector;
    }
}
