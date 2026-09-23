# Módulo 5 · Mensajería con RabbitMQ

**Objetivo:** publicar eventos de reservas en RabbitMQ y consumirlos de forma fiable: topología con *dead-lettering*, JSON, idempotencia, reintentos y confirmaciones del broker.

```bash
docker compose -f m5-amqp/compose.yaml up -d          # RabbitMQ 4.1 + consola en :15672
./mvnw -pl m5-amqp spring-boot:run
curl -X POST localhost:8080/api/eventos/reservas-confirmadas -H 'Content-Type: application/json' \
  -d '{"reservaId":"R-1","sala":"Turing","usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":30}'
```

## Topología

```
reservas.exchange (topic)
 ├─ reserva.confirmada ─► reservas.facturacion    ─(DLX)─► reservas.facturacion.dlq
 ├─ reserva.*          ─► reservas.notificaciones ─(DLX)─► reservas.notificaciones.dlq
 └─ reserva.#          ─► reservas.auditoria      ─(DLX)─► reservas.auditoria.dlq
reservas.dlx (direct)
 └─ error              ─► reservas.errores   (mensajes que agotan los reintentos, con la traza)
```

## Enunciados

### EJ 5.1 · Topología declarativa
Declara el exchange *topic*, las tres colas con `x-dead-letter-exchange`/`x-dead-letter-routing-key`, sus DLQ y los enlaces, todo en un único bean `Declarables`. Comprueba en la consola de RabbitMQ que se crean al arrancar.
*Test:* `TopologiaTest`, que no necesita broker.

### EJ 5.2 · Productor y consumidores
1. El evento `ReservaConfirmada` es un **record** que se serializa como JSON (`Jackson2JsonMessageConverter` con el `ObjectMapper` de Boot).
2. `PublicadorReservas` fija `messageId` = id de la reserva, que se usará como clave de idempotencia.
3. Consumidores:
   - `FacturacionListener`: **idempotente**, ignora los duplicados.
   - `NotificacionesListener`: lee la *routing key* recibida.
   - `AuditoriaListener`: recibe el `Message` sin convertir. Como no declara el tipo en la firma,
     el conversor no puede **inferirlo** y lo resuelve por la cabecera `__TypeId__`; por eso el
     `Jackson2JsonMessageConverter` declara los *trusted packages* (`com.atech.curso.m5.eventos`).
     Sin ellos solo se confía en `java.util`/`java.lang`, la conversión falla y el mensaje acaba
     en `reservas.errores` tras agotar los reintentos.

### EJ 5.3 · Errores y reintentos
1. Configura reintentos locales con *backoff* exponencial: `spring.rabbitmq.listener.simple.retry.*`.
2. Con un `MessageRecoverer` de tipo `RepublishMessageRecoverer`, los mensajes que agotan los reintentos se republican en `reservas.errores` con cabeceras `x-exception-*`.
3. *Para pensar:* ¿cuándo preferirías `RejectAndDontRequeueRecoverer`, que envía el mensaje a la DLQ de su cola? ¿Qué pasa si el error no es transitorio?

### EJ 5.4 · Publisher confirms y returns
Activa `publisher-confirm-type: correlated`, `publisher-returns` y `template.mandatory`. El publicador devuelve el `CompletableFuture<Confirm>` de `CorrelationData`, y el controlador espera el *ack* antes de responder `202`.

### EJ 5.5 · Pruebas de integración
`RabbitIntegracionTest` levanta RabbitMQ con Testcontainers + `@ServiceConnection` y usa **Awaitility** para las aserciones asíncronas. Cubre la difusión a tres colas, las cancelaciones sin factura, los duplicados y los errores que llegan a la cola de errores. Sin Docker, el test se omite.

## Extra Spring Boot 4 / RabbitMQ 4
- Spring AMQP 4 incluye el cliente **AMQP 1.0** (`spring-rabbitmq-client`) y `RabbitAmqpTemplate`:
  ```java
  CompletableFuture<Boolean> ok = rabbitAmqpTemplate.convertAndSend("reservas.exchange", "reserva.confirmada", evento);
  ```
- Prueba las **quorum queues** (`QueueBuilder.durable(..).quorum()`) con `deliveryLimit` en lugar de reintentos locales.
- Jackson 3: `JacksonJsonMessageConverter` sustituye a `Jackson2JsonMessageConverter`.
