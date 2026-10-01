package com.atech.curso.m4.web;

import java.util.LinkedHashSet;
import java.util.Set;

import com.atech.curso.m4.dominio.TipoSala;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * EJ 4.1 - Objeto de formulario (JavaBean con getters/setters: lo necesitan las etiquetas form de Spring).
 * Los mensajes se resuelven desde messages.properties.
 * <p>
 * EJ 4.5 - {@code @CapacidadSegunTipo} es una restricción de clase: compara dos campos (tipo y capacidad).
 */
@CapacidadSegunTipo
public class SalaForm {

    private Long id;

    @NotBlank
    @Size(max = 50)
    private String nombre;

    @NotNull
    private TipoSala tipo;

    @Min(1)
    @Max(500)
    private int capacidad = 10;

    private boolean proyector;

    @NotBlank
    @Email
    @Size(max = 100)
    private String emailResponsable;

    // EJ 4.5 - Ids de los equipos marcados: el formulario no maneja entidades, las resuelve el servicio
    private Set<Long> equipamiento = new LinkedHashSet<>();

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

    public TipoSala getTipo() {
        return tipo;
    }

    public void setTipo(TipoSala tipo) {
        this.tipo = tipo;
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

    public String getEmailResponsable() {
        return emailResponsable;
    }

    public void setEmailResponsable(String emailResponsable) {
        this.emailResponsable = emailResponsable;
    }

    public Set<Long> getEquipamiento() {
        return equipamiento;
    }

    public void setEquipamiento(Set<Long> equipamiento) {
        this.equipamiento = equipamiento;
    }
}
