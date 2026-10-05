package com.atech.curso.m6.flujos;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.atech.curso.m6.dominio.LineaReserva;
import com.atech.curso.m6.dominio.LineaTarificada;
import com.atech.curso.m6.dominio.ResumenSolicitud;
import com.atech.curso.m6.dominio.SolicitudReserva;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EJ 6.1 a 6.4 - Flujos con el DSL Java de Spring Integration. Tres entradas, un procesamiento común y
 * dos salidas (el diagrama completo está en el README):
 *
 * <pre>
 *  web (gateway) ───────┐
 *  BD (poller JDBC) ────┼─► solicitudes ─► filter ─► split ─► route ─┬─ cortas ─► tarifa estándar ─┐
 *  CSV (poller fichero) ┘                                            └─ largas ─► HTTP /tarifas ───┤
 *                                                                                                  ▼
 *                          RabbitMQ ◄─┬─ resumenes ◄─ ResumenSolicitud ◄─ aggregate ◄─ tarificadas
 *              buzon/salida/S-x.txt ◄─┘
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
public class FlujosReservas {

    public static final String ENDPOINT_JDBC = "lineasPendientes";
    public static final String ENDPOINT_FICHEROS = "buzonEntrada";
    public static final String ENDPOINT_TARIFA = "consultaTarifa";
    public static final String ENDPOINT_AMQP = "amqpSalida";

    /** Cabecera que pone cada entrada para saber, al final del flujo, por dónde llegó la solicitud. */
    public static final String CABECERA_ORIGEN = "origen";

    public static final String EXCHANGE = "reservas.exchange";
    public static final String RK_PROCESADA = "solicitud.procesada";
    public static final String COLA_PROCESADAS = "solicitudes.procesadas";

    // ================================================================ ① ENTRADAS (EJ 6.1)
    // La web entra por ReservasGateway (canal "solicitudes", origen "web").

    /** Base de datos: lee las líneas pendientes y las marca como procesadas en la misma transacción. */
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
                .enrichHeaders(h -> h.header(CABECERA_ORIGEN, "base-de-datos"))
                .transform(List.class, FlujosReservas::filasASolicitudes)
                .split()
                .channel("solicitudes")
                .get();
    }

    /** Fichero: cada CSV que aparece en el buzón de entrada se lee, se borra y se convierte en solicitudes. */
    @Bean
    IntegrationFlow lecturaFicheros(@Value("${atech.ficheros.entrada}") File buzonEntrada) {
        return IntegrationFlow.from(Files.inboundAdapter(buzonEntrada).patternFilter("*.csv"),
                        c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(2))).id(ENDPOINT_FICHEROS))
                .enrichHeaders(h -> h.header(CABECERA_ORIGEN, "fichero"))
                .transform(Files.toStringTransformer("UTF-8", true))
                .transform(String.class, FlujosReservas::csvASolicitudes)
                .split()
                .channel("solicitudes")
                .get();
    }

    // ================================================================ ② PROCESAMIENTO (EJ 6.2)

    /** Solicitudes rechazadas por el filtro (QueueChannel: esperan a que alguien las recoja con receive()). */
    @Bean
    QueueChannel descartadas() {
        return new QueueChannel();
    }

    /** Filtro + splitter: una solicitud con N líneas sale como N mensajes. */
    @Bean
    IntegrationFlow entradaSolicitudes() {
        return IntegrationFlow.from("solicitudes")
                .filter(SolicitudReserva.class, s -> !s.lineas().isEmpty(),
                        f -> f.discardChannel("descartadas"))
                .split(SolicitudReserva.class, SolicitudReserva::lineas)
                .channel("lineas")
                .get();
    }

    /** Router basado en contenido: las reservas de jornada (más de 4 h) tienen tarifa propia. */
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

    /** Agregador (correlación y liberación por las cabeceras de secuencia del splitter) + resumen. */
    @Bean
    IntegrationFlow agregacion() {
        return IntegrationFlow.from("tarificadas")
                .aggregate()
                .handle(List.class, (lineas, cabeceras) -> resumir(lineas, (String) cabeceras.get(CABECERA_ORIGEN)))
                .channel("resumenes")
                .get();
    }

    // ================================================================ ③ SALIDAS (EJ 6.3)

    /** Canal publicar-suscribir: cada resumen llega a TODOS los suscriptores (RabbitMQ y fichero). */
    @Bean
    PublishSubscribeChannel resumenes() {
        return new PublishSubscribeChannel();
    }

    /** Para los demás sistemas: RabbitMQ. */
    @Bean
    IntegrationFlow publicacionAmqp(RabbitTemplate rabbitTemplate) {
        return IntegrationFlow.from("resumenes")
                .handle(Amqp.outboundAdapter(rabbitTemplate).exchangeName(EXCHANGE).routingKey(RK_PROCESADA),
                        e -> e.id(ENDPOINT_AMQP))
                .get();
    }

    /** Topología mínima para ver los resúmenes en la consola de RabbitMQ (se declara al conectar). */
    @Bean
    TopicExchange reservasExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue solicitudesProcesadas() {
        return new Queue(COLA_PROCESADAS);
    }

    @Bean
    Binding procesadasBinding(Queue solicitudesProcesadas, TopicExchange reservasExchange) {
        return BindingBuilder.bind(solicitudesProcesadas).to(reservasExchange).with(RK_PROCESADA);
    }

    /** Para administración: un justificante de texto por solicitud en el buzón de salida. */
    @Bean
    IntegrationFlow escrituraJustificantes(@Value("${atech.ficheros.salida}") File buzonSalida) {
        return IntegrationFlow.from("resumenes")
                .enrichHeaders(h -> h.headerFunction(FileHeaders.FILENAME,
                        m -> ((ResumenSolicitud) m.getPayload()).solicitudId() + ".txt", true))
                .transform(ResumenSolicitud.class, ResumenSolicitud::justificante)
                .handle(Files.outboundAdapter(buzonSalida).fileExistsMode(FileExistsMode.REPLACE))
                .get();
    }

    // ================================================================ ERRORES (EJ 6.4)

    /** Suscriptor adicional del errorChannel global: recibe los fallos de los flujos que arrancan los pollers. */
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

    /** Filas leídas de la tabla (List&lt;Map&gt;) → solicitudes. */
    static List<SolicitudReserva> filasASolicitudes(List<?> filas) {
        return SolicitudReserva.agrupar(filas.stream()
                .map(fila -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> columnas = (Map<String, Object>) fila;
                    return LineaReserva.desdeFila(columnas);
                })
                .toList());
    }

    /** Contenido del CSV (cabecera {@code solicitud,sala,fecha,horas} y una línea por fila) → solicitudes. */
    static List<SolicitudReserva> csvASolicitudes(String csv) {
        return SolicitudReserva.agrupar(csv.lines()
                .skip(1)
                .filter(linea -> !linea.isBlank())
                .map(LineaReserva::desdeCsv)
                .toList());
    }

    static ResumenSolicitud resumir(List<?> lineas, String origen) {
        return ResumenSolicitud.de(lineas.stream().map(LineaTarificada.class::cast).toList(), origen);
    }
}
