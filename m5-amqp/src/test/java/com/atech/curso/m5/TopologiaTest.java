package com.atech.curso.m5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.atech.curso.m5.config.RabbitConfig;
import com.atech.curso.m5.config.Topologia;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** EJ 5.1 - Comprobar la topología sin necesidad de broker. */
class TopologiaTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            // RabbitTemplate necesita una ConnectionFactory; basta un mock (no se conecta a nada)
            .withBean(RabbitTemplate.class, () -> new RabbitTemplate(mock(ConnectionFactory.class)))
            .withUserConfiguration(RabbitConfig.class);

    @Test
    void cadaColaTieneSuDeadLetter() {
        runner.run(ctx -> {
            Declarables d = ctx.getBean(Declarables.class);
            List<Queue> colas = d.getDeclarablesByType(Queue.class);

            assertThat(colas).extracting(Queue::getName).contains(
                    Topologia.COLA_FACTURACION, Topologia.dlq(Topologia.COLA_FACTURACION),
                    Topologia.COLA_NOTIFICACIONES, Topologia.COLA_AUDITORIA, Topologia.COLA_ERRORES);

            Queue facturacion = colas.stream().filter(q -> q.getName().equals(Topologia.COLA_FACTURACION))
                    .findFirst().orElseThrow();
            assertThat(facturacion.getArguments())
                    .containsEntry("x-dead-letter-exchange", Topologia.DLX)
                    .containsEntry("x-dead-letter-routing-key", "reservas.facturacion.dlq");
        });
    }

    @Test
    void enlacesDelExchangeTopic() {
        runner.run(ctx -> {
            List<Binding> enlaces = ctx.getBean(Declarables.class).getDeclarablesByType(Binding.class);
            assertThat(enlaces).filteredOn(b -> b.getExchange().equals(Topologia.EXCHANGE))
                    .extracting(Binding::getRoutingKey)
                    .containsExactlyInAnyOrder("reserva.confirmada", "reserva.*", "reserva.#");
        });
    }
}
