package com.atech.curso.m5.productor;

import java.util.concurrent.CompletableFuture;

import com.atech.curso.m5.config.Topologia;
import com.atech.curso.m5.eventos.ReservaConfirmada;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * EJ 5.2/5.4 - Productor. Con publisher confirms (correlated) cada envío devuelve un futuro que se
 * completa cuando el broker confirma (ack) o rechaza (nack) el mensaje.
 */
@Component
public class PublicadorReservas {

    private static final Logger log = LoggerFactory.getLogger(PublicadorReservas.class);

    private final RabbitTemplate rabbit;

    public PublicadorReservas(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
        // Mensajes no enrutables (mandatory=true): el broker los devuelve en lugar de descartarlos
        this.rabbit.setReturnsCallback(r -> log.error("Mensaje devuelto por el broker: {} {} (rk={})",
                r.getReplyCode(), r.getReplyText(), r.getRoutingKey()));
    }

    public CompletableFuture<CorrelationData.Confirm> publicar(ReservaConfirmada evento) {
        return publicar(Topologia.RK_CONFIRMADA, evento);
    }

    public CompletableFuture<CorrelationData.Confirm> publicar(String routingKey, ReservaConfirmada evento) {
        CorrelationData correlacion = new CorrelationData(evento.reservaId() + ":" + System.nanoTime());
        rabbit.convertAndSend(Topologia.EXCHANGE, routingKey, evento, mensaje -> {
            // messageId = clave de idempotencia para los consumidores
            mensaje.getMessageProperties().setMessageId(evento.reservaId());
            mensaje.getMessageProperties().setHeader("x-origen", "m5-amqp");
            return mensaje;
        }, correlacion);
        return correlacion.getFuture().whenComplete((confirm, error) -> {
            if (confirm != null && !confirm.isAck()) {
                log.error("NACK del broker para {}: {}", correlacion.getId(), confirm.getReason());
            }
        });
    }
}
