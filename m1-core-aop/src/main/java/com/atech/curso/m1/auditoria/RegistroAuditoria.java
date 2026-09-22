package com.atech.curso.m1.auditoria;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/** Almacén en memoria de las entradas de auditoría (en producción: BD, log estructurado, Kafka...). */
@Component
public class RegistroAuditoria {

    public record Entrada(String operacion, String metodo, Duration duracion, boolean correcta) {
    }

    private final List<Entrada> entradas = new CopyOnWriteArrayList<>();

    void registrar(Entrada entrada) {
        entradas.add(entrada);
    }

    public List<Entrada> entradas() {
        return List.copyOf(entradas);
    }

    public void limpiar() {
        entradas.clear();
    }
}
