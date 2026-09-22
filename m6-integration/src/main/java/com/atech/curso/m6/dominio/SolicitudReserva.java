package com.atech.curso.m6.dominio;

import java.util.List;

/** Mensaje de entrada: una solicitud con varias líneas (se divide con un splitter). */
public record SolicitudReserva(String id, String usuario, List<LineaReserva> lineas) {
}
