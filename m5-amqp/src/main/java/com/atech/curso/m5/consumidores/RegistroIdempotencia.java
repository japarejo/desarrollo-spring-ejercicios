package com.atech.curso.m5.consumidores;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * EJ 5.2 - RabbitMQ garantiza entrega "al menos una vez": un consumidor debe tolerar duplicados.
 * En producción se usaría una tabla con clave única (consumidor, messageId) en la misma transacción.
 */
@Component
public class RegistroIdempotencia {

    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    /** @return true si es la primera vez que se ve el mensaje para ese consumidor */
    public boolean primeraVez(String consumidor, String messageId) {
        return procesados.add(consumidor + "|" + messageId);
    }

    public void limpiar() {
        procesados.clear();
    }
}
