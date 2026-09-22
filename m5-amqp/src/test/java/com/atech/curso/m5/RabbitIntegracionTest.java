package com.atech.curso.m5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.atech.curso.m5.config.Topologia;
import com.atech.curso.m5.consumidores.AuditoriaListener;
import com.atech.curso.m5.consumidores.FacturacionListener;
import com.atech.curso.m5.consumidores.NotificacionesListener;
import com.atech.curso.m5.consumidores.RegistroIdempotencia;
import com.atech.curso.m5.eventos.ReservaConfirmada;
import com.atech.curso.m5.productor.PublicadorReservas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * EJ 5.5 - Prueba de integración contra un RabbitMQ real (Testcontainers + @ServiceConnection).
 * La entrega es asíncrona: Awaitility espera a que se cumpla la condición. Sin Docker, se omite.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RabbitIntegracionTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @Autowired
    PublicadorReservas publicador;

    @Autowired
    FacturacionListener facturacion;

    @Autowired
    NotificacionesListener notificaciones;

    @Autowired
    AuditoriaListener auditoria;

    @Autowired
    RegistroIdempotencia idempotencia;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @BeforeEach
    void limpiar() {
        facturacion.limpiar();
        notificaciones.limpiar();
        auditoria.limpiar();
        idempotencia.limpiar();
    }

    private static ReservaConfirmada evento(String id, String importe) {
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 10, 9, 0);
        return new ReservaConfirmada(id, "Turing", "ana@atech.es", inicio, inicio.plusHours(2), new BigDecimal(importe));
    }

    @Test
    void elBrokerConfirmaYLasTresColasRecibenElEvento() throws Exception {
        CorrelationData.Confirm confirm = publicador.publicar(evento("R-100", "30.00")).get(5, TimeUnit.SECONDS);
        assertThat(confirm.isAck()).isTrue();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(facturacion.facturas()).containsExactly("R-100");
            assertThat(notificaciones.recibidas()).containsExactly("reserva.confirmada:R-100");
            assertThat(auditoria.registros()).singleElement().asString().contains("\"reservaId\":\"R-100\"");
        });
    }

    @Test
    void lasCancelacionesNoSeFacturan() {
        publicador.publicar(Topologia.RK_CANCELADA, evento("R-200", "30.00"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(notificaciones.recibidas()).containsExactly("reserva.cancelada:R-200");
            assertThat(auditoria.registros()).hasSize(1);
        });
        assertThat(facturacion.facturas()).isEmpty();
    }

    @Test
    void losDuplicadosNoGeneranDosFacturas() {
        publicador.publicar(evento("R-300", "10"));
        publicador.publicar(evento("R-300", "10"));

        // Esperamos también a las otras colas para no "contaminar" el siguiente test
        await().atMost(Duration.ofSeconds(10)).until(() -> facturacion.intentos() == 2
                && notificaciones.recibidas().size() == 2 && auditoria.registros().size() == 2);
        assertThat(facturacion.facturas()).containsExactly("R-300");
    }

    @Test
    void trasAgotarLosReintentosElMensajeVaALaColaDeErrores() {
        publicador.publicar(evento("R-400", "-5"));

        Message error = rabbitTemplate.receive(Topologia.COLA_ERRORES, 15_000);

        assertThat(error).isNotNull();
        assertThat(new String(error.getBody(), StandardCharsets.UTF_8)).contains("R-400");
        // RepublishMessageRecoverer añade la traza completa (incluidas las causas) en una cabecera
        // (llega como LongString si supera 1 KB: toString() devuelve el texto)
        assertThat(String.valueOf(error.getMessageProperties().<Object>getHeader("x-exception-stacktrace")))
                .contains("Importe negativo");
        assertThat(facturacion.intentos()).isEqualTo(3);
    }
}
