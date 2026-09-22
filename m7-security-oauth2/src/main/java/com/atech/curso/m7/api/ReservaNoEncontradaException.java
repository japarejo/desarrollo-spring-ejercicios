package com.atech.curso.m7.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(long id) {
        super("Reserva " + id + " no encontrada");
    }
}
