package com.atech.curso.m3.gobierno;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * EJ 3.7 - Un gobierno por legislatura. Sólo uno está vigente; al cesar se guarda la fecha de cese.
 * Los nombramientos se guardan en cascada con el gobierno (no hay que llamar a save por cada uno).
 */
@Entity
public class Gobierno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int legislatura;

    @Column(nullable = false)
    private LocalDateTime tomaPosesion;

    private LocalDateTime cese;

    private boolean vigente;

    // Se insertan en orden protocolario, así que ordenar por id devuelve ese mismo orden
    @OneToMany(mappedBy = "gobierno", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Nombramiento> nombramientos = new ArrayList<>();

    protected Gobierno() {
    }

    public Gobierno(int legislatura, LocalDateTime tomaPosesion) {
        this.legislatura = legislatura;
        this.tomaPosesion = tomaPosesion;
        this.vigente = true;
    }

    public Nombramiento nombrar(Organo organo, String titular) {
        Nombramiento nombramiento = new Nombramiento(this, organo, titular);
        nombramientos.add(nombramiento);
        return nombramiento;
    }

    public void cesar(LocalDateTime cuando) {
        if (!vigente) {
            throw new IllegalStateException("El gobierno de la legislatura " + legislatura + " ya estaba cesado");
        }
        this.vigente = false;
        this.cese = cuando;
    }

    public Long getId() {
        return id;
    }

    public int getLegislatura() {
        return legislatura;
    }

    public LocalDateTime getTomaPosesion() {
        return tomaPosesion;
    }

    public LocalDateTime getCese() {
        return cese;
    }

    public boolean isVigente() {
        return vigente;
    }

    public List<Nombramiento> getNombramientos() {
        return Collections.unmodifiableList(nombramientos);
    }
}
