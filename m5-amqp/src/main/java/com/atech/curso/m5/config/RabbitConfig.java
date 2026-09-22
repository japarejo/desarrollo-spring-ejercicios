package com.atech.curso.m5.config;

import static com.atech.curso.m5.config.Topologia.COLA_AUDITORIA;
import static com.atech.curso.m5.config.Topologia.COLA_ERRORES;
import static com.atech.curso.m5.config.Topologia.COLA_FACTURACION;
import static com.atech.curso.m5.config.Topologia.COLA_NOTIFICACIONES;
import static com.atech.curso.m5.config.Topologia.DLX;
import static com.atech.curso.m5.config.Topologia.EXCHANGE;
import static com.atech.curso.m5.config.Topologia.RK_CONFIRMADA;
import static com.atech.curso.m5.config.Topologia.RK_ERROR;
import static com.atech.curso.m5.config.Topologia.dlq;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EJ 5.1 - Topología declarativa. RabbitAdmin (autoconfigurado) la declara en el broker
 * al abrir la primera conexión.
 *
 * <pre>
 *                      reserva.confirmada   -> reservas.facturacion    (DLQ reservas.facturacion.dlq)
 * reservas.exchange -- reserva.*            -> reservas.notificaciones (DLQ ...)
 *    (topic)           reserva.#            -> reservas.auditoria      (DLQ ...)
 * reservas.dlx (direct) -- error            -> reservas.errores
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
public class RabbitConfig {

    @Bean
    Declarables topologiaReservas() {
        List<Declarable> d = new ArrayList<>();
        TopicExchange exchange = ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
        DirectExchange dlx = ExchangeBuilder.directExchange(DLX).durable(true).build();
        d.add(exchange);
        d.add(dlx);

        d.addAll(colaConDlq(COLA_FACTURACION, exchange, RK_CONFIRMADA, dlx));
        d.addAll(colaConDlq(COLA_NOTIFICACIONES, exchange, "reserva.*", dlx));
        d.addAll(colaConDlq(COLA_AUDITORIA, exchange, "reserva.#", dlx));

        Queue errores = QueueBuilder.durable(COLA_ERRORES).build();
        d.add(errores);
        d.add(BindingBuilder.bind(errores).to(dlx).with(RK_ERROR));
        return new Declarables(d);
    }

    /** Cola principal con dead-letter hacia el DLX y su DLQ correspondiente. */
    static List<Declarable> colaConDlq(String nombre, TopicExchange exchange, String patron, DirectExchange dlx) {
        Queue cola = QueueBuilder.durable(nombre)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(dlq(nombre))
                .build();
        Queue colaDlq = QueueBuilder.durable(dlq(nombre)).build();
        Binding principal = BindingBuilder.bind(cola).to(exchange).with(patron);
        Binding muerta = BindingBuilder.bind(colaDlq).to(dlx).with(dlq(nombre));
        return List.of(cola, colaDlq, principal, muerta);
    }

    /** EJ 5.2 - JSON con el ObjectMapper de Boot (soporte java.time). Boot lo aplica a plantilla y listeners. */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * EJ 5.3 - Cuando se agotan los reintentos (spring.rabbitmq.listener.simple.retry.*), el mensaje se
     * republica en reservas.errores con la traza de la excepción en cabeceras (x-exception-*).
     * Sin este bean Boot usa RejectAndDontRequeueRecoverer y el mensaje iría a la DLQ de su cola.
     */
    @Bean
    MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX, RK_ERROR);
    }
}
