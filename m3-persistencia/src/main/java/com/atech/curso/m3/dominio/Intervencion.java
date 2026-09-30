package com.atech.curso.m3.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter 
@Setter
@EqualsAndHashCode (of = {"id"})  
public class Intervencion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    LocalDateTime fechaHora;

    @Column(nullable = false, length = 255)
    private String contenido;

    @ManyToOne (optional = false)
    private Reserva reserva;
}
