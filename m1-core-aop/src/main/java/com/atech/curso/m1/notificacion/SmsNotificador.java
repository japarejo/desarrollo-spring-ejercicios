package com.atech.curso.m1.notificacion;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** EJ 1.1 - Implementación alternativa, sólo con el perfil "sms". */
@Component
@Profile("sms")
public class SmsNotificador extends NotificadorEnMemoria {

    @Override
    public String canal() {
        return "sms";
    }

    @Override
    public void notificar(String destinatario, String mensaje) {
        // Los SMS tienen longitud limitada
        String texto = mensaje.length() > 160 ? mensaje.substring(0, 157) + "..." : mensaje;
        registrar(destinatario, texto);
    }
}
