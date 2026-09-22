# Módulo 6 · Spring Integration

**Objetivo:** construir con el **DSL Java** un flujo de integración que recibe solicitudes de reserva por dos entradas (REST y base de datos), las procesa aplicando patrones EIP y publica el resultado en RabbitMQ.

```bash
cd m6-integration && docker compose up -d
mvn -pl m6-integration spring-boot:run
curl -X POST localhost:8080/api/solicitudes -H 'Content-Type: application/json' -d '{
  "id":"S-1","usuario":"ana@atech.es","lineas":[
    {"solicitudId":"S-1","sala":"Turing","fecha":"2030-03-01","horas":2},
    {"solicitudId":"S-1","sala":"Hopper","fecha":"2030-03-01","horas":6}]}'
```

## Flujo

```
ReservasGateway ─┐
                 ├─► solicitudes ─► filter ─► split(lineas) ─► lineas ─► route(horas > 4)
JDBC (poller) ───┘      │ descartadas                                     ├─ no ─► tarifa estándar (10 €/h) ─┐
                                                                          └─ sí ─► HTTP /tarifas (reintentos) ┤
                                                                                                         tarificadas
                             RabbitMQ ◄── Amqp.outboundAdapter ◄── ResumenSolicitud ◄── aggregate ◄─────────┘
```

## Enunciados

### EJ 6.1 · Entradas
1. `ReservasGateway` (`@MessagingGateway`) usada desde `SolicitudController`: el controlador no sabe nada de mensajería.
2. `JdbcPollingChannelAdapter` que cada 10 s lee `linea_pendiente` con estado `PENDIENTE`, marca las filas como procesadas (`updateSql`) y agrupa las líneas por solicitud.

### EJ 6.2 · Procesamiento (patrones EIP)
- **Filter**: descarta las solicitudes sin líneas y las envía al canal `descartadas`.
- **Splitter**: divide cada solicitud en sus líneas.
- **Router** por contenido: separa las líneas cortas de las largas (más de 4 horas).
- **Aggregator**: reúne las líneas de cada solicitud usando las cabeceras de secuencia que añade el *splitter* y calcula el total.

### EJ 6.3 · Salidas
- `Http.outboundGateway` hacia el servicio de tarifas (simulado en `TarifasController`).
- `Amqp.outboundAdapter` al exchange `reservas.exchange` con la *routing key* `solicitud.procesada`.

### EJ 6.4 · Robustez
- `RequestHandlerRetryAdvice` con *backoff* exponencial en la llamada HTTP.
- Un suscriptor adicional del `errorChannel` global (`ErroresIntegracion`). *Para pensar:* ¿por qué los errores del *gateway* síncrono no llegan al `errorChannel` y los del *poller* sí?

### EJ 6.5 · Pruebas
`FlujosReservasTest` usa `@SpringIntegrationTest` y `MockIntegrationContext` para sustituir los extremos HTTP y AMQP por *mocks* (`MockIntegration.mockMessageHandler`). El *poller* JDBC no arranca automáticamente (`noAutoStartup`) y el test lo inicia cuando lo necesita.

## Extra Spring Boot 4 / Spring Integration 7
- Compila con `-Xlint:deprecation` para revisar las deprecaciones del DSL, y adapta los *advices* de reintento a la nueva API de reintentos de Spring Framework 7 (`org.springframework.core.retry`).
- Observabilidad: activa `spring.integration.management.observation-patterns=*` y consulta las trazas.
- Genera el grafo del flujo con Actuator (`/actuator/integrationgraph`).
