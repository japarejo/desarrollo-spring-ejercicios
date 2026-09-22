package com.atech.curso.m7.api;

import java.time.LocalDateTime;

public record Reserva(long id, String sala, String propietario, LocalDateTime inicio) {
}
