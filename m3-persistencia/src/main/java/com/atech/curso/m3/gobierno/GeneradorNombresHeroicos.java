package com.atech.curso.m3.gobierno;

import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/**
 * EJ 3.7 - Nombres de superhéroe aleatorios: un sustantivo y un adjetivo, sin concordancia
 * («Vengador Holístico», «Aguja Dinámico»). Es un bean normal: no sabe nada de JPA ni de transacciones.
 */
@Component
public class GeneradorNombresHeroicos {

    static final List<String> SUSTANTIVOS = List.of(
            "Vengador", "Aguja", "Centella", "Tornado", "Halcón", "Titán", "Relámpago", "Coloso", "Pantera",
            "Meteoro", "Escarabajo", "Capitán", "Fantasma", "Cometa", "Tifón", "Murciélago", "Guardián",
            "Martillo", "Trueno", "Gladiador", "Lince", "Búho", "Mapache", "Grapadora", "Tostadora", "Chancla",
            "Botijo", "Fregona", "Croqueta", "Chubasquero", "Pulpo", "Ventilador");

    static final List<String> ADJETIVOS = List.of(
            "Holístico", "Dinámico", "Sinérgico", "Transversal", "Resiliente", "Disruptivo", "Proactivo",
            "Escalable", "Cuántico", "Empoderado", "Sostenible", "Inoxidable", "Galáctico", "Invisible",
            "Atómico", "Magnético", "Implacable", "Supersónico", "Omnicanal", "Ágil", "Blockchain",
            "Hipervitaminado", "Low-cost", "Deconstruido", "Termonuclear", "Premium", "Interdepartamental",
            "Biodegradable");

    private final RandomGenerator azar;

    public GeneradorNombresHeroicos(RandomGenerator azar) {
        this.azar = azar;
    }

    public String generar() {
        return elegir(SUSTANTIVOS) + " " + elegir(ADJETIVOS);
    }

    /** Un nombre que no esté ya en {@code usados} (en un mismo gobierno no se repiten superhéroes). */
    public String generarDistinto(Set<String> usados) {
        if (usados.size() >= combinacionesPosibles()) {
            throw new IllegalStateException("Se han agotado los superhéroes disponibles");
        }
        String nombre;
        do {
            nombre = generar();
        } while (usados.contains(nombre));
        return nombre;
    }

    public int combinacionesPosibles() {
        return SUSTANTIVOS.size() * ADJETIVOS.size();
    }

    private String elegir(List<String> opciones) {
        return opciones.get(azar.nextInt(opciones.size()));
    }
}
