package com.atech.curso.m3.gobierno;

public class JoyasOcultasException extends EscandaloException {

    public JoyasOcultasException(String titular, String organo) {
        super(titular, organo, "¡Joyas ocultas! Aparecen las joyas de la abuela de " + titular + " (" + organo
                + ") en una cuenta opaca de un paraíso fiscal de Kriptón");
    }
}
