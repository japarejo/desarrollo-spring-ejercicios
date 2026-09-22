package com.atech.curso.m6.web;

import com.atech.curso.m6.dominio.SolicitudReserva;
import com.atech.curso.m6.flujos.ReservasGateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EJ 6.1 - El controlador sólo conoce la interfaz de la pasarela. */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final ReservasGateway gateway;

    public SolicitudController(ReservasGateway gateway) {
        this.gateway = gateway;
    }

    @PostMapping
    public ResponseEntity<Void> recibir(@RequestBody SolicitudReserva solicitud) {
        gateway.enviar(solicitud);
        return ResponseEntity.accepted().build();
    }
}
