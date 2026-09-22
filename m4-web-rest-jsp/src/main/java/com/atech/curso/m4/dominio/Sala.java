package com.atech.curso.m4.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    private int capacidad;

    private boolean proyector;

    protected Sala() {
    }

    public Sala(String nombre, int capacidad, boolean proyector) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.proyector = proyector;
    }

    public void actualizar(String nombre, int capacidad, boolean proyector) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.proyector = proyector;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public boolean isProyector() {
        return proyector;
    }
}
