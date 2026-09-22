package com.atech.curso.m2.reservas;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sala;
    private String usuario;
    private LocalDate fecha;
    private LocalTime inicio;
    private LocalTime fin;

    protected Reserva() {
    }

    public Reserva(String sala, String usuario, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        this.sala = sala;
        this.usuario = usuario;
        this.fecha = fecha;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() {
        return id;
    }

    public String getSala() {
        return sala;
    }

    public String getUsuario() {
        return usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public LocalTime getFin() {
        return fin;
    }
}
