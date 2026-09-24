package com.atech.curso.m1.pista;

import org.springframework.stereotype.Component;

/** EJ 1.7 - Sustituto del contexto de seguridad para poder probar el aspecto sin Spring Security. */
@Component
public class UsuarioActualEnMemoria implements UsuarioActual {

    private volatile String nombre = "anonimo";

    @Override
    public String nombre() {
        return nombre;
    }

    public void autenticar(String nombre) {
        this.nombre = nombre;
    }
}
