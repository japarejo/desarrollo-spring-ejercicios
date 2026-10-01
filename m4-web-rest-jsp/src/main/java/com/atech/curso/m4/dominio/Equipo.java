package com.atech.curso.m4.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * EJ 4.5 - Equipamiento que puede tener una sala (pizarra, pantalla...). Es una entidad con su propia tabla:
 * se pueden dar de alta equipos nuevos sin tocar el código. Una sala tiene varios y un equipo está en varias
 * salas ({@code @ManyToMany} en {@link Sala}).
 */
@Entity
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    protected Equipo() {
    }

    public Equipo(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
