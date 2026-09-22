package com.atech.curso.m1.pedidos;

import java.math.BigDecimal;
import java.time.Instant;

/** EJ 1.3 - Evento de aplicación: desde Spring 4.2 cualquier objeto puede publicarse como evento. */
public record PedidoConfirmado(String pedidoId, String cliente, BigDecimal importe, Instant instante) {
}
