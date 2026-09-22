package com.atech.curso.m5.consumidores;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.atech.curso.m5.config.Topologia;
import com.atech.curso.m5.eventos.ReservaConfirmada;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * EJ 5.2/5.3 - Consumidor idempotente. Un importe negativo simula un error de negocio: el
 * listener lo reintenta (backoff exponencial) y, al agotar los intentos, acaba en reservas.errores.
 */
@Component
public class FacturacionListener {

    private static final Logger log = LoggerFactory.getLogger(FacturacionListener.class);

    private final RegistroIdempotencia idempotencia;
    private final List<String> facturas = new CopyOnWriteArrayList<>();
    private final AtomicInteger intentos = new AtomicInteger();

    public FacturacionListener(RegistroIdempotencia idempotencia) {
        this.idempotencia = idempotencia;
    }

    @RabbitListener(queues = Topologia.COLA_FACTURACION)
    public void facturar(ReservaConfirmada evento,
            @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        intentos.incrementAndGet();
        String clave = messageId != null ? messageId : evento.reservaId();
        if (evento.importe().signum() < 0) {
            throw new IllegalArgumentException("Importe negativo en la reserva " + evento.reservaId());
        }
        if (!idempotencia.primeraVez("facturacion", clave)) {
            log.info("Duplicado ignorado: {}", clave);
            return;
        }
        facturas.add(evento.reservaId());
        log.info("Factura emitida para la reserva {} ({} EUR)", evento.reservaId(), evento.importe());
    }

    public List<String> facturas() {
        return List.copyOf(facturas);
    }

    public int intentos() {
        return intentos.get();
    }

    public void limpiar() {
        facturas.clear();
        intentos.set(0);
    }
}
