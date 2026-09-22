package com.atech.curso.m5.consumidores;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.atech.curso.m5.config.Topologia;
import com.atech.curso.m5.eventos.ReservaConfirmada;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** EJ 5.2 - Recibe confirmaciones y cancelaciones (patrón reserva.*). */
@Component
public class NotificacionesListener {

    private final List<String> recibidas = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = Topologia.COLA_NOTIFICACIONES)
    public void notificar(ReservaConfirmada evento, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        recibidas.add(routingKey + ":" + evento.reservaId());
    }

    public List<String> recibidas() {
        return List.copyOf(recibidas);
    }

    public void limpiar() {
        recibidas.clear();
    }
}
