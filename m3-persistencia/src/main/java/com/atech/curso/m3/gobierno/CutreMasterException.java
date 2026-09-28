package com.atech.curso.m3.gobierno;

public class CutreMasterException extends EscandaloException {

    public CutreMasterException(String titular, String organo) {
        super(titular, organo, "¡CutreMaster! El máster en Superheroicidad Aplicada de " + titular + " (" + organo
                + ") se aprobó en tres días, sin pisar el aula y con las actas firmadas por su primo");
    }
}
