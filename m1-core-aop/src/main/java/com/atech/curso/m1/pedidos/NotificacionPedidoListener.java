package com.atech.curso.m1.pedidos;

import com.atech.curso.m1.notificacion.Notificador;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** EJ 1.3 - Escucha el evento sin que PedidoService conozca al Notificador (bajo acoplamiento). */
@Component
public class NotificacionPedidoListener {

    private final Notificador notificador;

    public NotificacionPedidoListener(Notificador notificador) {
        this.notificador = notificador;
    }

    @EventListener
    @Order(1)
    public void alConfirmar(PedidoConfirmado evento) {
        notificador.notificar(evento.cliente(), "Su pedido " + evento.pedidoId() + " ha sido confirmado ("
                + evento.importe() + " EUR)");
    }
}
