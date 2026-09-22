package com.atech.curso.m4.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Sala sala;

    private String usuario;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    protected Reserva() {
    }

    public Reserva(Sala sala, String usuario, LocalDateTime inicio, LocalDateTime fin) {
        this.sala = sala;
        this.usuario = usuario;
        this.inicio = inicio;
        this.fin = fin;
    }

    public void modificar(Sala sala, LocalDateTime inicio, LocalDateTime fin) {
        this.sala = sala;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() {
        return id;
    }

    public Sala getSala() {
        return sala;
    }

    public String getUsuario() {
        return usuario;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }
}
