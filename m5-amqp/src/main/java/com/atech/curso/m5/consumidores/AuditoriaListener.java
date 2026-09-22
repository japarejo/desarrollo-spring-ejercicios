package com.atech.curso.m5.consumidores;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.atech.curso.m5.config.Topologia;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** EJ 5.2 - Auditoría: recibe todo (reserva.#) como Message "en bruto", sin convertir. */
@Component
public class AuditoriaListener {

    private final List<String> registros = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = Topologia.COLA_AUDITORIA)
    public void auditar(Message mensaje) {
        registros.add(mensaje.getMessageProperties().getReceivedRoutingKey() + " "
                + new String(mensaje.getBody(), StandardCharsets.UTF_8));
    }

    public List<String> registros() {
        return List.copyOf(registros);
    }

    public void limpiar() {
        registros.clear();
    }
}
