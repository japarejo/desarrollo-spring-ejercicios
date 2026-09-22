package com.atech.curso.m1.pedidos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** EJ 1.3 (ampliación) - Listener condicional con SpEL: sólo pedidos de más de 1000 EUR. */
@Component
public class GrandesPedidosListener {

    private final List<String> revisiones = new CopyOnWriteArrayList<>();

    @EventListener(condition = "#evento.importe() > 1000")
    @Order(2)
    public void revisar(PedidoConfirmado evento) {
        revisiones.add(evento.pedidoId());
    }

    public List<String> revisiones() {
        return List.copyOf(revisiones);
    }

    public void limpiar() {
        revisiones.clear();
    }
}
