package com.atech.curso.m1.pista;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * EJ 1.7 - Almacén de la pista de auditoría. En producción sería una tabla de solo inserción,
 * un log estructurado o un tópico de Kafka; lo importante es que el aspecto no sepa cuál.
 */
@Component
public class LibroAuditoria {

    private final List<ApunteAuditoria> apuntes = new CopyOnWriteArrayList<>();

    void anotar(ApunteAuditoria apunte) {
        apuntes.add(apunte);
    }

    public List<ApunteAuditoria> apuntes() {
        return List.copyOf(apuntes);
    }

    public void limpiar() {
        apuntes.clear();
    }
}
