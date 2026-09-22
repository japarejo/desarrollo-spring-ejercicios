package com.atech.curso.m2.reservas;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ReservaInvalidaException extends RuntimeException {

    public ReservaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
