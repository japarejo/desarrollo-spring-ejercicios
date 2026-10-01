package com.atech.curso.m5.productor;

import java.util.concurrent.TimeUnit;

import com.atech.curso.m5.eventos.ReservaConfirmada;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada para probar a mano: POST /api/eventos/reservas-confirmadas. Se puede llamar desde
 * Swagger UI (http://localhost:8080/swagger-ui.html), que ofrece los ejemplos del guion ya rellenos.
 */
@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos de reservas", description = "Publicación de eventos en reservas.exchange")
public class ReservaEventosController {

    private static final String EJEMPLO_VALIDO = """
            {"reservaId":"R-1","sala":"Turing","usuario":"ana@atech.es",
             "inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":30}""";

    private static final String EJEMPLO_ERROR = """
            {"reservaId":"R-400","sala":"Turing","usuario":"ana@atech.es",
             "inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":-5}""";

    private final PublicadorReservas publicador;

    public ReservaEventosController(PublicadorReservas publicador) {
        this.publicador = publicador;
    }

    @Operation(summary = "Publica un evento ReservaConfirmada",
            description = "Lo envía a reservas.exchange con la routing key reserva.confirmada y espera la "
                    + "confirmación del broker (publisher confirms). Llega a facturación, notificaciones y auditoría.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(name = "Reserva válida (R-1)",
                                description = "Se factura y se notifica. Publícalo dos veces para ver la idempotencia.",
                                value = EJEMPLO_VALIDO),
                        @ExampleObject(name = "Importe negativo (R-400)",
                                description = "Facturación falla: 3 reintentos y el mensaje acaba en reservas.errores.",
                                value = EJEMPLO_ERROR)
                    })))
    @ApiResponse(responseCode = "202", description = "El broker ha confirmado el mensaje (ack)")
    @ApiResponse(responseCode = "400", description = "Evento no válido: falta algún campo obligatorio")
    @ApiResponse(responseCode = "500", description = "El broker ha rechazado el mensaje (nack)")
    @PostMapping("/reservas-confirmadas")
    public ResponseEntity<String> publicar(@Valid @RequestBody ReservaConfirmada evento) throws Exception {
        CorrelationData.Confirm confirm = publicador.publicar(evento).get(5, TimeUnit.SECONDS);
        return confirm.isAck()
                ? ResponseEntity.accepted().body("Evento confirmado por el broker")
                : ResponseEntity.internalServerError().body("NACK: " + confirm.getReason());
    }
}
