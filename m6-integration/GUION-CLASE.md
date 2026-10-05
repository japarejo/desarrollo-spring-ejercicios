# Guión de clase · Módulo 6 · Spring Integration

**Duración:** 2 h 30 min netas · **Sesión 5** del curso · Enunciados en [README.md](README.md)

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`
> (los `cp` funcionan igual en PowerShell).
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
./mvnw -pl m6-integration test          # 3 clases en verde
```

Pestañas del IDE:

1. [`src/main/java/com/atech/curso/m6/web/SolicitudController.java`](src/main/java/com/atech/curso/m6/web/SolicitudController.java)
2. [`src/main/java/com/atech/curso/m6/flujos/ReservasGateway.java`](src/main/java/com/atech/curso/m6/flujos/ReservasGateway.java)
3. [`src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java`](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java) ← la clase central, tenla siempre a mano. Está dividida en tres bloques comentados (① entradas, ② procesamiento, ③ salidas) que coinciden con los del dibujo.
4. [`ejemplos/solicitudes.csv`](ejemplos/solicitudes.csv) y [`ejemplos/erronea.csv`](ejemplos/erronea.csv)
5. [`src/test/java/com/atech/curso/m6/FlujosReservasTest.java`](src/test/java/com/atech/curso/m6/FlujosReservasTest.java)

Pestañas del navegador: Swagger UI (<http://localhost:8080/swagger-ui.html>, cuando arranques la
aplicación en el paso 3) y la consola de RabbitMQ (<http://localhost:15672>, guest/guest).

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | Gancho: el controlador que no sabe nada | 10 | 0:10 |
| 2 | Concepto: EIP, canales y la historia del flujo | 20 | 0:30 |
| 3 | Entradas: gateway, poller JDBC y buzón de ficheros (EJ 6.1) | 35 | 1:05 |
| 4 | Procesamiento: filter, split, route, aggregate (EJ 6.2) | 40 | 1:45 |
| 5 | Salidas y robustez (EJ 6.3 y 6.4) | 30 | 2:15 |
| 6 | Pruebas (EJ 6.5) | 10 | 2:25 |
| 7 | Cierre del módulo | 5 | 2:30 |

---

## Paso 1 · Gancho: el controlador que no sabe nada · 10 min

1. **✏️ Proyecta el método `recibir` de
   [`SolicitudController.java`](src/main/java/com/atech/curso/m6/web/SolicitudController.java#L67-L71).**
   Quitando las anotaciones de Swagger, es esto:

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
   @MessagingGateway(defaultRequestChannel = "solicitudes",
           defaultHeaders = @GatewayHeader(name = FlujosReservas.CABECERA_ORIGEN, value = "web"))
   public interface ReservasGateway {
       void enviar(SolicitudReserva solicitud);
   }
   ```

3. **❓ Pregunta:** «¿Quién implementa esta interfaz?»
   → **Nadie.** La genera Spring Integration en tiempo de ejecución: envuelve el argumento en un mensaje, le
   añade la cabecera `origen=web` y lo pone en el canal `solicitudes`.

   **🗣️ Engánchalo con lo que ya saben:** «Es el mismo truco que los repositorios de Spring Data del
   módulo 3: vosotros declaráis la intención, el framework pone la implementación. Y, como allí, por debajo
   hay un **proxy**: la caja de la pizarra del primer día.»

4. **🗣️ Plantea el módulo con una historia** (es la que da sentido a todo el flujo):

   > «La empresa recibe solicitudes de reserva de salas **por tres sitios**: la **web** nueva; el **sistema
   > antiguo**, que las deja en una tabla de la base de datos; y **recepción**, que las apunta en una hoja de
   > cálculo y la exporta como **CSV** a una carpeta. Vengan de donde vengan, todas pasan por **las mismas
   > reglas**: se descartan las vacías, se tarifica cada línea y se calcula el total. Y el resultado va a
   > **dos sitios**: a **RabbitMQ**, para el resto de servicios (la facturación del módulo 5), y a una
   > **carpeta**, como justificante de texto, porque administración no tiene RabbitMQ.»

   **🗣️ Remata:** «Tres puertas, un pasillo y dos salidas. Hoy vamos a montarlo **sin un solo `if` de
   fontanería**.»

---

## Paso 2 · Concepto: EIP, canales y la historia del flujo · 20 min

1. **Proyecta este diagrama** (o cópialo en la pizarra). Es **el mapa de las próximas dos horas**; ve
   tachando cada caja según la implementéis. Es el flujo de
   [`FlujosReservas`](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java), tal cual:

   ```mermaid
   flowchart TB
       subgraph entradas ["① Entradas · EJ 6.1"]
           direction LR
           gw(["<b>Web</b><br/>ReservasGateway"])
           jdbc(["<b>Base de datos</b><br/>poller JDBC<br/>cada 10 s"])
           fich(["<b>Fichero CSV</b><br/>buzon/entrada<br/>cada 2 s"])
       end
   
       solicitudes[/"solicitudes"/]
       descartadas[("descartadas<br/>QueueChannel")]
   
       subgraph proceso ["② Procesamiento · EJ 6.2"]
           filtro["<b>filter</b><br/>¿tiene líneas?"]
           split["<b>split</b><br/>un mensaje por línea"]
           lineas[/"lineas"/]
           router{"<b>route</b><br/>horas > 4"}
           estandar["<b>transform</b><br/>tarifa estándar<br/>10 €/h"]
           http["<b>Http.outboundGateway</b><br/>tarifa de jornada<br/>9 €/h · 3 reintentos"]
           tarificadas[/"tarificadas"/]
           agregar["<b>aggregate</b><br/>reúne las líneas de cada solicitud"]
           resumir["<b>handle</b><br/>ResumenSolicitud + cabecera origen"]
       end
   
       resumenes[/"resumenes<br/>publicar-suscribir"/]
   
       subgraph salidas ["③ Salidas · EJ 6.3"]
           direction LR
           amqp["<b>Amqp.outboundAdapter</b><br/>solicitud.procesada"]
           fsal["<b>Files.outboundAdapter</b><br/>justificante S-x.txt"]
       end
   
       rabbit{{"RabbitMQ<br/>cola solicitudes.procesadas"}}
       buzon[("buzon/salida")]
       errores[/"errorChannel"/]
       registro(["ErroresIntegracion"])
   
       gw -- "origen=web" --> solicitudes
       jdbc -- "origen=base-de-datos" --> solicitudes
       fich -- "origen=fichero" --> solicitudes
       solicitudes --> filtro
       filtro -. "sin líneas" .-> descartadas
       filtro --> split --> lineas --> router
       router -- "no · lineasCortas" --> estandar
       router -- "sí · lineasLargas" --> http
       estandar --> tarificadas
       http --> tarificadas
       tarificadas --> agregar --> resumir --> resumenes
       resumenes --> amqp --> rabbit
       resumenes --> fsal --> buzon
       jdbc -. "si falla" .-> errores
       fich -. "si falla" .-> errores
       errores --> registro
   
       classDef entrada fill:#fff6e0,stroke:#b07800,color:#3d2c00
       classDef canal fill:#e8f1fb,stroke:#2f6db5,stroke-width:2px,color:#1b2b40
       classDef endpoint fill:#eaf6ee,stroke:#1b7f3b,color:#14361f
       classDef router fill:#f3e8ff,stroke:#7e3fa0,stroke-width:2px,color:#2d2a32
       classDef externo fill:#f1f1f1,stroke:#555,stroke-width:2px,color:#222
       classDef problema fill:#fdecec,stroke:#b00020,color:#4a0d16
       class gw,jdbc,fich entrada
       class solicitudes,lineas,tarificadas,resumenes canal
       class filtro,split,estandar,http,agregar,resumir,amqp,fsal endpoint
       class router router
       class rabbit,buzon externo
       class descartadas,errores,registro problema
       style entradas fill:none,stroke:#b07800,stroke-dasharray:4 3
       style proceso fill:none,stroke:#1b7f3b,stroke-dasharray:4 3
       style salidas fill:none,stroke:#1b7f3b,stroke-dasharray:4 3
   ```

   Cómo leerlo: los **paralelogramos azules son canales**, las **cajas verdes son endpoints** (cada uno,
   un patrón EIP), el **rombo es el router**, las **cápsulas amarillas son las entradas** y lo **rojo y
   punteado** es el camino de los mensajes que no siguen el flujo normal (se ve a fondo en el paso 5). Los
   números de los recuadros son el orden en que los vais a implementar y coinciden con los bloques de
   `FlujosReservas`.

2. **🗣️ El modelo, en tres frases:**
   - Un **mensaje** es carga útil (*payload*) **más cabeceras**. Las cabeceras son tan importantes como el
     contenido: ahí viaja la información de control. **Ejemplo en el dibujo:** cada entrada pone la
     cabecera `origen`, y no se vuelve a leer hasta el final, después de cinco saltos.
   - Un **canal** conecta dos puntos. Por defecto es directo y **síncrono** (el que envía ejecuta al que
     recibe); un `QueueChannel` desacopla de verdad, y un canal **publicar-suscribir** entrega cada mensaje a
     todos sus suscriptores.
   - Un **endpoint** es cada caja del dibujo: filtro, *splitter*, *router*, agregador, adaptador.

3. **❓ Pregunta sobre el dibujo:** «¿Cuántas veces está escrita la regla "más de 4 horas va al servicio de
   tarifas"?» → **Una.** Las tres entradas desembocan en el mismo canal `solicitudes`, así que **añadir una
   cuarta entrada** (un correo, un FTP...) **no toca nada del procesamiento**. Esa es la gracia.

4. **🗣️ Sitúa la herramienta en 2 minutos, sin vender nada.** «¿Cuándo merece la pena esto en vez de
   escribir el código a mano? Cuando el flujo tiene **muchos pasos con reglas**, cuando hay **varias
   entradas y salidas distintas** —como hoy—, y sobre todo cuando queréis que **el diagrama de la pizarra y
   el código se parezcan**. Para leer una cola y guardar en base de datos, un `@RabbitListener` es más
   sencillo y es la respuesta correcta.»

5. **🗣️ Los patrones tienen nombre desde 2003** (el libro *Enterprise Integration Patterns*): filtro,
   *splitter*, *router*, agregador, publicar-suscribir, *wire tap*. «Ese vocabulario es útil aunque no uséis
   Spring Integration: sirve para hablar con el equipo y para buscar en Google lo que os pasa.»

---

## Paso 3 · Entradas: gateway, poller JDBC y buzón de ficheros (EJ 6.1) · 35 min

1. **⌨️ Arranca la aplicación y déjala corriendo:**

   ```bash
   ./mvnw -q -pl m6-integration spring-boot:run
   ```

   **🗣️ Avisa:** «Al arrancar, el poller JDBC ya ha encontrado dos solicitudes pendientes en la tabla. Luego
   veremos dónde han acabado.»

2. **⌨️ Primera puerta: la web.** Abre **Swagger UI** (<http://localhost:8080/swagger-ui.html>) →
   `POST /api/solicitudes` → *Try it out* → en *Examples* elige **«Una línea corta y una larga (S-1)»** →
   *Execute*. Respuesta: **202**.

   > Desde la terminal sería lo mismo con
   > `curl -X POST localhost:8080/api/solicitudes -H 'Content-Type: application/json' -d '{"id":"S-1","lineas":[{"sala":"Turing","fecha":"2030-03-01","horas":2},{"sala":"Hopper","fecha":"2030-03-01","horas":6}]}'`.

   **✏️ Enseña las dos salidas:**
   - **RabbitMQ** (<http://localhost:15672>) → *Queues* → `solicitudes.procesadas` → *Get messages*: el
     resumen de `S-1` en JSON, con `"origen":"web"` y `"total":74`.
   - **El buzón de salida** `m6-integration/buzon/salida/`: el justificante `S-1.txt`. Y, junto a él,
     `S-100.txt` y `S-101.txt`: **las dos solicitudes de la base de datos**, que han hecho el mismo camino.

   **🗣️ Di:** «Ha entrado por HTTP y ha salido por AMQP y por un fichero, pasando por seis patrones. Ahora
   vamos a abrir la caja, puerta por puerta.»

3. **✏️ La segunda puerta: el poller JDBC**
   ([`FlujosReservas` líneas 75-92](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L75-L92)):

   ```java
   JdbcPollingChannelAdapter adaptador = new JdbcPollingChannelAdapter(dataSource,
           "SELECT * FROM linea_pendiente WHERE estado = 'PENDIENTE' ORDER BY id");
   adaptador.setUpdateSql("UPDATE linea_pendiente SET estado = 'PROCESADA' WHERE id IN (:id)");
   ```

   ```java
   IntegrationFlow.from(lineasPendientesSource,
                   c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(10))).id(ENDPOINT_JDBC))
           .enrichHeaders(h -> h.header(CABECERA_ORIGEN, "base-de-datos"))
           .transform(List.class, FlujosReservas::filasASolicitudes)
           .split()
           .channel("solicitudes")
   ```

   **🗣️ Léelo de arriba abajo:** «Cada 10 segundos lee las filas pendientes, les pone la cabecera `origen`,
   las agrupa por solicitud, las separa y las deja **en el mismo canal que la web**.»

4. **❓ Pregunta clave (déjales pensar 1 minuto):** «¿Por qué hace falta el `updateSql`, y por qué tiene que
   ejecutarse en la **misma transacción** que el `SELECT`?»
   → Porque si no, el siguiente sondeo —diez segundos después— **vuelve a coger las mismas filas** y todo se
   procesa dos veces. Es la idea de siempre: marcar lo que ya has cogido.

   **🗣️ Añade el aviso de producción:** «Y si hay dos instancias de la aplicación, las dos sondean a la vez.
   Ahí hacen falta bloqueos (`SELECT ... FOR UPDATE SKIP LOCKED`) o un planificador distribuido. Que os lo
   pregunten en una revisión de arquitectura es buena señal.»

   **✏️ Enseña la tabla** ([`schema.sql`](src/main/resources/schema.sql)) y los datos de ejemplo
   ([`data.sql`](src/main/resources/data.sql)): tres líneas pendientes, dos de la solicitud `S-100` y una de
   `S-101`.

   **⌨️ Y compruébalo en la consola H2** (<http://localhost:8080/h2-console>, JDBC URL
   `jdbc:h2:mem:reservas`, usuario `sa`, sin contraseña):

   ```sql
   SELECT * FROM linea_pendiente;
   ```

   Las tres filas están ya en `PROCESADA`: es el `updateSql` en acción. **⌨️ Mete una fila nueva** y mira
   cómo, en menos de 10 segundos, cambia de estado y aparece su justificante en `buzon/salida`:

   ```sql
   INSERT INTO linea_pendiente (solicitud_id, sala, fecha, horas) VALUES ('S-102', 'Turing', DATE '2030-03-09', 5);
   ```

5. **✏️ La tercera puerta: el buzón de ficheros**
   ([líneas 95-105](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L95-L105)):

   ```java
   IntegrationFlow.from(Files.inboundAdapter(buzonEntrada).patternFilter("*.csv"),
                   c -> c.poller(Pollers.fixedDelay(Duration.ofSeconds(2))).id(ENDPOINT_FICHEROS))
           .enrichHeaders(h -> h.header(CABECERA_ORIGEN, "fichero"))
           .transform(Files.toStringTransformer("UTF-8", true))
           .transform(String.class, FlujosReservas::csvASolicitudes)
           .split()
           .channel("solicitudes")
   ```

   **🗣️ Di:** «Ponedlo al lado del JDBC: **es el mismo flujo**. Cambia de dónde se lee, nada más. Sondear
   una carpeta es sondear una tabla.»

   **⌨️ Demuéstralo.** Enseña [`ejemplos/solicitudes.csv`](ejemplos/solicitudes.csv) (dos solicitudes,
   `S-200` y `S-201`) y déjalo en el buzón:

   ```bash
   cp m6-integration/ejemplos/solicitudes.csv m6-integration/buzon/entrada/
   ```

   En dos segundos, el CSV **desaparece** de `buzon/entrada`, aparecen `S-200.txt` y `S-201.txt` en
   `buzon/salida` y dos mensajes más con `"origen":"fichero"` en `solicitudes.procesadas`.

6. **❓ Pregunta:** «¿Cuál es el `updateSql` de esta entrada?»
   → El **`true`** de `Files.toStringTransformer("UTF-8", true)`: **borra el fichero** después de leerlo.
   Sin él, el adaptador recuerda en memoria lo que ya leyó, pero al reiniciar la aplicación lo procesaría
   otra vez.

   **🗣️ Los dos avisos de producción con ficheros:**
   - «Si quien deja el fichero tarda en escribirlo, el poller puede leerlo **a medias**. Solución clásica:
     escribirlo como `.tmp` y renombrarlo al terminar; el `patternFilter("*.csv")` ignora los `.tmp`.»
   - «Aquí borramos al leer: si algo falla después, el fichero se pierde. En producción se **mueve** a una
     carpeta `procesados/` o `errores/`.»

7. **Ejercicio (15 min):** el `@MessagingGateway` con su cabecera, el adaptador JDBC con su `updateSql` y
   la entrada de ficheros.

   **⌨️ Criterio:**

   ```bash
   ./mvnw -pl m6-integration test "-Dtest=FlujosReservasTest#elPollerJdbcProcesaLasLineasPendientes+elBuzonDeEntradaLeeElCsvYDejaLosJustificantes"
   ```

---

## Paso 4 · Procesamiento: los patrones EIP (EJ 6.2) · 40 min

**Escribe en vivo el filtro y el *splitter*; deja el *router* y el agregador como ejercicio.**

1. **Filter (5 min).** **✏️ Proyecta
   [las líneas 116-124](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L116-L124):**

   ```java
   .filter(SolicitudReserva.class, s -> !s.lineas().isEmpty(),
           f -> f.discardChannel("descartadas"))
   ```

   **🗣️ El detalle que importa:** «Fijaos en el `discardChannel`. Sin él, lo descartado **desaparece**.
   Con él, va a un canal donde alguien puede mirarlo. Es la misma idea que la cola de errores de ayer: en un
   sistema asíncrono, *tirar algo en silencio* nunca es una opción.»

   **⌨️ Demuéstralo en Swagger UI:** envía el ejemplo **«Solicitud sin líneas (S-VACIA)»** (202, pero no sale
   nada en RabbitMQ ni en el buzón) y después `GET /api/solicitudes/descartadas`: ahí está. Vuelve a
   llamarlo: la lista sale vacía.

   **🗣️ Di:** «`descartadas` es un `QueueChannel`: los mensajes **esperan** a que alguien los recoja, y al
   recogerlos salen del canal. Es el único canal del flujo que guarda mensajes.»

2. **Splitter (5 min).** La línea siguiente:

   ```java
   .split(SolicitudReserva.class, SolicitudReserva::lineas)
   ```

   **🗣️ Di:** «Entra un mensaje con dos líneas, salen dos mensajes. Y aquí viene lo importante para después:
   el *splitter* **añade cabeceras de secuencia** a cada mensaje hijo: `correlationId`, `sequenceSize` y
   `sequenceNumber`. **Y copia las cabeceras del padre**, así que el `origen` sigue viajando. Acordaos de
   esto dentro de veinte minutos.»

3. **Router (10 min).** **✏️ Proyecta
   [las líneas 127-133](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L127-L133):**

   ```java
   .route(LineaReserva.class, linea -> linea.horas() > 4,
           r -> r.channelMapping(true, "lineasLargas").channelMapping(false, "lineasCortas"))
   ```

   **🗣️ La regla de negocio:** «Las reservas cortas se cobran con la tarifa estándar, 10 €/hora, que
   calculamos aquí mismo. Las de **jornada** —más de 4 horas— tienen tarifa propia, 9 €/hora, y esa la
   calcula **otro sistema**, el servicio de tarifas, al que llamamos por HTTP.»

   **❓ Pregunta de comprobación:** «¿Diferencia entre filtro y router?» → El **filtro** decide *si pasa*;
   el **router** decide *por dónde*. Vuelve al dibujo y señálalos.

4. **Aggregator (10 min). Explícalo ANTES de que lo programen**, con la pizarra, porque es donde se pierde
   todo el mundo
   ([líneas 157-164](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L157-L164)):

   ```java
   .aggregate()
   .handle(List.class, (lineas, cabeceras) -> resumir(lineas, (String) cabeceras.get(CABECERA_ORIGEN)))
   .channel("resumenes")
   ```

   **🗣️ Di:** «`aggregate()`, una línea. ¿Cómo sabe qué mensajes van juntos y cuándo parar de esperar? Por
   las **cabeceras de secuencia que puso el *splitter***: agrupa por `correlationId` y libera el grupo cuando
   ha recibido `sequenceSize` mensajes. Por eso funciona sin configurar nada: *splitter* y agregador están
   hechos el uno para el otro.»

   **🗣️ Y el `handle` de después:** «Recibe la carga útil **y las cabeceras**. Aquí, al final del todo, se
   lee por fin el `origen` que pusieron las entradas: ha sobrevivido al *splitter*, al *router*, a la
   llamada HTTP y al agregador. Esto es lo que significa que *las cabeceras viajan con el mensaje*.»

   **🗣️ Y el aviso que hay que dar sí o sí:** «Si el grupo **nunca se completa** —porque un mensaje se
   perdió por el camino, o porque venís de una fuente que no puso cabeceras de secuencia—, el agregador se
   queda esperando **para siempre y sin dar ningún error**. Es un fallo silencioso. Cuando uséis un agregador
   en producción, configurad la estrategia de liberación y un *timeout* (`groupTimeout`,
   `expireGroupsUponCompletion`).»

5. **Ejercicio (15 min):** router y agregador.

   **⌨️ Criterio:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest#divideEnrutaTarificaYAgrega
   ```

   **Si alguien se atasca**, el desatascador universal de este módulo:

   ```bash
   ./mvnw -q -pl m6-integration spring-boot:run "-Dspring-boot.run.arguments=--logging.level.org.springframework.integration=DEBUG"
   ```

   **🗣️ «Con el log en DEBUG se ve cada salto entre canales, con su carga y sus cabeceras. En este módulo,
   eso es el depurador.»**

---

## Paso 5 · Salidas y robustez (EJ 6.3 y 6.4) · 30 min

1. **✏️ Salida HTTP**
   ([líneas 144-154](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L144-L154)):

   ```java
   .handle(Http.outboundGateway(urlTarifas, restTemplateBuilder.build())
                   .httpMethod(HttpMethod.POST)
                   .expectedResponseType(LineaTarificada.class),
           e -> e.id(ENDPOINT_TARIFA).advice(reintentos))
   ```

   **🗣️ Dos cosas:** es un **gateway** (pide y **espera respuesta**), a diferencia de los adaptadores de
   salida, que solo envían; y el `.id(...)` no es decorativo: **es el nombre por el que el test lo
   sustituirá por un simulador** (paso 6). El servicio de tarifas es
   [`TarifasController`](src/main/java/com/atech/curso/m6/web/TarifasController.java), en el mismo proceso
   para no complicar la práctica (por eso no aparece en Swagger UI: lo llama el flujo, no las personas).

2. **✏️ El reintento**
   ([líneas 223-231](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L223-L231)):

   ```java
   RetryTemplate.builder().maxAttempts(3).exponentialBackoff(200, 2, 2000).build()
   ```

   **🗣️ «Mismo concepto de ayer con RabbitMQ y del módulo 1 con `@Retryable`. Tres sitios distintos, la
   misma idea: el fallo transitorio se reintenta con espera creciente.»**

3. **✏️ El canal publicar-suscribir**
   ([líneas 169-172](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L169-L172)) **y sus dos
   suscriptores**:

   ```java
   @Bean
   PublishSubscribeChannel resumenes() { return new PublishSubscribeChannel(); }
   ```

   **🗣️ Di:** «Cada resumen tiene que ir a dos sitios. En lugar de un `handle` que haga las dos cosas,
   declaramos un canal **publicar-suscribir** y colgamos de él dos flujos independientes. Es el exchange
   *fanout* del módulo 5, pero dentro de la aplicación. ¿Que mañana hay que mandar también un correo? Un
   tercer suscriptor, sin tocar los otros dos.»

   - **AMQP**
     ([líneas 175-181](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L175-L181)):
     `Amqp.outboundAdapter(...).exchangeName(EXCHANGE).routingKey(RK_PROCESADA)`. **🗣️ «Mismo exchange que
     el módulo 5, otra *routing key*: `solicitud.procesada`. Los dos módulos podrían convivir en el mismo
     broker sin enterarse el uno del otro.»** La aplicación declara el exchange y la cola
     `solicitudes.procesadas` ([líneas 184-197](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L184-L197))
     para poder ver los mensajes en la consola.
   - **Fichero**
     ([líneas 200-208](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L200-L208)):

     ```java
     IntegrationFlow.from("resumenes")
             .enrichHeaders(h -> h.headerFunction(FileHeaders.FILENAME,
                     m -> ((ResumenSolicitud) m.getPayload()).solicitudId() + ".txt", true))
             .transform(ResumenSolicitud.class, ResumenSolicitud::justificante)
             .handle(Files.outboundAdapter(buzonSalida).fileExistsMode(FileExistsMode.REPLACE))
     ```

     **❓ Pregunta trampa:** «¿Para qué es el `true` del final de `headerFunction`?» → Para **sobrescribir**
     la cabecera si ya existe. Y existe en las solicitudes que llegaron por el buzón: el adaptador de entrada
     puso `file_name=solicitudes.csv`, y ha viajado hasta aquí. Sin el `true`, **todos los justificantes de
     ese CSV se llamarían `solicitudes.csv`** y se pisarían unos a otros. Otra vez: las cabeceras viajan.

4. **❓ La mejor pregunta del módulo (dale 5 minutos), con demostración en vivo:**

   **⌨️ Deja en el buzón un CSV mal formado** ([`ejemplos/erronea.csv`](ejemplos/erronea.csv) tiene
   `horas = dos`):

   ```bash
   cp m6-integration/ejemplos/erronea.csv m6-integration/buzon/entrada/
   ```

   En el log aparece: `Error en un flujo de integración: For input string: "dos"`. Es
   [`gestionErrores`](src/main/java/com/atech/curso/m6/flujos/FlujosReservas.java#L213-L221), el suscriptor
   del `errorChannel` global, que lo pasa a `ErroresIntegracion`. Y pregunta:

   > «Si el flujo falla con una solicitud que entró **por el buzón o por la base de datos**, el error llega
   > a `ErroresIntegracion`. Si falla con una que entró **por la web**, no llega: Swagger UI recibe un 500.
   > ¿Por qué?»

   Deja que lo intenten. La respuesta:
   → El **gateway síncrono tiene a quien devolverle la excepción**: quien llamó al método. Y debe ser él
   quien decida (devolver un 500, reintentar, avisar). Los **pollers no tienen a nadie**: nadie está
   esperando una respuesta, así que el error se publica en el `errorChannel`.

   **🗣️ Remátalo:** «Esa distinción —*¿hay alguien esperando la respuesta?*— es la que gobierna el
   tratamiento de errores en cualquier sistema asíncrono, uséis Spring Integration o no.»

5. **🗣️ Observabilidad (2 min, solo mención).** «Con el *starter* de Actuator añadido, hay un endpoint
   `/actuator/integrationgraph` que devuelve el grafo del flujo en JSON, y
   `spring.integration.management.observation-patterns=*` activa trazas por cada salto. Este módulo no
   incluye Actuator: si lo queréis ver, añadid `spring-boot-starter-actuator` al `pom.xml`. Merece el
   minuto que cuesta.»

---

## Paso 6 · Pruebas (EJ 6.5) · 10 min

1. **⌨️ Ejecuta y proyecta:**

   ```bash
   ./mvnw -pl m6-integration test -Dtest=FlujosReservasTest
   ```

2. **✏️ La cabecera**
   ([líneas 42-45](src/test/java/com/atech/curso/m6/FlujosReservasTest.java#L42-L45)):

   ```java
   @SpringBootTest(properties = {
           "atech.ficheros.entrada=" + FlujosReservasTest.ENTRADA,
           "atech.ficheros.salida=" + FlujosReservasTest.SALIDA })
   @SpringIntegrationTest(noAutoStartup = { FlujosReservas.ENDPOINT_JDBC, FlujosReservas.ENDPOINT_FICHEROS })
   ```

   **🗣️ «`noAutoStartup`: los pollers **no arrancan solos** en los tests. Si arrancaran, estarían sondeando
   mientras se ejecutan los demás tests y los resultados serían impredecibles. El test que los necesita los
   arranca a mano. Y los buzones apuntan a `target/test-buzon`, para no mezclar con los de la aplicación.»**

3. **✏️ Y la sustitución de los extremos**
   ([líneas 71-85](src/test/java/com/atech/curso/m6/FlujosReservasTest.java#L71-L85)):

   ```java
   mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_TARIFA, tarifas);
   publicados = MockIntegration.messageArgumentCaptor();
   mockIntegration.substituteMessageHandlerFor(FlujosReservas.ENDPOINT_AMQP, amqp);
   ```

   **🗣️ Di:** «Aquí está el valor de haber puesto `.id(...)` en cada extremo: el test **desenchufa** el
   servicio HTTP real y RabbitMQ, y pone simuladores en su sitio. Se prueba **todo el flujo** —las tres
   entradas, filtro, splitter, router, agregador— sin red y sin broker. Con el `ArgumentCaptor` se comprueba
   exactamente qué se habría publicado. **Los ficheros, en cambio, no se simulan**: son baratos y locales, así
   que el test escribe un CSV de verdad y comprueba que aparecen los justificantes.»

4. **🗣️ La regla que se llevan:** «Simulad lo que es caro o remoto (red, broker), usad de verdad lo que es
   barato y local (ficheros, H2), y dejad uno o dos tests con Testcontainers para comprobar que los extremos
   reales también funcionan.»

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
   rm -rf m6-integration/buzon          # opcional: los justificantes generados en clase
   ```

3. **🗣️ Las tres frases del módulo:**
   - El negocio no debe saber cómo le llegan los mensajes: para eso están el *gateway* y los adaptadores.
     Una entrada nueva es un adaptador nuevo, no un cambio en las reglas.
   - Los patrones tienen nombre desde hace veinte años: úsalos y el diagrama será el código.
   - Y en un flujo asíncrono, la pregunta importante no es qué pasa cuando va bien, sino **adónde van los
     errores**.

4. **Enlaza con el módulo 7:** «Nos queda lo que hemos ido dejando para el final en todos los módulos:
   quién es el que llama y qué tiene permitido hacer.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 3, ejercicio | 10 min | Enseña el adaptador JDBC y el de ficheros con la demo; el ejercicio se queda en el gateway. |
| Paso 5, puntos 1-2 | 8 min | Proyecta la pasarela HTTP sin entrar en el `advice`. |
| Paso 6 | 5 min | Ejecuta el test y proyecta solo el `substituteMessageHandlerFor`. |

**No recortes el punto 4 del paso 4** (el agregador y sus cabeceras de secuencia) **ni el punto 4 del
paso 5** (gateway síncrono frente a poller): son los dos conceptos que no están en la documentación de
primeras.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Desatascador |
|---|---|---|
| «No pasa nada» | El canal de entrada no es el que creen, o el poller no arrancó | Log de `org.springframework.integration` en `DEBUG` |
| El CSV se queda en el buzón | No termina en `.csv`, o está en otra carpeta | La ruta absoluta del buzón está en la descripción de Swagger UI |
| El agregador no emite nunca | Faltan las cabeceras de secuencia o la estrategia de liberación no se cumple | ¿Viene de un `split`? ¿Se han perdido mensajes por el camino? |
| El poller procesa las mismas filas una y otra vez | Falta el `updateSql` o no está en la misma transacción | Mirar el estado de las filas en la tabla |
| Los justificantes del CSV se pisan entre sí | `headerFunction` sin el `true`: se conserva el `file_name` del CSV de entrada | Sobrescribir la cabecera |
| Confunden router y filtro | — | Volver al dibujo: *si pasa* frente a *por dónde* |
| El error del flujo no aparece en ningún sitio | Entró por el gateway síncrono: la excepción vuelve a quien llamó | Es el comportamiento correcto (Swagger UI recibe un 500) |
| El test es intermitente | Un poller arrancando por su cuenta | `noAutoStartup` |
