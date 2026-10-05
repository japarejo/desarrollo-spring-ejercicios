package com.atech.curso.m6.web;

import java.util.ArrayList;
import java.util.List;

import com.atech.curso.m6.dominio.SolicitudReserva;
import com.atech.curso.m6.flujos.ReservasGateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EJ 6.1 - El controlador sólo conoce la interfaz de la pasarela. Se prueba desde Swagger UI
 * (http://localhost:8080/swagger-ui.html), que ofrece los ejemplos del guion ya rellenos.
 */
@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes de reserva", description = "La entrada web del flujo de integración")
public class SolicitudController {

    private static final String EJEMPLO_MIXTA = """
            {"id":"S-1","lineas":[
              {"sala":"Turing","fecha":"2030-03-01","horas":2},
              {"sala":"Hopper","fecha":"2030-03-01","horas":6}]}""";

    private static final String EJEMPLO_VACIA = """
            {"id":"S-VACIA","lineas":[]}""";

    private final ReservasGateway gateway;
    private final QueueChannel descartadas;

    public SolicitudController(ReservasGateway gateway, QueueChannel descartadas) {
        this.gateway = gateway;
        this.descartadas = descartadas;
    }

    @Operation(summary = "Envía una solicitud al flujo de integración",
            description = "La pasarela la pone en el canal `solicitudes` con la cabecera `origen=web`. El flujo "
                    + "se ejecuta en este mismo hilo: cuando responde, el resumen ya está en RabbitMQ y el "
                    + "justificante en el buzón de salida.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(name = "Una línea corta y una larga (S-1)",
                                description = "Turing 2 h → tarifa estándar (20 €). Hopper 6 h → servicio de "
                                        + "tarifas (54 €). Total: 74 €.",
                                value = EJEMPLO_MIXTA),
                        @ExampleObject(name = "Solicitud sin líneas (S-VACIA)",
                                description = "El filtro la descarta al canal `descartadas`. Consúltalo después "
                                        + "con GET /api/solicitudes/descartadas.",
                                value = EJEMPLO_VACIA)
                    })))
    @ApiResponse(responseCode = "202", description = "El flujo ha procesado la solicitud (o el filtro la ha descartado)")
    @ApiResponse(responseCode = "500", description = "El flujo ha fallado: la excepción vuelve a quien llamó al gateway")
    @PostMapping
    public ResponseEntity<Void> recibir(@RequestBody SolicitudReserva solicitud) {
        gateway.enviar(solicitud);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Recoge las solicitudes descartadas por el filtro",
            description = "`descartadas` es un QueueChannel: los mensajes esperan en él hasta que alguien los "
                    + "recoge. Cada llamada vacía el canal.")
    @GetMapping("/descartadas")
    public List<SolicitudReserva> descartadas() {
        List<SolicitudReserva> recogidas = new ArrayList<>();
        Message<?> mensaje;
        while ((mensaje = descartadas.receive(0)) != null) {
            recogidas.add((SolicitudReserva) mensaje.getPayload());
        }
        return recogidas;
    }
}
