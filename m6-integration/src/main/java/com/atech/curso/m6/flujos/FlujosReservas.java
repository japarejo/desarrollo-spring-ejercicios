package com.atech.curso.m6.flujos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;
import com.atech.curso.m6.dominio.ResumenSolicitud;
import com.atech.curso.m6.dominio.SolicitudReserva;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EJ 6.1 a 6.4 - Flujos con el DSL Java de Spring Integration.
 *
 * <pre>
 *  ReservasGateway ─┐
 *                   ├─► solicitudes ─► filter ─► split(lineas) ─► lineas ─► route(horas &gt; 4)
 *  JDBC (poller) ───┘        │ (descartadas)                                 ├─ false ─► lineasCortas ─► tarifa estándar ─┐
 *                                                                            └─ true  ─► lineasLargas ─► HTTP tarifas ──────┤
 *                                                                                                                        tarificadas
 *                                           RabbitMQ ◄─ Amqp.outboundAdapter ◄─ ResumenSolicitud ◄─ aggregate ◄──────────┘
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
public class FlujosReservas {

    public static final String ENDPOINT_JDBC = "lineasPendientes";
    public static final String ENDPOINT_TARIFA = "consultaTarifa";
    public static final String ENDPOINT_AMQP = "amqpSalida";

    public static final String EXCHANGE = "reservas.exchange";
    public static final String RK_PROCESADA = "solicitud.procesada";

    /** Solicitudes rechazadas por el filtro (QueueChannel: se pueden consultar con receive()). */
    @Bean
    QueueChannel descartadas() {
        return new QueueChannel();
    }

    /** EJ 6.1/6.2 - Entrada: filtro + splitter. */
    @Bean
    IntegrationFlow entradaSolicitudes() {
        return IntegrationFlow.from("solicitudes")
                .filter(SolicitudReserva.class, s -> s.lineas() != null && !s.lineas().isEmpty(),
                        f -> f.discardChannel("descartadas"))
                .split(SolicitudReserva.class, SolicitudReserva::lineas)
                .channel("lineas")
                .get();
    }

    /** EJ 6.1 - Adaptador JDBC de sondeo: lee las líneas pendientes y las marca como procesadas. */
    @Bean
    JdbcPollingChannelAdapter lineasPendientesSource(DataSource dataSource) {
        JdbcPollingChannelAdapter adaptador = new JdbcPollingChannelAdapter(dataSource,
                "SELECT * FROM linea_pendiente WHERE estado = 'PENDIENTE' ORDER BY id");
        adaptador.setUpdateSql("UPDATE linea_pendiente SET estado = 'PROCESADA' WHERE id IN (:id)");
        return adaptador;
    }

    @Bean
    IntegrationFlow lecturaPendientes(JdbcPollingChannelAdapter lineasPendientesSource) {
        return IntegrationFlow.from(lineasPendientesSource,
                        c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(10))).id(ENDPOINT_JDBC))
                .transform(List.class, FlujosReservas::agruparPorSolicitud)
                .split()
                .channel("solicitudes")
                .get();
    }

    /** EJ 6.2 - Router basado en contenido. */
    @Bean
    IntegrationFlow enrutadoLineas() {
        return IntegrationFlow.from("lineas")
                .route(LineaReserva.class, linea -> linea.horas() > 4,
                        r -> r.channelMapping(true, "lineasLargas").channelMapping(false, "lineasCortas"))
                .get();
    }

    @Bean
    IntegrationFlow tarifaEstandar() {
        return IntegrationFlow.from("lineasCortas")
                .transform(LineaReserva.class, LineaTarificada::estandar)
                .channel("tarificadas")
                .get();
    }

    /** EJ 6.3/6.4 - Pasarela HTTP de salida con reintentos (advice) ante fallos del servicio remoto. */
    @Bean
    IntegrationFlow tarifaExterna(RestTemplateBuilder restTemplateBuilder,
            @Value("${atech.tarifas.url}") String urlTarifas, RequestHandlerRetryAdvice reintentos) {
        return IntegrationFlow.from("lineasLargas")
                .handle(Http.outboundGateway(urlTarifas, restTemplateBuilder.build())
                                .httpMethod(HttpMethod.POST)
                                .expectedResponseType(LineaTarificada.class),
                        e -> e.id(ENDPOINT_TARIFA).advice(reintentos))
                .channel("tarificadas")
                .get();
    }

    /** EJ 6.2/6.3 - Agregador (correlación y liberación por las cabeceras de secuencia del splitter) + AMQP. */
    @Bean
    IntegrationFlow agregacionYPublicacion(RabbitTemplate rabbitTemplate) {
        return IntegrationFlow.from("tarificadas")
                .aggregate()
                .transform(List.class, FlujosReservas::resumir)
                .handle(Amqp.outboundAdapter(rabbitTemplate).exchangeName(EXCHANGE).routingKey(RK_PROCESADA),
                        e -> e.id(ENDPOINT_AMQP))
                .get();
    }

    /** EJ 6.4 - Suscriptor adicional del errorChannel global. */
    @Bean
    IntegrationFlow gestionErrores(ErroresIntegracion errores) {
        return IntegrationFlow.from(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
                .handle((payload, headers) -> {
                    errores.registrar((Throwable) payload);
                    return null;
                })
                .get();
    }

    @Bean
    RequestHandlerRetryAdvice reintentosTarifas() {
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
        advice.setRetryTemplate(RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(200, 2, 2000)
                .build());
        return advice;
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ---------------------------------------------------------------- transformaciones

    /** Agrupa las filas leídas (List&lt;Map&gt;) en solicitudes. */
    static List<SolicitudReserva> agruparPorSolicitud(List<?> filas) {
        Map<String, List<LineaReserva>> porSolicitud = new LinkedHashMap<>();
        for (Object fila : filas) {
            @SuppressWarnings("unchecked")
            LineaReserva linea = LineaReserva.desdeFila((Map<String, Object>) fila);
            porSolicitud.computeIfAbsent(linea.solicitudId(), k -> new ArrayList<>()).add(linea);
        }
        return porSolicitud.entrySet().stream()
                .map(e -> new SolicitudReserva(e.getKey(), "jdbc", e.getValue()))
                .toList();
    }

    static ResumenSolicitud resumir(List<?> lineas) {
        return ResumenSolicitud.de(lineas.stream().map(LineaTarificada.class::cast).toList());
    }
}
