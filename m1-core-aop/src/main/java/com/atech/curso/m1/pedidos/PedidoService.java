package com.atech.curso.m1.pedidos;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import com.atech.curso.m1.auditoria.Auditado;
import com.atech.curso.m1.precios.CalculadoraDescuento;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * EJ 1.2 - Inyección por constructor: dependencias obligatorias, campos final y clase
 * fácil de probar sin Spring (ver PedidoServiceUnitTest). Con un único constructor no hace falta @Autowired.
 */
@Service
public class PedidoService {

    private final ApplicationEventPublisher eventos;
    private final CalculadoraDescuento descuentos;
    private final Clock clock;

    public PedidoService(ApplicationEventPublisher eventos, CalculadoraDescuento descuentos, Clock clock) {
        this.eventos = eventos;
        this.descuentos = descuentos;
        this.clock = clock;
    }

    /** EJ 1.3 y 1.4 - Publica el evento y queda auditado por el aspecto. */
    @Auditado("confirmar-pedido")
    public PedidoConfirmado confirmar(Pedido pedido) {
        if (pedido.importe().signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo");
        }
        BigDecimal total = descuentos.aplicar(pedido.importe());
        PedidoConfirmado evento = new PedidoConfirmado(pedido.id(), pedido.cliente(), total, clock.instant());
        eventos.publishEvent(evento);
        return evento;
    }

    /**
     * EJ 1.5 - Auto-invocación: {@code this.confirmar(..)} NO pasa por el proxy, así que el
     * aspecto {@code @Auditado} no se aplica a las confirmaciones individuales del lote.
     * Soluciones: mover el método a otro bean, inyectar el propio proxy (ObjectProvider/@Lazy)
     * o usar AspectJ con tejido en compilación/carga.
     */
    public List<PedidoConfirmado> confirmarLote(List<Pedido> pedidos) {
        return pedidos.stream().map(this::confirmar).toList();
    }
}
