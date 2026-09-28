package com.atech.curso.m3.gobierno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** EJ 3.7 - Un puesto del gobierno (presidencia, ministerio...). Es un catálogo fijo cargado por Flyway (V3). */
@Entity
public class Organo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private TipoOrgano tipo;

    /** Orden protocolario: también es el orden en el que se nombra a los titulares. */
    private int orden;

    protected Organo() {
    }

    public Organo(String nombre, TipoOrgano tipo, int orden) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoOrgano getTipo() {
        return tipo;
    }

    public int getOrden() {
        return orden;
    }
}
