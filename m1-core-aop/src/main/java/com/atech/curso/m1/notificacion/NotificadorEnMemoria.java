package com.atech.curso.m1.notificacion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base común: guarda los mensajes en memoria y los escribe en el log. */
public abstract class NotificadorEnMemoria implements Notificador {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final List<String> enviados = new CopyOnWriteArrayList<>();

    protected void registrar(String destinatario, String mensaje) {
        String linea = "[" + canal() + "] " + destinatario + ": " + mensaje;
        enviados.add(linea);
        log.info(linea);
    }

    @Override
    public List<String> enviados() {
        return List.copyOf(enviados);
    }

    @Override
    public void limpiar() {
        enviados.clear();
    }
}
