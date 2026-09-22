package com.atech.curso.m6.flujos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** EJ 6.4 - Registro de los errores que llegan al errorChannel (flujos asíncronos/pollers). */
@Component
public class ErroresIntegracion {

    private static final Logger log = LoggerFactory.getLogger(ErroresIntegracion.class);

    private final List<String> errores = new CopyOnWriteArrayList<>();

    public void registrar(Throwable error) {
        Throwable raiz = error;
        while (raiz.getCause() != null) {
            raiz = raiz.getCause();
        }
        errores.add(raiz.getMessage());
        log.error("Error en un flujo de integración: {}", raiz.getMessage());
    }

    public List<String> errores() {
        return List.copyOf(errores);
    }
}
