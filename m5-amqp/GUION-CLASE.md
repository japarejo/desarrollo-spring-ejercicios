# Guión de clase · Módulo 5 · Mensajería con RabbitMQ

**Duración:** 3 h netas, **repartidas en dos sesiones** · Enunciados en [README.md](README.md)

- **Final de la sesión 4 (1 h 30):** pasos 1 a 4 — topología, productor y consumidores.
- **Sesión 5 (1 h 30):** pasos 5 a 8 — errores, confirmaciones y pruebas.

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.
>
> **Lo primero de la sesión, antes de saludar:** levanta el broker. Tarda, y sin él no hay demo.
>
> ```bash
> docker compose -f m5-amqp/compose.yaml up -d
> ```
>
> **Y deja la consola de RabbitMQ proyectada todo el módulo** (<http://localhost:15672>, `guest`/`guest`).
> Es el mejor material didáctico del tema: se ve el mensaje entrar, moverse y morir.

---

## Paso 0 · Antes de entrar en el aula (10 min)

```bash
docker pull rabbitmq:4.1-management
docker compose -f m5-amqp/compose.yaml up -d
./mvnw -pl m5-amqp test                 # 4 clases; RabbitIntegracionTest se omite sin Docker
```

Comprueba que entras en <http://localhost:15672> y **ten localizadas estas tres pestañas de la consola**:
*Exchanges*, *Queues and Streams* y, dentro de una cola, el botón **Get messages**. Las vas a usar seis o
siete veces.

Para publicar eventos no hace falta `curl`: con la aplicación arrancada, **deja abierta
<http://localhost:8080/swagger-ui.html>** en otra pestaña. La operación `POST /api/eventos/reservas-confirmadas`
trae ya los dos ejemplos del guion en el desplegable *Examples*: **Reserva válida (R-1)** e **Importe negativo
(R-400)**. Si el puerto 8080 está ocupado (por ejemplo, por la aplicación del módulo 4), arranca con
`./mvnw -pl m5-amqp spring-boot:run "-Dspring-boot.run.arguments=--server.port=8085"` y usa ese puerto.

Pestañas del IDE:

1. [`src/main/java/com/atech/curso/m5/config/RabbitConfig.java`](src/main/java/com/atech/curso/m5/config/RabbitConfig.java)
2. [`src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java`](src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java)
3. [`src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java`](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java)
4. [`src/main/resources/application.properties`](src/main/resources/application.properties)

---

## Cronograma

| Paso | Contenido | Min | Sesión |
|---|---|---|---|
| 1 | Gancho: del evento en memoria al broker | 15 | 4 |
| 2 | Concepto: AMQP y la topología en la pizarra | 20 | 4 |
| 3 | Ejercicio EJ 5.1 (topología declarativa) | 30 | 4 |
| 4 | Productor y consumidores (EJ 5.2) | 25 | 4 |
| 5 | 🔴 Errores y reintentos (EJ 5.3) | 45 | 5 |
| 6 | Publisher confirms y returns (EJ 5.4) | 20 | 5 |
| 7 | Pruebas de integración (EJ 5.5) | 15 | 5 |
| 8 | Cierre del módulo | 10 | 5 |

---

## Paso 1 · Gancho: del evento en memoria al broker · 15 min

1. **🗣️ Empieza recordando el módulo 1** (no des por hecho que lo tienen fresco): «El primer día
   publicamos un `PedidoConfirmado` con `ApplicationEventPublisher` y lo recogió un listener. ¿Dónde
   viajaba ese evento?» → En memoria, dentro de la misma JVM, de forma síncrona.

2. **❓ Haz las tres preguntas que rompen ese modelo**, una a una, dejando responder:
   - «¿Qué pasa si el proceso se cae justo después de publicar?» → Se pierde.
   - «¿Y si quien tiene que reaccionar es **otra aplicación**, de otro equipo?» → No hay forma.
   - «¿Y si el consumidor está caído veinte minutos?» → Se pierde todo lo de esos veinte minutos.

3. **🗣️ Presenta el broker:** «Un intermediario que **guarda** el mensaje hasta que alguien lo procesa con
   éxito. Cambia tres cosas: los procesos se desacoplan en el tiempo, aparecen garantías de entrega… y
   aparecen problemas nuevos, que son los que vamos a ver hoy: duplicados, mensajes que no se pueden
   procesar y confirmaciones.»

4. **✏️ Enseña la consola de RabbitMQ vacía** (<http://localhost:15672> → *Queues and Streams*: no hay
   ninguna cola). **🗣️ Di:** «Al final de la primera parte, aquí habrá siete colas y las habrá creado
   nuestra aplicación al arrancar.»

---

## Paso 2 · Concepto: AMQP y la topología en la pizarra · 20 min

1. **Proyecta este diagrama** (o cópialo en la pizarra) **y déjalo a la vista las dos sesiones**. Es la
   topología de [`RabbitConfig`](src/main/java/com/atech/curso/m5/config/RabbitConfig.java), tal cual:

   ```mermaid
   flowchart LR
       pub(["PublicadorReservas"])
       ex{{"reservas.exchange<br/><b>topic</b>"}}

       subgraph trabajo ["Colas de trabajo"]
           direction TB
           fact["reservas.facturacion"]
           notif["reservas.notificaciones"]
           audi["reservas.auditoria"]
       end

       subgraph consumidores ["Consumidores (@RabbitListener)"]
           direction TB
           lfact(["FacturacionListener"])
           lnotif(["NotificacionesListener"])
           laudi(["AuditoriaListener"])
       end

       dlx{{"reservas.dlx<br/><b>direct</b>"}}

       subgraph problemas ["Mensajes con problemas"]
           direction TB
           err["reservas.errores"]
           dfact["reservas.facturacion.dlq"]
           dnotif["reservas.notificaciones.dlq"]
           daudi["reservas.auditoria.dlq"]
       end

       pub -- "reserva.confirmada<br/>reserva.cancelada" --> ex
       ex -- "reserva.confirmada" --> fact
       ex -- "reserva.*" --> notif
       ex -- "reserva.#" --> audi
       fact --> lfact
       notif --> lnotif
       audi --> laudi

       lfact -. "reintentos agotados" .-> dlx
       lnotif -.-> dlx
       laudi -.-> dlx
       fact -. "dead-letter" .-> dlx
       notif -.-> dlx
       audi -.-> dlx

       dlx -- "error" --> err
       dlx -- "#60;cola#62;.dlq" --> dfact
       dlx --> dnotif
       dlx --> daudi

       classDef exchange fill:#f3e8ff,stroke:#7e3fa0,stroke-width:2px,color:#2d2a32
       classDef cola fill:#e8f1fb,stroke:#2f6db5,color:#1b2b40
       classDef consumidor fill:#eaf6ee,stroke:#1b7f3b,color:#14361f
       classDef problema fill:#fdecec,stroke:#b00020,color:#4a0d16
       classDef productor fill:#fff6e0,stroke:#b07800,color:#3d2c00
       class ex,dlx exchange
       class fact,notif,audi cola
       class lfact,lnotif,laudi consumidor
       class err,dfact,dnotif,daudi problema
       class pub productor
       style trabajo fill:none,stroke:#2f6db5,stroke-dasharray:4 3
       style consumidores fill:none,stroke:#1b7f3b,stroke-dasharray:4 3
       style problemas fill:none,stroke:#b00020,stroke-dasharray:4 3
   ```

   Cómo leerlo: las flechas continuas son el camino normal y las punteadas, el de los mensajes con
   problemas. La etiqueta de cada flecha que sale de `reservas.exchange` es el **patrón del binding**. Las
   dos salidas hacia `reservas.dlx` son distintas y conviene distinguirlas desde ya (se ven a fondo en el
   paso 5):
   - **Reintentos agotados:** el *listener* falla 3 veces y `RepublishMessageRecoverer` lo republica con la
     clave `error`, que lo lleva a `reservas.errores` con la traza en las cabeceras.
   - **Dead-letter de la cola:** el broker lo saca de la cola (rechazo sin reencolar, TTL...) y lo envía a
     su DLQ con la clave `<cola>.dlq`.

2. **🗣️ El vocabulario, con el dibujo delante** (cinco palabras y ni una más):
   - **Exchange:** a quien publicas. **Nunca publicas a una cola.**
   - **Routing key:** la etiqueta del mensaje (`reserva.confirmada`).
   - **Binding:** la regla que une exchange y cola. En un *topic*, con patrones: `*` es **una** palabra,
     `#` son **cero o más**.
   - **Cola:** donde el mensaje espera.
   - **Ack:** el consumidor dice «hecho». Hasta entonces, el mensaje sigue siendo del broker.

3. **❓ Comprueba que lo han cogido** con dos preguntas rápidas sobre el dibujo:
   - «¿A qué colas llega `reserva.confirmada`?» → A las tres.
   - «¿Y `reserva.cancelada`?» → A notificaciones y auditoría, **no a facturación**. Una cancelación no se
     factura, y eso no está programado en ningún `if`: **está en los bindings**.

4. **🗣️ Remata la idea clave del diseño:** «El productor no sabe quién le escucha. Mañana entra un equipo
   nuevo que quiere las reservas confirmadas: añade su cola y su binding, y **no toca vuestro código**.»

---

## Paso 3 · Ejercicio EJ 5.1 (topología declarativa) · 30 min

1. **✏️ Proyecta [`RabbitConfig.java` líneas 49-77](src/main/java/com/atech/curso/m5/config/RabbitConfig.java#L49-L77)**
   y explica el bean `Declarables`:

   ```java
   Queue cola = QueueBuilder.durable(nombre)
           .deadLetterExchange(DLX)
           .deadLetterRoutingKey(dlq(nombre))
           .build();
   ```

   **🗣️ Di:** «`Declarables` es una lista de cosas que hay que declarar. `RabbitAdmin`, que Boot ya ha
   configurado, las crea en el broker al abrir la primera conexión. Y cada cola nace **con su dead-letter
   puesto**: el sitio donde caen los mensajes que la cola rechaza.»

2. **🗣️ `durable(...)`, con un ejemplo:** «Una cola duradera sobrevive al reinicio del broker. Una no
   duradera desaparece, con sus mensajes dentro. Parece obvio dicho así, y es el motivo por el que alguien
   pierde pedidos un lunes por la mañana.»

3. **Ejercicio (20 min).** El exchange *topic*, el DLX *direct*, las tres colas con sus DLQ y los enlaces.

   **⌨️ Criterio de aceptación** (no necesita broker, corre en milisegundos):

   ```bash
   ./mvnw -pl m5-amqp test -Dtest=TopologiaTest
   ```

   **✏️ Señala eso mismo** en la cabecera del test: usa `ApplicationContextRunner` con una
   `ConnectionFactory` simulada. «Se puede probar la topología **sin broker**. En el *pipeline*, esto vale
   oro.»

4. **⌨️ Y ahora arranca la aplicación y enséñalo en el broker:**

   ```bash
   ./mvnw -q -pl m5-amqp spring-boot:run
   ```

   **✏️ Recarga <http://localhost:15672> → *Queues and Streams*:** han aparecido las siete colas.
   Entra en `reservas.facturacion` y enseña, en *Features*, `DLX` y `DLK`. **🗣️ «Eso lo ha declarado
   nuestro código, no un administrador a mano en la consola.»**

5. **🔴 La rotura que les va a pasar a todos, provócala tú ahora** (2 min, y te ahorra veinte después):

   **✏️ En [`RabbitConfig.java` línea 69](src/main/java/com/atech/curso/m5/config/RabbitConfig.java#L69)**,
   cambia `QueueBuilder.durable(nombre)` por `QueueBuilder.nonDurable(nombre)` y vuelve a arrancar.

   **Salida esperada:** la aplicación falla al declarar la cola con
   `PRECONDITION_FAILED - inequivalent arg 'durable'`.

   **🗣️ Di:** «Las colas son **inmutables**: si cambiáis cualquier propiedad de una cola que ya existe, el
   broker os lo rechaza. Para desatascar: borrar la cola en la consola (*Queues* → la cola → *Delete*) o
   `docker compose down -v`. Os va a pasar hoy al menos una vez.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m5-amqp/src/main/java/com/atech/curso/m5/config/RabbitConfig.java
   ```

---

## Paso 4 · Productor y consumidores (EJ 5.2) · 25 min

1. **⌨️ Con la aplicación arrancada, publica un evento desde Swagger UI**
   (<http://localhost:8080/swagger-ui.html>): abre `POST /api/eventos/reservas-confirmadas`, elige el ejemplo
   **Reserva válida (R-1)** en *Examples* y pulsa **Execute**.

   **Salida esperada:** `202` con `Evento confirmado por el broker` en el cuerpo de la respuesta, y en el log
   de la aplicación, la factura emitida y la notificación. Pon el navegador y el log uno al lado del otro.

   <details><summary>Lo mismo con <code>curl</code>, si lo prefieres</summary>

   ```bash
   curl -X POST localhost:8080/api/eventos/reservas-confirmadas -H 'Content-Type: application/json' \
     -d '{"reservaId":"R-1","sala":"Turing","usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":30}'
   ```

   </details>

   **🗣️ De paso:** «Fijaos en el `202 Accepted` y no `201` ni `200`: el broker ha **aceptado** el mensaje, pero
   nadie lo ha procesado todavía. La respuesta HTTP llega antes que la factura.»

2. **✏️ Ve a la consola** → cola `reservas.auditoria` → *Get messages* → *Ack mode: Nack message requeue
   true* → *Get Message(s)*. **Proyecta el payload**: es JSON, y en *Properties* están `message_id`,
   `x-origen` y `__TypeId__`.

   **🗣️ Tres cosas que enseñar ahí mismo:**
   - **El cuerpo es JSON**, no una serialización binaria de Java. «Cualquier lenguaje puede consumirlo.
     Si veis `application/x-java-serialized-object` en un proyecto, es una bomba de acoplamiento.»
   - **`message_id` = el id de la reserva**
     ([`PublicadorReservas` línea 40](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java#L40)):
     será la clave de idempotencia.
   - **`__TypeId__`**: la cabecera con la clase original. Vuelve en el paso 5.

3. **✏️ Proyecta los tres consumidores seguidos** y señala **por qué son distintos a propósito**:

   | Listener | Firma | Qué enseña |
   |---|---|---|
   | [`FacturacionListener`](src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java) | `(ReservaConfirmada evento, @Header messageId)` | Tipo convertido + **idempotencia** |
   | [`NotificacionesListener`](src/main/java/com/atech/curso/m5/consumidores/NotificacionesListener.java) | `(ReservaConfirmada, @Header RECEIVED_ROUTING_KEY)` | Se puede leer con qué *routing key* llegó |
   | [`AuditoriaListener`](src/main/java/com/atech/curso/m5/consumidores/AuditoriaListener.java) | `(Message mensaje)` | El mensaje **en bruto**, sin convertir |

4. **🗣️ La idempotencia, que es el concepto más importante del módulo.** Proyecta
   [`FacturacionListener` líneas 42-45](src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java#L42-L45):

   ```java
   if (!idempotencia.primeraVez("facturacion", clave)) {
       log.info("Duplicado ignorado: {}", clave);
       return;
   }
   ```

   **🗣️ Di, despacio:** «RabbitMQ garantiza **al menos una vez**, no *exactamente una vez*. Si el
   consumidor procesa el mensaje y se cae justo antes de hacer el *ack*, el mensaje **se vuelve a entregar**.
   Eso no es un fallo del broker: es su diseño. La unicidad la ponéis vosotros.»

   **⌨️ Demuéstralo pulsando otra vez *Execute* con el ejemplo R-1 del punto 1.** En el log aparece
   `Duplicado ignorado: R-1` y **no** se emite una segunda factura.

   **❓ Pregunta abierta (vale la pena dedicarle 3 minutos):** «¿Cuál sería la clave de idempotencia en
   vuestro sistema, y dónde la guardaríais?» → En producción, una tabla con clave única
   `(consumidor, messageId)` escrita **en la misma transacción** que el efecto de negocio. Si están en
   transacciones distintas, no sirve de nada.

5. **🗣️ Cierra la sesión 4** con el gancho de mañana: «Mañana empezamos por lo que pasa cuando el mensaje
   **no se puede procesar**. Que es, con diferencia, la parte del diseño que más se descuida.»

---

## Paso 5 · 🔴 Errores y reintentos (EJ 5.3) · 45 min

**El bloque central del módulo.** Sigue estos siete puntos en orden.

1. **🗣️ Plantea el problema (3 min):** «Llega un mensaje y el consumidor revienta. ¿Qué debe pasar?»
   Recoge respuestas y ordénalas en la pizarra en tres opciones:
   - Descartarlo → pierdes datos, y encima en silencio.
   - Devolverlo a la cola tal cual → **bucle infinito**: vuelve a fallar, vuelve a la cola, vuelve a
     fallar… a máxima velocidad. Es el clásico que tumba un broker.
   - Reintentar unas cuantas veces y, si no hay manera, **apartarlo en un sitio donde alguien lo mire**.

2. **✏️ Proyecta [`application.properties` líneas 14-18](src/main/resources/application.properties#L14-L18):**

   ```properties
   spring.rabbitmq.listener.simple.retry.enabled=true
   spring.rabbitmq.listener.simple.retry.max-attempts=3
   spring.rabbitmq.listener.simple.retry.initial-interval=200ms
   spring.rabbitmq.listener.simple.retry.multiplier=2
   spring.rabbitmq.listener.simple.retry.max-interval=2s
   ```

   **🗣️ Di:** «Tres intentos con espera creciente: 200 ms, 400 ms. El *backoff* exponencial no es un
   capricho: si el fallo es que la base de datos está saturada, reintentar de inmediato **empeora** la
   saturación.»

3. **⌨️ Provoca el error, con la aplicación arrancada** (un importe negativo simula un fallo de negocio): en
   Swagger UI, elige el ejemplo **Importe negativo (R-400)** y pulsa **Execute**.

   **❓ Pregunta antes de mirar el log:** «La respuesta es `202`. ¿Ha ido bien?» → Para el broker, sí: ha
   recibido el mensaje. El fallo ocurre **después**, en el consumidor, y el productor no se entera. Esa es la
   esencia de la mensajería asíncrona.

   <details><summary>Con <code>curl</code></summary>

   ```bash
   curl -X POST localhost:8080/api/eventos/reservas-confirmadas -H 'Content-Type: application/json' \
     -d '{"reservaId":"R-400","sala":"Turing","usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":-5}'
   ```

   </details>

4. **✏️ Enseña el log:** tres intentos de facturación, con las esperas entre ellos, y luego el mensaje
   republicado.

5. **✏️ Y ahora la consola → cola `reservas.errores` → *Get messages*.** Proyecta las **cabeceras**
   `x-exception-message`, `x-exception-stacktrace`, `x-original-exchange` y `x-original-routingKey`.

   **🗣️ Di:** «Esto es lo que hace `RepublishMessageRecoverer`: no solo aparta el mensaje, sino que **le
   pega la traza del error encima**. Cuando dentro de seis meses alguien mire esa cola, sabrá qué pasó sin
   tener que buscar en los logs de aquel día.»

   **✏️ Enséñalo en el código**
   ([`RabbitConfig` líneas 99-102](src/main/java/com/atech/curso/m5/config/RabbitConfig.java#L99-L102)):

   ```java
   @Bean
   MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
       return new RepublishMessageRecoverer(rabbitTemplate, DLX, RK_ERROR);
   }
   ```

6. **❓ El debate de los 5 minutos:** «Sin este bean, Boot usa `RejectAndDontRequeueRecoverer`, que manda el
   mensaje a la DLQ **de su propia cola**. ¿Cuál preferís?»
   → Con DLQ por cola sabes de dónde venía el problema y puedes reprocesar cola por cola; con una cola de
   errores única tienes un solo sitio que vigilar y la traza dentro. Las dos son defendibles.
   **🗣️ Lo que no es defendible es no tener ninguna de las dos**, que es lo más común.

   **🗣️ Y la pregunta que va debajo:** «¿Y si el error **no es transitorio**? Si el mensaje viene mal
   formado, reintentar tres veces es perder tres veces el tiempo. Merece la pena distinguir en el
   consumidor: los errores de datos, directos a la cola de errores; los de infraestructura, reintentar.»

7. **🔴 Rotura fina (5 min): los *trusted packages*.** Es un fallo real que parece otra cosa.

   **✏️ En [`RabbitConfig.java` línea 91](src/main/java/com/atech/curso/m5/config/RabbitConfig.java#L91)**,
   quita el segundo argumento:

   ```java
   return new Jackson2JsonMessageConverter(objectMapper);   // sin "com.atech.curso.m5.eventos"
   ```

   **⌨️ Reinicia y vuelve a publicar el ejemplo R-1 desde Swagger UI.** Facturación y notificaciones **siguen
   funcionando**; auditoría falla y su mensaje acaba en `reservas.errores` con
   `is not in the trusted packages`.

   **🗣️ Explica por qué solo falla uno:** «Los listeners que declaran el tipo en la firma usan el **tipo
   inferido** del método. `AuditoriaListener` recibe un `Message` en bruto, así que el conversor tiene que
   resolver la clase por la cabecera `__TypeId__`… y por seguridad solo confía en `java.util` y
   `java.lang`. Esa restricción existe porque deserializar una clase arbitraria que te manda otro es una
   vía de ejecución remota de código.»

   **🗣️ Y la moraleja pedagógica, que es la importante:** «Fijaos en el síntoma: *un solo consumidor falla
   y sus mensajes acaban en la cola de errores*. Parece un problema de red o de carga. Es un problema de
   configuración. Distinguir eso rápido es exactamente lo que se paga en producción.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m5-amqp/src/main/java/com/atech/curso/m5/config/RabbitConfig.java
   ```

---

## Paso 6 · Publisher confirms y returns (EJ 5.4) · 20 min

1. **❓ Gancho:** «Habéis hecho `convertAndSend(...)` y no ha saltado ninguna excepción. ¿El mensaje está en
   el broker?» → **No lo sabéis.** El envío es asíncrono: sin confirmaciones, `convertAndSend` solo
   significa «se lo he dado al sistema operativo».

2. **✏️ Proyecta [`application.properties` líneas 7-9](src/main/resources/application.properties#L7-L9):**

   ```properties
   spring.rabbitmq.publisher-confirm-type=correlated
   spring.rabbitmq.publisher-returns=true
   spring.rabbitmq.template.mandatory=true
   ```

3. **✏️ Y el publicador**
   ([`PublicadorReservas` líneas 36-48](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java#L36-L48)):
   el `CorrelationData` y el `CompletableFuture<Confirm>` que devuelve.

   **✏️ Y el controlador**
   ([`ReservaEventosController` líneas 61-67](src/main/java/com/atech/curso/m5/productor/ReservaEventosController.java#L61-L67)):

   ```java
   CorrelationData.Confirm confirm = publicador.publicar(evento).get(5, TimeUnit.SECONDS);
   return confirm.isAck() ? ResponseEntity.accepted()... : ResponseEntity.internalServerError()...
   ```

   **🗣️ Di:** «Aquí el `202` significa de verdad *el broker lo tiene*. Esto cuesta latencia, así que es una
   decisión: para un evento de auditoría, quizá no compense; para un pago, no se discute.»

4. **🗣️ `mandatory` y los *returns*, que es lo que menos se conoce:** «Si publicáis con una *routing key*
   que no encaja con **ningún** binding, RabbitMQ **descarta el mensaje en silencio**. Con
   `mandatory=true` os lo devuelve, y el `setReturnsCallback`
   ([líneas 28-30](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java#L28-L30)) lo registra.»

   **⌨️ Demuéstralo** publicando con una *routing key* que no existe. Si has añadido un endpoint de prueba,
   úsalo; si no, hazlo desde la propia consola: *Exchanges* → `reservas.exchange` → *Publish message* con
   `routing key` = `pedido.creado` → **no llega a ninguna cola**.

   **🗣️ Remata:** «Un error de una letra en la *routing key* y el mensaje desaparece sin dejar rastro.
   Con `mandatory`, al menos hay un log.»

---

## Paso 7 · Pruebas de integración (EJ 5.5) · 15 min

1. **⌨️ Ejecuta la clase completa y proyéctala** (tarda: levanta un RabbitMQ real):

   ```bash
   ./mvnw -pl m5-amqp test -Dtest=RabbitIntegracionTest
   ```

2. **✏️ La cabecera**
   ([líneas 36-42](src/test/java/com/atech/curso/m5/RabbitIntegracionTest.java#L36-L42)):

   ```java
   @SpringBootTest
   @Testcontainers(disabledWithoutDocker = true)
   class RabbitIntegracionTest {
       @Container @ServiceConnection
       static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");
   ```

   **🗣️ «Ni host, ni puerto, ni credenciales en el test: `@ServiceConnection` los saca del contenedor.»**

3. **✏️ Y ahora **Awaitility**, que es lo que hay que llevarse:

   ```java
   await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
       assertThat(facturacion.facturas()).containsExactly("R-100");
       ...
   });
   ```

   **🗣️ Di:** «La entrega es asíncrona: cuando el test sigue, el consumidor **quizá** no ha terminado. La
   tentación es poner un `Thread.sleep(2000)`. Eso hace dos cosas malas a la vez: tarda dos segundos
   **siempre**, y falla el día que la máquina de integración vaya lenta. Awaitility comprueba cada poco y
   sale en cuanto se cumple.»

4. **✏️ Recorre los cuatro tests en 3 minutos**, porque son el resumen del módulo entero: difusión a las
   tres colas con *ack* del broker, la cancelación que **no** se factura, el duplicado que no genera dos
   facturas y el mensaje que acaba en `reservas.errores` tras tres intentos.

---

## Paso 8 · Cierre del módulo · 10 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m5-amqp test
   ```

   Mínimo exigible: `TopologiaTest` y `FacturacionListenerTest#ignoraDuplicados`.

2. **↩️ Limpieza (importante: el módulo 6 usa otro RabbitMQ en el mismo puerto):**

   ```bash
   docker compose -f m5-amqp/compose.yaml down
   git status --short
   ```

3. **🗣️ Las tres frases del módulo:**
   - El mensaje llega **al menos una vez**: diseña para el duplicado desde el primer día.
   - Todo mensaje que falla tiene que acabar en un sitio donde alguien lo mire, con la traza puesta.
   - Y publicar no es haber entregado: si el mensaje importa, espera la confirmación.

4. **Enlaza con el módulo 6:** «Hoy hemos publicado y consumido. Mañana, lo que pasa **entre medias**:
   cuando el mensaje hay que filtrarlo, partirlo, enrutarlo y volver a juntarlo. Y hay patrones con nombre
   para todo eso desde hace veinte años.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 6 (confirms) | 15 min | Cuéntalo con las propiedades y el `CompletableFuture` en pantalla, sin demo. |
| Paso 3, ejercicio | 15 min | Da la topología hecha, ejecuta `TopologiaTest` y enseña las colas en la consola. |
| Paso 7 | 10 min | Ejecuta tú el test y proyecta solo el bloque de Awaitility. |

**No recortes el paso 5** (errores y reintentos) **ni el punto 4 del paso 4** (idempotencia): son la razón
de ser del módulo.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa |
|---|---|
| `PRECONDITION_FAILED - inequivalent arg` | Han cambiado los argumentos de una cola ya existente. Borrarla en la consola o `docker compose down -v` |
| El listener recibe un `LinkedHashMap` | Falta el `MessageConverter` JSON, o la firma no declara el tipo |
| `is not in the trusted packages` | Faltan los paquetes de confianza en `Jackson2JsonMessageConverter` |
| El mensaje no llega a ninguna cola | *Routing key* que no encaja con ningún binding. Con `mandatory=true` al menos se registra |
| El test asíncrono falla a veces | `Thread.sleep` en vez de Awaitility |
| La cola se llena y no se consume | La aplicación no está arrancada, o el listener está en otra cola. Mira *Consumers* en la consola |
| Bucle infinito de reintentos | `requeue` sin límite: hay que usar reintentos con `MessageRecoverer` |
