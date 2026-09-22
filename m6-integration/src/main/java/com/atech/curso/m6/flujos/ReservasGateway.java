package com.atech.curso.m6.flujos;

import com.atech.curso.m6.dominio.SolicitudReserva;

import org.springframework.integration.annotation.MessagingGateway;

/**
 * EJ 6.1 - Pasarela de mensajería: el código de negocio llama a un método Java y Spring
 * Integration crea el mensaje y lo envía al canal "solicitudes". El resto del sistema no
 * conoce la infraestructura de mensajería.
 */
@MessagingGateway(defaultRequestChannel = "solicitudes")
public interface ReservasGateway {

    void enviar(SolicitudReserva solicitud);
}
