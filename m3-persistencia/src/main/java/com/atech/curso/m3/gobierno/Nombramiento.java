package com.atech.curso.m3.gobierno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** EJ 3.7 - Quién ocupa cada órgano en un gobierno concreto. */
@Entity
public class Nombramiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gobierno_id")
    private Gobierno gobierno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organo_id")
    private Organo organo;

    @Column(nullable = false, length = 100)
    private String titular;

    protected Nombramiento() {
    }

    Nombramiento(Gobierno gobierno, Organo organo, String titular) {
        this.gobierno = gobierno;
        this.organo = organo;
        this.titular = titular;
    }

    public Long getId() {
        return id;
    }

    public Gobierno getGobierno() {
        return gobierno;
    }

    public Organo getOrgano() {
        return organo;
    }

    public String getTitular() {
        return titular;
    }
}
