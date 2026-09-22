package com.atech.curso.m1.notificacion;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * EJ 1.1 - Implementación por defecto (activa salvo que el perfil "sms" esté activo).
 *
 * <p>EJ 1.6 - Simula un servidor SMTP inestable: {@link #simularFallos(int)} hace que las
 * siguientes llamadas fallen. {@code @Retryable} (Spring Retry) reintenta de forma transparente.
 * En Spring Boot 4 se usa {@code org.springframework.resilience.annotation.Retryable}.
 */
@Component
@Profile("!sms")
public class EmailNotificador extends NotificadorEnMemoria {

    private final AtomicInteger fallosPendientes = new AtomicInteger();
    private final AtomicInteger intentos = new AtomicInteger();

    @Override
    public String canal() {
        return "email";
    }

    @Override
    @Retryable(retryFor = IllegalStateException.class, maxAttempts = 3, backoff = @Backoff(delay = 50, multiplier = 2))
    public void notificar(String destinatario, String mensaje) {
        intentos.incrementAndGet();
        if (fallosPendientes.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            log.warn("Servidor SMTP no disponible, se reintentará");
            throw new IllegalStateException("SMTP no disponible");
        }
        registrar(destinatario, mensaje);
    }

    public void simularFallos(int fallos) {
        fallosPendientes.set(fallos);
    }

    public int intentos() {
        return intentos.get();
    }

    @Override
    public void limpiar() {
        super.limpiar();
        intentos.set(0);
        fallosPendientes.set(0);
    }
}
