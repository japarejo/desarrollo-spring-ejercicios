package com.atech.curso.xml.reservas;

/** Bean con estado mutable: se declara con ámbito {@code prototype} para obtener una instancia nueva cada vez. */
public class BorradorReserva {

    private String idSala;
    private int horas;

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public int getHoras() {
        return horas;
    }

    public void setHoras(int horas) {
        this.horas = horas;
    }
}
