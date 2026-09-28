package com.atech.curso.m3.gobierno;

public class CasoplonException extends EscandaloException {

    public CasoplonException(String titular, String organo) {
        super(titular, organo, "¡Casoplón! " + titular + " (" + organo + ") se ha comprado una guarida de 600 m²"
                + " con piscina en forma de su propio logotipo");
    }
}
