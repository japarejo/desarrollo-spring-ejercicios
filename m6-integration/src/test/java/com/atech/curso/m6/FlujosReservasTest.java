package com.atech.curso.m6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;
import com.atech.curso.m6.dominio.ResumenSolicitud;
import com.atech.curso.m6.dominio.SolicitudReserva;
import com.atech.curso.m6.flujos.FlujosReservas;
import com.atech.curso.m6.flujos.ReservasGateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * EJ 6.5 - Pruebas del flujo con MockIntegrationContext: se sustituyen los extremos externos
 * (HTTP y RabbitMQ) por manejadores simulados. El poller JDBC no arranca solo (noAutoStartup).
 */
@SpringBootTest
@SpringIntegrationTest(noAutoStartup = FlujosReservas.ENDPOINT_JDBC)
class FlujosReservasTest {

    @Autowired
    MockIntegrationContext mockIntegration;

    @Autowired
    ReservasGateway gateway;

    @Autowired
    QueueChannel descartadas;

    @Autowired
    SourcePollingChannelAdapter lineasPendientes;

    ArgumentCaptor<Message<?>> publicados;

    @BeforeEach
    void simularExtremos() {
        // Servicio de tarifas: responde 7 EUR/hora
        MessageHandler tarifas = MockIntegration.mockMessageHandler()
                .handleNextAndReply(m -> tarificar((LineaReserva) m.getPayload()))
                .handleNextAndReply(m -> tarificar((LineaReserva) m.getPayload()));
        mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_TARIFA, tarifas);

        // RabbitMQ: capturamos lo que se publicaría
        publicados = MockIntegration.messageArgumentCaptor();
        MessageHandler amqp = MockIntegration.mockMessageHandler(publicados).handleNext(m -> { });
        mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_AMQP, amqp);
    }

    @AfterEach
    void restaurar() {
        lineasPendientes.stop();
        mockIntegration.resetBeans();
    }

    private static LineaTarificada tarificar(LineaReserva linea) {
        return new LineaTarificada(linea, new BigDecimal("7").multiply(BigDecimal.valueOf(linea.horas())), "mock");
    }

    private static LineaReserva linea(String solicitud, String sala, int horas) {
        return new LineaReserva(solicitud, sala, LocalDate.of(2030, 3, 1), horas);
    }

    @Test
    void divideEnrutaTarificaYAgrega() {
        gateway.enviar(new SolicitudReserva("S-1", "ana@atech.es",
                List.of(linea("S-1", "Turing", 2), linea("S-1", "Hopper", 6))));

        ResumenSolicitud resumen = (ResumenSolicitud) publicados.getValue().getPayload();
        assertThat(resumen.solicitudId()).isEqualTo("S-1");
        assertThat(resumen.lineas()).isEqualTo(2);
        // 2 h estándar (10 EUR/h) + 6 h servicio externo simulado (7 EUR/h)
        assertThat(resumen.total()).isEqualByComparingTo("62");
        assertThat(resumen.detalle()).extracting(LineaTarificada::origenTarifa)
                .containsExactlyInAnyOrder("estandar", "mock");
    }

    @Test
    void elFiltroDescartaSolicitudesVacias() {
        gateway.enviar(new SolicitudReserva("S-VACIA", "ana@atech.es", List.of()));

        Message<?> descartada = descartadas.receive(1000);
        assertThat(descartada).isNotNull();
        assertThat(((SolicitudReserva) descartada.getPayload()).id()).isEqualTo("S-VACIA");
    }

    @Test
    void elPollerJdbcProcesaLasLineasPendientes() {
        lineasPendientes.start();

        // data.sql tiene dos solicitudes pendientes: S-100 (2 líneas) y S-101 (1 línea)
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(publicados.getAllValues()).hasSize(2));
        assertThat(publicados.getAllValues())
                .extracting(m -> ((ResumenSolicitud) m.getPayload()).solicitudId())
                .containsExactlyInAnyOrder("S-100", "S-101");
    }
}
