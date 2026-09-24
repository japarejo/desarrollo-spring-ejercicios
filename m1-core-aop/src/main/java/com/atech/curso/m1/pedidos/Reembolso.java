package com.atech.curso.m1.pedidos;

import java.math.BigDecimal;
import java.time.Instant;

/** EJ 1.7 - Resultado de un reembolso; la pista de auditoría la deja el aspecto, no este record. */
public record Reembolso(String pedidoId, BigDecimal importe, Instant momento) {
}
