package com.atech.curso.m1.pedidos;

import java.math.BigDecimal;
import java.util.Objects;

public record Pedido(String id, String cliente, BigDecimal importe) {

    public Pedido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cliente, "cliente");
        Objects.requireNonNull(importe, "importe");
    }
}
