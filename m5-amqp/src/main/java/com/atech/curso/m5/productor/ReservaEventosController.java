package com.atech.curso.m5.productor;

import java.util.concurrent.TimeUnit;

import com.atech.curso.m5.eventos.ReservaConfirmada;

import jakarta.validation.Valid;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Punto de entrada para probar a mano: POST /api/eventos/reservas-confirmadas */
@RestController
@RequestMapping("/api/eventos")
public class ReservaEventosController {

    private final PublicadorReservas publicador;

    public ReservaEventosController(PublicadorReservas publicador) {
        this.publicador = publicador;
    }

    @PostMapping("/reservas-confirmadas")
    public ResponseEntity<String> publicar(@Valid @RequestBody ReservaConfirmada evento) throws Exception {
        CorrelationData.Confirm confirm = publicador.publicar(evento).get(5, TimeUnit.SECONDS);
        return confirm.isAck()
                ? ResponseEntity.accepted().body("Evento confirmado por el broker")
                : ResponseEntity.internalServerError().body("NACK: " + confirm.getReason());
    }
}
