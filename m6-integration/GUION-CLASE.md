# Guión de clase · Módulo 6 · Spring Integration

**Duración:** 2 h 30 min netas · **Sesión 5** del curso · Enunciados en [README.md](README.md)

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.
>
> **Levanta el broker antes de empezar** (este módulo publica en RabbitMQ):
>
> ```bash
> docker compose -f m5-amqp/compose.yaml down        # si viene del módulo 5, baja el anterior
> docker compose -f m6-integration/compose.yaml up -d
> ```
>
> **Regla de oro de este módulo: no empieces por el DSL, empieza por el dibujo.** Es el contenido más
> abstracto del curso. Si no ven el flujo en la pizarra, el código les parecerá una sopa de `handle`,
> `route` y `transform`.

---

## Paso 0 · Antes de entrar en el aula (10 min)

```bash
docker compose -f m6-integration/compose.yaml up -d
./mvnw -pl m6-integration test          # 2 clases en verde
```

Pestañas del IDE:

1. [`src/main/java/com/atech/curso/m6/web/SolicitudController.java`](src/main/java/com/atech/curso/m6/web/SolicitudController.java)
2. [`src/main/java/com/atech/curso/m6/flujos/ReservasGateway.java`](src/main/java/com/atech/curso/m6/flujos/ReservasGateway.java)
3. [`src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java`](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java) ← la clase central, tenla siempre a mano
4. [`src/test/java/com/atech/curso/m6/FlujosReservasTest.java`](src/test/java/com/atech/curso/m6/FlujosReservasTest.java)

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | Gancho: el controlador que no sabe nada | 10 | 0:10 |
| 2 | Concepto: EIP y el modelo de canales | 20 | 0:30 |
| 3 | Entradas: gateway y poller JDBC (EJ 6.1) | 30 | 1:00 |
| 4 | Procesamiento: filter, split, route, aggregate (EJ 6.2) | 45 | 1:45 |
| 5 | Salidas y robustez (EJ 6.3 y 6.4) | 25 | 2:10 |
| 6 | Pruebas (EJ 6.5) | 15 | 2:25 |
| 7 | Cierre del módulo | 5 | 2:30 |

---

## Paso 1 · Gancho: el controlador que no sabe nada · 10 min

1. **✏️ Proyecta [`SolicitudController.java`](src/main/java/com/atech/curso/m6/web/SolicitudController.java)**
   entera. Son 14 líneas útiles:

   ```java
   @PostMapping
   public ResponseEntity<Void> recibir(@RequestBody SolicitudReserva solicitud) {
       gateway.enviar(solicitud);
       return ResponseEntity.accepted().build();
   }
   ```

   **🗣️ Di:** «Un controlador normal que llama a una interfaz. Ni rastro de mensajería.»

2. **✏️ Ahora abre [`ReservasGateway.java`](src/main/java/com/atech/curso/m6/flujos/ReservasGateway.java):**

   ```java
   @MessagingGateway(defaultRequestChannel = "solicitudes")
   public interface ReservasGateway {
       void enviar(SolicitudReserva solicitud);
   }
   ```

3. **❓ Pregunta:** «¿Quién implementa esta interfaz?»
   → **Nadie.** La genera Spring Integration en tiempo de ejecución: envuelve el argumento en un mensaje y
   lo pone en el canal `solicitudes`.

   **🗣️ Engánchalo con lo que ya saben:** «Es el mismo truco que los repositorios de Spring Data del
   módulo 3: vosotros declaráis la intención, el framework pone la implementación. Y, como allí, por debajo
   hay un **proxy**: la caja de la pizarra del primer día.»

4. **🗣️ Plantea el módulo:** «Hoy vamos a montar lo que hay detrás de ese canal: un flujo que recibe
   solicitudes por dos sitios distintos, las filtra, las parte en líneas, decide qué hacer con cada una,
   llama a un servicio externo, junta los resultados y publica un resumen en RabbitMQ. Y todo eso, **sin un
   solo `if` de fontanería**.»

---

## Paso 2 · Concepto: EIP y el modelo de canales · 20 min

1. **Dibuja el flujo completo en la pizarra** (cópialo del README). Es **el mapa de las próximas dos horas**;
   ve tachando cada caja según la implementéis:

   ```
   ReservasGateway ─┐
                    ├─► solicitudes ─► filter ─► split ─► lineas ─► route(horas > 4)
   JDBC (poller) ───┘        │                                        ├─ no ─► tarifa estándar ─┐
                             ▼                                        └─ sí ─► HTTP /tarifas ───┤
                        descartadas                                                             │
                RabbitMQ ◄── Amqp.outbound ◄── ResumenSolicitud ◄── aggregate ◄── tarificadas ◄──┘
   ```

2. **🗣️ El modelo, en tres frases:**
   - Un **mensaje** es carga útil (*payload*) **más cabeceras**. Las cabeceras son tan importantes como el
     contenido: ahí viaja la información de control.
   - Un **canal** conecta dos puntos. Por defecto es directo y **síncrono** (el que envía ejecuta al que
     recibe); un `QueueChannel` desacopla de verdad.
   - Un **endpoint** es cada caja del dibujo: filtro, *splitter*, *router*, agregador, adaptador.

3. **🗣️ Sitúa la herramienta en 2 minutos, sin vender nada.** «¿Cuándo merece la pena esto en vez de
   escribir el código a mano? Cuando el flujo tiene **muchos pasos con reglas**, cuando hay **varias
   entradas y salidas distintas**, y sobre todo cuando queréis que **el diagrama de la pizarra y el código
   se parezcan**. Para leer una cola y guardar en base de datos, un `@RabbitListener` es más sencillo y es
   la respuesta correcta.»

4. **🗣️ Los patrones tienen nombre desde 2003** (el libro *Enterprise Integration Patterns*): filtro,
   *splitter*, *router*, agregador, *wire tap*. «Ese vocabulario es útil aunque no uséis Spring
   Integration: sirve para hablar con el equipo y para buscar en Google lo que os pasa.»

---

## Paso 3 · Entradas: gateway y poller JDBC (EJ 6.1) · 30 min

1. **⌨️ Arranca la aplicación y déjala corriendo:**

   ```bash
   ./mvnw -q -pl m6-integration spring-boot:run
   ```

2. **⌨️ Manda una solicitud por la entrada HTTP:**

   ```bash
   curl -X POST localhost:8080/api/solicitudes -H 'Content-Type: application/json' -d '{
     "id":"S-1","usuario":"ana@atech.es","lineas":[
       {"solicitudId":"S-1","sala":"Turing","fecha":"2030-03-01","horas":2},
       {"solicitudId":"S-1","sala":"Hopper","fecha":"2030-03-01","horas":6}]}'
   ```

   **✏️ Y enseña el resultado en la consola de RabbitMQ** (<http://localhost:15672>): la cola enlazada al
   exchange `reservas.exchange` con `solicitud.procesada` ha recibido el resumen.

   **🗣️ Di:** «Ha entrado por HTTP y ha salido por AMQP, pasando por cinco patrones. Ahora vamos a abrir
   la caja.»

3. **✏️ La segunda entrada: el poller JDBC**
   ([`FlujosReservas` líneas 76-92](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L76-L92)):

   ```java
   JdbcPollingChannelAdapter adaptador = new JdbcPollingChannelAdapter(dataSource,
           "SELECT * FROM linea_pendiente WHERE estado = 'PENDIENTE' ORDER BY id");
   adaptador.setUpdateSql("UPDATE linea_pendiente SET estado = 'PROCESADA' WHERE id IN (:id)");
   ```

   ```java
   IntegrationFlow.from(lineasPendientesSource,
           c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(10))).id(ENDPOINT_JDBC))
   ```

4. **❓ Pregunta clave (déjales pensar 1 minuto):** «¿Por qué hace falta el `updateSql`, y por qué tiene que
   ejecutarse en la **misma transacción** que el `SELECT`?»
   → Porque si no, el siguiente sondeo —diez segundos después— **vuelve a coger las mismas filas** y todo se
   procesa dos veces. Es el patrón *claim check* de toda la vida: marcar lo que ya has cogido.

   **🗣️ Añade el aviso de producción:** «Y si hay dos instancias de la aplicación, las dos sondean a la vez.
   Ahí hacen falta bloqueos (`SELECT ... FOR UPDATE SKIP LOCKED`) o un planificador distribuido. Que os lo
   pregunten en una revisión de arquitectura es buena señal.»

5. **✏️ Enseña la tabla** ([`schema.sql`](src/main/resources/schema.sql)) y los datos de ejemplo
   ([`data.sql`](src/main/resources/data.sql)): tres líneas pendientes, dos de la solicitud `S-100` y una de
   `S-101`.

6. **Ejercicio (15 min):** el `@MessagingGateway`, el controlador y el adaptador JDBC con su `updateSql`.

   **⌨️ Criterio:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest#elPollerJdbcProcesaLasLineasPendientes
   ```

---

## Paso 4 · Procesamiento: los patrones EIP (EJ 6.2) · 45 min

**Escribe en vivo el filtro y el *splitter*; deja el *router* y el agregador como ejercicio.**

1. **Filter (5 min).** **✏️ Proyecta
   [las líneas 65-73](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L65-L73):**

   ```java
   .filter(SolicitudReserva.class, s -> s.lineas() != null && !s.lineas().isEmpty(),
           f -> f.discardChannel("descartadas"))
   ```

   **🗣️ El detalle que importa:** «Fijaos en el `discardChannel`. Sin él, lo descartado **desaparece**.
   Con él, va a un canal donde alguien puede mirarlo. Es la misma idea que la cola de errores de ayer: en un
   sistema asíncrono, *tirar algo en silencio* nunca es una opción.»

   **⌨️ Demuéstralo:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest#elFiltroDescartaSolicitudesVacias
   ```

2. **Splitter (5 min).** La línea siguiente:

   ```java
   .split(SolicitudReserva.class, SolicitudReserva::lineas)
   ```

   **🗣️ Di:** «Entra un mensaje con dos líneas, salen dos mensajes. Y aquí viene lo importante para después:
   el *splitter* **añade cabeceras de secuencia** a cada mensaje hijo: `correlationId`, `sequenceSize` y
   `sequenceNumber`. Acordaos de esto dentro de veinte minutos.»

3. **Router (10 min).** **✏️ Proyecta
   [las líneas 95-101](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L95-L101):**

   ```java
   .route(LineaReserva.class, linea -> linea.horas() > 4,
           r -> r.channelMapping(true, "lineasLargas").channelMapping(false, "lineasCortas"))
   ```

   **❓ Pregunta de comprobación:** «¿Diferencia entre filtro y router?» → El **filtro** decide *si pasa*;
   el **router** decide *por dónde*. Vuelve al dibujo y señálalos.

4. **Aggregator (10 min). Explícalo ANTES de que lo programen**, con la pizarra, porque es donde se pierde
   todo el mundo:

   ```java
   .aggregate()
   ```

   **🗣️ Di:** «Una línea. ¿Cómo sabe qué mensajes van juntos y cuándo parar de esperar? Por las **cabeceras
   de secuencia que puso el *splitter***: agrupa por `correlationId` y libera el grupo cuando ha recibido
   `sequenceSize` mensajes. Por eso funciona sin configurar nada: *splitter* y agregador están hechos el uno
   para el otro.»

   **🗣️ Y el aviso que hay que dar sí o sí:** «Si el grupo **nunca se completa** —porque un mensaje se
   perdió por el camino, o porque venís de una fuente que no puso cabeceras de secuencia—, el agregador se
   queda esperando **para siempre y sin dar ningún error**. Es un fallo silencioso. Cuando useis un agregador
   en producción, configurad la estrategia de liberación y un *timeout* (`groupTimeout`, `expireGroupsUponCompletion`).»

5. **Ejercicio (15 min):** router y agregador.

   **⌨️ Criterio:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest#divideEnrutaTarificaYAgrega
   ```

   **Si alguien se atasca**, el desatascador universal de este módulo:

   ```bash
   ./mvnw -q -pl m6-integration spring-boot:run \
     -Dspring-boot.run.arguments=--logging.level.org.springframework.integration=DEBUG
   ```

   **🗣️ «Con el log en DEBUG se ve cada salto entre canales, con su carga y sus cabeceras. En este módulo,
   eso es el depurador.»**

---

## Paso 5 · Salidas y robustez (EJ 6.3 y 6.4) · 25 min

1. **✏️ Salida HTTP**
   ([líneas 112-122](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L112-L122)):

   ```java
   .handle(Http.outboundGateway(urlTarifas, restTemplateBuilder.build())
                   .httpMethod(HttpMethod.POST)
                   .expectedResponseType(LineaTarificada.class),
           e -> e.id(ENDPOINT_TARIFA).advice(reintentos))
   ```

   **🗣️ Dos cosas:** es un **gateway** (pide y **espera respuesta**), a diferencia del adaptador AMQP, que
   solo publica; y el `.id(...)` no es decorativo: **es el nombre por el que el test lo sustituirá por un
   simulador** (paso 6).

2. **✏️ El reintento**
   ([líneas 146-154](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L146-L154)):

   ```java
   RetryTemplate.builder().maxAttempts(3).exponentialBackoff(200, 2, 2000).build()
   ```

   **🗣️ «Mismo concepto de ayer con RabbitMQ y del módulo 1 con `@Retryable`. Tres sitios distintos, la
   misma idea: el fallo transitorio se reintenta con espera creciente.»**

3. **✏️ Salida AMQP**
   ([líneas 125-133](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L125-L133)):
   `Amqp.outboundAdapter(...).exchangeName(EXCHANGE).routingKey(RK_PROCESADA)`.
   **🗣️ «Mismo exchange que el módulo 5, otra *routing key*: `solicitud.procesada`. Los dos módulos podrían
   convivir en el mismo broker sin enterarse el uno del otro.»**

4. **❓ La mejor pregunta del módulo (dale 5 minutos):**

   **✏️ Proyecta [`gestionErrores`](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L136-L144),
   el suscriptor del `errorChannel` global.** Y pregunta:

   > «Si el flujo falla cuando la solicitud entró **por el poller JDBC**, el error llega a
   > `ErroresIntegracion`. Si falla cuando entró **por el gateway HTTP**, no llega. ¿Por qué?»

   Deja que lo intenten. La respuesta:
   → El **gateway síncrono tiene a quien devolverle la excepción**: quien llamó al método. Y debe ser él
   quien decida (devolver un 500, reintentar, avisar). El **poller no tiene a nadie**: nadie está esperando
   una respuesta, así que el error se publica en el `errorChannel`.

   **🗣️ Remátalo:** «Esa distinción —*¿hay alguien esperando la respuesta?*— es la que gobierna el
   tratamiento de errores en cualquier sistema asíncrono, uséis Spring Integration o no.»

5. **🗣️ Observabilidad (2 min, solo mención).** «Con el *starter* de Actuator añadido, hay un endpoint
   `/actuator/integrationgraph` que devuelve el grafo del flujo en JSON, y
   `spring.integration.management.observation-patterns=*` activa trazas por cada salto. Este módulo no
   incluye Actuator: si lo queréis ver, añadid `spring-boot-starter-actuator` al `pom.xml`. Merece el
   minuto que cuesta.»

---

## Paso 6 · Pruebas (EJ 6.5) · 15 min

1. **⌨️ Ejecuta y proyecta:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest
   ```

2. **✏️ La cabecera**
   ([líneas 36-38](src/test/java/com/atech/curso/m6/FlujosReservasTest.java#L36-L38)):

   ```java
   @SpringBootTest
   @SpringIntegrationTest(noAutoStartup = FlujosReservas.ENDPOINT_JDBC)
   ```

   **🗣️ «`noAutoStartup`: el poller **no arranca solo** en los tests. Si arrancara, estaría sondeando cada
   diez segundos mientras se ejecutan los demás tests y los resultados serían impredecibles. El test que lo
   necesita lo arranca a mano.»**

3. **✏️ Y la sustitución de los extremos**
   ([líneas 54-66](src/test/java/com/atech/curso/m6/FlujosReservasTest.java#L54-L66)):

   ```java
   mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_TARIFA, tarifas);
   publicados = MockIntegration.messageArgumentCaptor();
   mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_AMQP, amqp);
   ```

   **🗣️ Di:** «Aquí está el valor de haber puesto `.id(...)` en cada extremo: el test **desenchufa** el
   servicio HTTP real y RabbitMQ, y pone simuladores en su sitio. Se prueba **todo el flujo** —filtro,
   splitter, router, agregador— sin red y sin broker. Y con el `ArgumentCaptor` se comprueba exactamente qué
   se habría publicado.»

4. **🗣️ La regla que se llevan:** «Probad el flujo entero con los extremos simulados, y dejad uno o dos
   tests de verdad con Testcontainers para comprobar que los extremos también funcionan.»

---

## Paso 7 · Cierre del módulo · 5 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m6-integration test
   ```

   Mínimo exigible: `TransformacionesTest` y `FlujosReservasTest#divideEnrutaTarificaYAgrega`.

2. **↩️ Limpieza:**

   ```bash
   docker compose -f m6-integration/compose.yaml down
   ```

3. **🗣️ Las tres frases del módulo:**
   - El negocio no debe saber cómo le llegan los mensajes: para eso está el *gateway*.
   - Los patrones tienen nombre desde hace veinte años: úsalos y el diagrama será el código.
   - Y en un flujo asíncrono, la pregunta importante no es qué pasa cuando va bien, sino **adónde van los
     errores**.

4. **Enlaza con el módulo 7:** «Nos queda lo que hemos ido dejando para el final en todos los módulos:
   quién es el que llama y qué tiene permitido hacer.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 3, ejercicio del poller | 15 min | Enseña el adaptador y la tabla; el ejercicio se queda en el gateway. |
| Paso 5, puntos 1-3 | 10 min | Proyecta las tres salidas seguidas sin entrar en el `advice`. |
| Paso 6 | 10 min | Ejecuta el test y proyecta solo el `substituteMessageHandlerFor`. |

**No recortes el punto 4 del paso 4** (el agregador y sus cabeceras de secuencia) **ni el punto 4 del
paso 5** (gateway síncrono frente a poller): son los dos conceptos que no están en la documentación de
primeras.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Desatascador |
|---|---|---|
| «No pasa nada» | El canal de entrada no es el que creen, o el poller no arrancó | Log de `org.springframework.integration` en `DEBUG` |
| El agregador no emite nunca | Faltan las cabeceras de secuencia o la estrategia de liberación no se cumple | ¿Viene de un `split`? ¿Se han perdido mensajes por el camino? |
| El poller procesa las mismas filas una y otra vez | Falta el `updateSql` o no está en la misma transacción | Mirar el estado de las filas en la tabla |
| Confunden router y filtro | — | Volver al dibujo: *si pasa* frente a *por dónde* |
| El error del flujo no aparece en ningún sitio | Entró por el gateway síncrono: la excepción vuelve a quien llamó | Es el comportamiento correcto |
| El test es intermitente | El poller JDBC arrancando por su cuenta | `noAutoStartup` |
