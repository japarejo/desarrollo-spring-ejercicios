package com.atech.curso.m6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.FileSystemUtils;

/**
 * EJ 6.5 - Pruebas del flujo con MockIntegrationContext: se sustituyen los extremos externos
 * (HTTP y RabbitMQ) por manejadores simulados. Los ficheros sí son reales, en target/test-buzon.
 * Los dos pollers (JDBC y ficheros) no arrancan solos (noAutoStartup): los arranca el test que los necesita.
 */
@SpringBootTest(properties = {
        "atech.ficheros.entrada=" + FlujosReservasTest.ENTRADA,
        "atech.ficheros.salida=" + FlujosReservasTest.SALIDA })
@SpringIntegrationTest(noAutoStartup = { FlujosReservas.ENDPOINT_JDBC, FlujosReservas.ENDPOINT_FICHEROS })
class FlujosReservasTest {

    static final String ENTRADA = "target/test-buzon/entrada";
    static final String SALIDA = "target/test-buzon/salida";

    @Autowired
    MockIntegrationContext mockIntegration;

    @Autowired
    ReservasGateway gateway;

    @Autowired
    QueueChannel descartadas;

    @Autowired
    @Qualifier(FlujosReservas.ENDPOINT_JDBC)
    SourcePollingChannelAdapter lineasPendientes;

    @Autowired
    @Qualifier(FlujosReservas.ENDPOINT_FICHEROS)
    SourcePollingChannelAdapter buzonEntrada;

    ArgumentCaptor<Message<?>> publicados;

    @BeforeEach
    void simularExtremos() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("target/test-buzon"));
        Files.createDirectories(Path.of(ENTRADA));

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
        buzonEntrada.stop();
        mockIntegration.resetBeans();
    }

    private static LineaTarificada tarificar(LineaReserva linea) {
        return new LineaTarificada(linea, new BigDecimal("7").multiply(BigDecimal.valueOf(linea.horas())), "mock");
    }

    private static LineaReserva linea(String sala, int horas) {
        return new LineaReserva(null, sala, LocalDate.of(2030, 3, 1), horas);
    }

    private List<ResumenSolicitud> resumenesPublicados() {
        return publicados.getAllValues().stream().map(m -> (ResumenSolicitud) m.getPayload()).toList();
    }

    @Test
    void divideEnrutaTarificaYAgrega() throws IOException {
        gateway.enviar(new SolicitudReserva("S-1", List.of(linea("Turing", 2), linea("Hopper", 6))));

        ResumenSolicitud resumen = (ResumenSolicitud) publicados.getValue().getPayload();
        assertThat(resumen.solicitudId()).isEqualTo("S-1");
        assertThat(resumen.origen()).isEqualTo("web");
        assertThat(resumen.lineas()).isEqualTo(2);
        // 2 h estándar (10 EUR/h) + 6 h servicio externo simulado (7 EUR/h)
        assertThat(resumen.total()).isEqualByComparingTo("62");
        assertThat(resumen.detalle()).extracting(LineaTarificada::origenTarifa)
                .containsExactlyInAnyOrder("estandar", "mock");

        // El canal publicar-suscribir también lo ha entregado al escritor de justificantes
        assertThat(Files.readString(Path.of(SALIDA, "S-1.txt"))).contains("solicitud S-1", "TOTAL", "62,00");
    }

    @Test
    void elFiltroDescartaSolicitudesVacias() {
        gateway.enviar(new SolicitudReserva("S-VACIA", List.of()));

        Message<?> descartada = descartadas.receive(1000);
        assertThat(descartada).isNotNull();
        assertThat(((SolicitudReserva) descartada.getPayload()).id()).isEqualTo("S-VACIA");
        assertThat(publicados.getAllValues()).isEmpty();
    }

    @Test
    void elPollerJdbcProcesaLasLineasPendientes() {
        lineasPendientes.start();

        // data.sql tiene dos solicitudes pendientes: S-100 (2 líneas) y S-101 (1 línea)
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(publicados.getAllValues()).hasSize(2));
        assertThat(resumenesPublicados()).extracting(ResumenSolicitud::solicitudId)
                .containsExactlyInAnyOrder("S-100", "S-101");
        assertThat(resumenesPublicados()).extracting(ResumenSolicitud::origen)
                .containsOnly("base-de-datos");
    }

    @Test
    void elBuzonDeEntradaLeeElCsvYDejaLosJustificantes() throws IOException {
        Path csv = Path.of(ENTRADA, "solicitudes.csv");
        Files.copy(Path.of("ejemplos/solicitudes.csv"), csv);

        buzonEntrada.start();

        // ejemplos/solicitudes.csv: S-200 (2 líneas) y S-201 (1 línea)
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(publicados.getAllValues()).hasSize(2));
        assertThat(resumenesPublicados()).extracting(ResumenSolicitud::solicitudId)
                .containsExactlyInAnyOrder("S-200", "S-201");
        assertThat(resumenesPublicados()).extracting(ResumenSolicitud::origen).containsOnly("fichero");

        assertThat(csv).doesNotExist();                      // leído y borrado: no se procesará dos veces
        assertThat(Path.of(SALIDA, "S-200.txt")).exists();
        assertThat(Path.of(SALIDA, "S-201.txt")).exists();
    }
}
