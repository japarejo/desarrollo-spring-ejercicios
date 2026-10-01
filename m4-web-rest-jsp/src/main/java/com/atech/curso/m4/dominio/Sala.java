package com.atech.curso.m4.dominio;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // EJ 4.5 - STRING y no ORDINAL: reordenar el enum no debe cambiar el significado de los datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSala tipo;

    private int capacidad;

    private boolean proyector;

    @Column(length = 100)
    private String emailResponsable;

    // EJ 4.5 - Relación N:M con tabla intermedia sala_equipo(sala_id, equipo_id). Es LAZY por defecto y con
    // open-in-view=false hay que cargarla antes de llegar a la JSP: @EntityGraph en SalaRepository.
    @ManyToMany
    @JoinTable(name = "sala_equipo",
            joinColumns = @JoinColumn(name = "sala_id"),
            inverseJoinColumns = @JoinColumn(name = "equipo_id"))
    @OrderBy("nombre")
    private Set<Equipo> equipamiento = new LinkedHashSet<>();

    protected Sala() {
    }

    public Sala(String nombre, TipoSala tipo, int capacidad, boolean proyector) {
        this(nombre, tipo, capacidad, proyector, null, Set.of());
    }

    public Sala(String nombre, TipoSala tipo, int capacidad, boolean proyector, String emailResponsable,
            Set<Equipo> equipamiento) {
        actualizar(nombre, tipo, capacidad, proyector, emailResponsable, equipamiento);
    }

    public void actualizar(String nombre, TipoSala tipo, int capacidad, boolean proyector,
            String emailResponsable, Set<Equipo> equipamiento) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.proyector = proyector;
        this.emailResponsable = emailResponsable;
        // Se modifica la colección existente: Hibernate la vigila y actualiza las filas de sala_equipo
        this.equipamiento.clear();
        this.equipamiento.addAll(equipamiento);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public boolean isProyector() {
        return proyector;
    }

    public String getEmailResponsable() {
        return emailResponsable;
    }

    public Set<Equipo> getEquipamiento() {
        return Collections.unmodifiableSet(equipamiento);
    }
}
