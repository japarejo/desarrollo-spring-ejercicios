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
./mvnw -pl m5-amqp test                 # 3 clases; RabbitIntegracionTest se omite sin Docker
```

Comprueba que entras en <http://localhost:15672> y **ten localizadas estas tres pestañas de la consola**:
*Exchanges*, *Queues and Streams* y, dentro de una cola, el botón **Get messages**. Las vas a usar seis o
siete veces.

Pestañas del IDE:

1. [`src/main/java/com/atech/curso/m5/config/RabbitConfig.java`](src/main/java/com/atech/curso/m5/config/RabbitConfig.java)
2. [`src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java`](src/main/java/com/atech/curso/m5/consumidores/FacturacionListener.java)
3. [`src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java`](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java)
4. [`src/main/resources/application.yml`](src/main/resources/application.yml)

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

1. **Dibuja esto en la pizarra** (tal cual, y no lo borres en las dos sesiones):

   ```
                              ┌──► reservas.facturacion    ─(DLX)─► reservas.facturacion.dlq
   publicador ──► reservas.exchange (topic)
                   reserva.confirmada  ──► reservas.notificaciones ─(DLX)─► ....dlq   [reserva.*]
                                       ──► reservas.auditoria      ─(DLX)─► ....dlq   [reserva.#]

   reservas.dlx (direct) ── error ──► reservas.errores   (los que agotan los reintentos)
   ```

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

1. **⌨️ Con la aplicación arrancada, publica un evento desde otra terminal:**

   ```bash
   curl -X POST localhost:8080/api/eventos/reservas-confirmadas -H 'Content-Type: application/json' \
     -d '{"reservaId":"R-1","sala":"Turing","usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":30}'
   ```

   **Salida esperada:** `Evento confirmado por el broker` (el `202`), y en el log de la aplicación, la
   factura emitida y la notificación.

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

   **⌨️ Demuéstralo publicando dos veces el mismo `curl` del punto 1.** En el log aparece
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

2. **✏️ Proyecta [`application.yml` líneas 17-24](src/main/resources/application.yml#L17-L24):**

   ```yaml
   retry:
     enabled: true
     max-attempts: 3
     initial-interval: 200ms
     multiplier: 2
     max-interval: 2s
   ```

   **🗣️ Di:** «Tres intentos con espera creciente: 200 ms, 400 ms. El *backoff* exponencial no es un
   capricho: si el fallo es que la base de datos está saturada, reintentar de inmediato **empeora** la
   saturación.»

3. **⌨️ Provoca el error, con la aplicación arrancada** (un importe negativo simula un fallo de negocio):

   ```bash
   curl -X POST localhost:8080/api/eventos/reservas-confirmadas -H 'Content-Type: application/json' \
     -d '{"reservaId":"R-400","sala":"Turing","usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":-5}'
   ```

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

   **⌨️ Reinicia y vuelve a publicar el `curl` del paso 4.** Facturación y notificaciones **siguen
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

2. **✏️ Proyecta [`application.yml` líneas 10-14](src/main/resources/application.yml#L10-L14):**

   ```yaml
   publisher-confirm-type: correlated
   publisher-returns: true
   template:
     mandatory: true
   ```

3. **✏️ Y el publicador**
   ([`PublicadorReservas` líneas 36-48](src/main/java/com/atech/curso/m5/productor/PublicadorReservas.java#L36-L48)):
   el `CorrelationData` y el `CompletableFuture<Confirm>` que devuelve.

   **✏️ Y el controlador**
   ([`ReservaEventosController` líneas 27-33](src/main/java/com/atech/curso/m5/productor/ReservaEventosController.java#L27-L33)):

   ```java
   CorrelationData.Confirm confirm = publicador.publicar(evento).get(5, TimeUnit.SECONDS);
   return confirm.isAck() ? ResponseEntity.accepted()... : ResponseEntity.internalServerError()...
   ```

   **🗣️ Di:** «Aquí el `202` significa de verdad *el broker lo tiene*. Esto cuesta latencia, así que es una
   decisión: para un evento de auditoría, quizá no compense; para un pago, no se discute.»

4. **🗣️ `mandatory` y los *returns*, que es lo que menos se conoce:** «Si publicáis con una *routing key*
   que no encaja con **ningún** binding, RabbitMQ **descarta el mensaje en silencio**. Con
   `mandatory: true` os lo devuelve, y el `setReturnsCallback`
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
| Paso 6 (confirms) | 15 min | Cuéntalo con el YAML y el `CompletableFuture` en pantalla, sin demo. |
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
| El mensaje no llega a ninguna cola | *Routing key* que no encaja con ningún binding. Con `mandatory: true` al menos se registra |
| El test asíncrono falla a veces | `Thread.sleep` en vez de Awaitility |
| La cola se llena y no se consume | La aplicación no está arrancada, o el listener está en otra cola. Mira *Consumers* en la consola |
| Bucle infinito de reintentos | `requeue` sin límite: hay que usar reintentos con `MessageRecoverer` |
