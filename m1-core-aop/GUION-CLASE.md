# Guión de clase · Módulo 1 · El núcleo de Spring y AOP

**Duración:** 3 h 45 min netas · **Sesión 1** del curso · Enunciados en [README.md](README.md)

> **Cómo se lee este guión.** Cada paso lleva lo que hay que hacer, en orden y con el comando o la acción
> exacta. Los iconos: **⌨️** ejecuta esto · **✏️** edita esto · **🗣️** dilo así · **❓** pregunta al aula
> (con la respuesta que buscas) · **🔴** rotura provocada · **↩️** cómo deshacerla.
>
> Todos los comandos se lanzan **desde la raíz del repositorio**, no desde la carpeta del módulo.
> En Windows, `mvnw.cmd` en lugar de `./mvnw`.

---

## Paso 0 · Antes de entrar en el aula (10 min, el día anterior)

```bash
./mvnw -pl m1-core-aop test                     # todo en verde: 5 clases de test
./mvnw -pl m1-core-aop spring-boot:run          # arranca, imprime dos pedidos y termina solo
git status --short                               # el repositorio tiene que estar limpio
```

Ten abiertos en pestañas del IDE, en este orden (es el orden en que los vas a necesitar):

1. [`src/main/java/com/atech/curso/m1/pedidos/PedidoService.java`](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java)
2. [`src/main/java/com/atech/curso/m1/notificacion/Notificador.java`](src/main/java/com/atech/curso/m1/notificacion/Notificador.java)
3. [`src/main/java/com/atech/curso/m1/precios/SinDescuento.java`](src/main/java/com/atech/curso/m1/precios/SinDescuento.java)
4. [`src/main/java/com/atech/curso/m1/auditoria/AuditoriaAspect.java`](src/main/java/com/atech/curso/m1/auditoria/AuditoriaAspect.java)
5. [`src/test/java/com/atech/curso/m1/PedidoServiceTest.java`](src/test/java/com/atech/curso/m1/PedidoServiceTest.java)
6. [`src/main/java/com/atech/curso/m1/pista/PistaAuditoriaAspect.java`](src/main/java/com/atech/curso/m1/pista/PistaAuditoriaAspect.java)

**Dibuja esto en la pizarra antes de empezar y no lo borres en todo el módulo** (lo usarás otra vez en M3 y en M7):

```
   cliente ──► [ PROXY ] ──► PedidoService (el objeto real)
                  │
                  └── aquí viven @Auditado, @Transactional, @Retryable, @PreAuthorize
```

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | Gancho: la misma aplicación con dos canales | 10 | 0:10 |
| 2 | Concepto: IoC, DI y perfiles | 15 | 0:25 |
| 3 | Ejercicio EJ 1.1 + EJ 1.2 | 35 | 1:00 |
| 4 | Puesta en común + 🔴 rotura de `@Primary` | 15 | 1:15 |
| 5 | Eventos de aplicación (EJ 1.3) | 25 | 1:40 |
| 6 | Concepto: AOP y proxies | 15 | 1:55 |
| 7 | Demo y ejercicio del aspecto (EJ 1.4) | 35 | 2:30 |
| 8 | 🔴 Auto-invocación (EJ 1.5) | 20 | 2:50 |
| 9 | Reintentos (EJ 1.6, solo demo) | 10 | 3:00 |
| 10 | Pista de auditoría (EJ 1.7) | 35 | 3:35 |
| 11 | Cierre del módulo | 10 | 3:45 |

Pausa de 15 min después del paso 4 o del paso 5, lo que caiga más cerca de la hora y media.

---

## Paso 1 · Gancho: la misma aplicación con dos canales · 10 min

**Objetivo:** que vean el cambio de comportamiento **antes** de saber cómo se hace.

1. **⌨️ Ejecuta, sin explicar nada todavía:**

   ```bash
   ./mvnw -q -pl m1-core-aop spring-boot:run
   ```

   Señala en la salida las dos líneas `[email] ana@atech.es: Su pedido P-1 ha sido confirmado...`

2. **⌨️ Ejecuta ahora con el perfil `sms`:**

   ```bash
   ./mvnw -q -pl m1-core-aop spring-boot:run -Dspring-boot.run.profiles=sms
   ```

   Salida esperada (proyéctala y léela en voz alta):

   ```
   ... The following 1 profile is active: "sms"
   ... c.a.c.m1.notificacion.SmsNotificador : [sms] ana@atech.es: Su pedido P-1 ha sido confirmado (120.50 EUR)
   ... c.a.curso.m1.auditoria.AuditoriaAspect : AUDITORIA op=confirmar-pedido metodo=PedidoService.confirmar(..) ok=true ms=17
   ```

3. **❓ Pregunta:** «¿Qué línea de código he cambiado entre las dos ejecuciones?»
   → **Ninguna.** Solo un parámetro de arranque.

4. **✏️ Abre [`PedidoService.java`](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java)** y pide que
   busquen la palabra `Notificador` en el fichero. **No aparece.** El servicio ni siquiera sabe que existen
   las notificaciones.

5. **🗣️ Di:** «En las próximas tres horas vamos a construir esto: quién decide qué implementación se usa,
   cómo se entera el notificador de que hay un pedido, y de dónde sale esa tercera línea de AUDITORIA que
   nadie ha escrito en el servicio.»

6. **❓ Para los que ya usan Spring:** «¿Cuántos tenéis en vuestro código algún `if (entorno.equals("pre"))`?»
   Es el mismo problema resuelto a mano. Apunta los ejemplos que salgan: los reutilizarás en el paso 2.

---

## Paso 2 · Concepto: IoC, DI y perfiles · 15 min

**Objetivo:** vocabulario mínimo y la razón de ser del contenedor. Máximo 15 minutos hablando.

1. **🗣️ Los tres conceptos, en este orden:**
   - **Inversión de control:** tú no construyes tus colaboradores, los pides. El contenedor los construye.
   - **Inyección de dependencias:** la forma concreta de pedirlos. Por constructor, siempre que se pueda.
   - **Bean:** un objeto que gestiona el contenedor, con su ciclo de vida (creación, inicialización,
     destrucción) y su ámbito (*singleton* por defecto).

2. **✏️ Proyecta [`Notificador.java`](src/main/java/com/atech/curso/m1/notificacion/Notificador.java)** y sus
   dos implementaciones. Señala las anotaciones de una en una:
   - `@Component` en [`EmailNotificador`](src/main/java/com/atech/curso/m1/notificacion/EmailNotificador.java):
     «esto es un bean, regístralo».
   - `@Profile("!sms")`: «está activo **salvo** que el perfil `sms` lo esté». Las expresiones admiten
     `!` (no), `&` (y), `|` (o).
   - `@Profile("sms")` en [`SmsNotificador`](src/main/java/com/atech/curso/m1/notificacion/SmsNotificador.java).

3. **🗣️ La regla:** «Las dos implementaciones son mutuamente excluyentes. Si te equivocas y quedan las dos
   activas, el arranque falla; lo veremos dentro de un rato, en el paso 4.»

4. **🗣️ Inyección por constructor, con [`PedidoService.java` líneas 26-34](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java#L26-L34)
   en pantalla:** campos `final`, un único constructor, **sin `@Autowired`** (desde Spring 4.3 no hace falta).
   Tres razones: el objeto no puede existir a medio construir, las dependencias están a la vista, y se puede
   instanciar con `new` en un test. La tercera la comprobarán ellos en el paso 4.

5. **🗣️ Menciona `Clock` de pasada** (línea 28): «una dependencia que no parece una dependencia. Volveremos
   a ella cuando queramos que un test sea determinista.»

---

## Paso 3 · Ejercicio EJ 1.1 + EJ 1.2 · 35 min

1. **🗣️ Da el enunciado y el criterio de aceptación** (el README del módulo, apartados EJ 1.1 y EJ 1.2):

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest='PerfilPorDefectoTest,PerfilSmsTest,PedidoServiceUnitTest'
   ```

   **🗣️ Di:** «Cuando esos tres tests estén en verde, el ejercicio está hecho.»

2. **Qué tienen que escribir** (proyéctalo o pásalo por chat):
   - La interfaz `Notificador` con `canal()` y `notificar(destinatario, mensaje)`.
   - `EmailNotificador` (`@Profile("!sms")`) y `SmsNotificador` (`@Profile("sms")`, que trunca a 160 caracteres).
   - `PedidoService` con inyección por constructor.
   - `CalculadoraDescuento` con `SinDescuento` (`@Primary`) y `DescuentoBlackFriday` (`@Qualifier("blackFriday")`).
   - El bean `Clock` en la clase de aplicación.

3. **Mientras trabajan**, pasa por los puestos buscando estos tres fallos, que son los que salen siempre:

   | Lo que ves | Qué les dices |
   |---|---|
   | `@Autowired` sobre el campo | «Quítalo y ponlo en el constructor; luego veremos por qué.» |
   | Han puesto `@Profile("sms")` y `@Profile("!sms")` en la misma clase | Las anotaciones no se acumulan así: una clase, un perfil. |
   | El test de SMS falla porque el mensaje no se trunca | El truncado va en `SmsNotificador`, no en el servicio: el servicio no sabe de canales. |

4. **A los 30 minutos avisa:** «Cinco minutos. Quien no lo tenga, que se quede en el punto donde esté; lo
   resolvemos juntos ahora.»

---

## Paso 4 · Puesta en común y rotura de `@Primary` · 15 min

1. **Proyecta [`PedidoServiceUnitTest.java`](src/test/java/com/atech/curso/m1/PedidoServiceUnitTest.java)**
   y léelo entero, son 12 líneas:

   ```java
   PedidoService servicio = new PedidoService(publisher, new DescuentoBlackFriday(), reloj);
   ```

   **🗣️ Di:** «Esto es el argumento de la inyección por constructor. Ni `@SpringBootTest`, ni contexto, ni
   dos segundos de arranque: `new` y ya. Un test así se ejecuta mil veces al día sin que nadie se queje.»

2. **Señala el reloj fijo** (línea 26): `Clock.fixed(Instant.parse("2026-11-27T10:00:00Z"), ZoneOffset.UTC)`.
   **❓ Pregunta:** «¿Cómo probaríais esto si el servicio llamara a `Instant.now()`?» → No se puede, salvo
   con trucos. Por eso el reloj se inyecta.

3. **🔴 Rotura: quita `@Primary`.**

   **✏️ En [`SinDescuento.java` línea 10](src/main/java/com/atech/curso/m1/precios/SinDescuento.java#L10)**,
   comenta la anotación:

   ```java
   //@Primary
   ```

   **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest=PerfilPorDefectoTest
   ```

   **Salida esperada** (proyéctala; el bloque `Description/Action` es lo importante):

   ```
   Parameter 1 of constructor in com.atech.curso.m1.pedidos.PedidoService required a single bean, but 2 were found:
   Action:
   Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans,
   or using @Qualifier to identify the bean that should be consumed
   ```

   **🗣️ Di:** «Leed el mensaje entero. Spring os dice qué ha encontrado y las tres salidas posibles. La
   mitad de las incidencias con Spring se resuelven leyendo el error hasta el final.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m1-core-aop/src/main/java/com/atech/curso/m1/precios/SinDescuento.java
   ```

4. **❓ Pregunta de cierre del bloque:** «`@Primary` y `@Qualifier`, ¿cuál gana?» → `@Qualifier` es explícito
   en el punto de inyección y manda sobre `@Primary`, que solo decide cuando nadie ha pedido nada concreto.

---

## Paso 5 · Eventos de aplicación (EJ 1.3) · 25 min

1. **🗣️ Plantea el problema (2 min):** «El servicio de pedidos ya funciona. Ahora quieren que, al confirmar,
   se envíe una notificación. ¿Inyectamos el `Notificador` en `PedidoService`?» Deja que digan que sí.
   «Y mañana, además, hay que actualizar el stock, avisar a facturación y sumar una métrica. ¿Cuántas
   dependencias tendrá el servicio dentro de un año?»

2. **✏️ Proyecta [`PedidoConfirmado.java`](src/main/java/com/atech/curso/m1/pedidos/PedidoConfirmado.java)**
   (un `record`, cuatro campos) y
   [`PedidoService#confirmar` líneas 37-46](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java#L37-L46):
   `eventos.publishEvent(evento)`. **El servicio no conoce a nadie.**

3. **✏️ Proyecta los dos listeners** y señala la diferencia:
   - [`NotificacionPedidoListener`](src/main/java/com/atech/curso/m1/pedidos/NotificacionPedidoListener.java): `@EventListener` normal.
   - [`GrandesPedidosListener` línea 16](src/main/java/com/atech/curso/m1/pedidos/GrandesPedidosListener.java#L16):
     `@EventListener(condition = "#evento.importe() > 1000")` — SpEL, y el evento ni se entrega si no se cumple.

4. **Ejercicio (15 min).** Criterio:

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest='PedidoServiceTest#confirmarPublicaEventoYNotifica+listenerCondicionalSoloParaGrandesPedidos'
   ```

5. **❓ Pregunta mientras compila:** «¿Qué pasa si un listener lanza una excepción?»
   → Los `@EventListener` son **síncronos** por defecto: la excepción sube hasta quien publicó el evento.
   Si eso ocurre dentro de una transacción, se va todo al traste. De ahí `@TransactionalEventListener(phase = AFTER_COMMIT)`,
   que se verá en M3 y que es justo lo que quieres para «manda el correo solo si de verdad se guardó».
   **🗣️ Añade:** «Y si lo quieres asíncrono, `@Async`, pero entonces pierdes la transacción y el orden.
   Nada es gratis.»

---

## Paso 6 · Concepto: AOP y proxies · 15 min

1. **🗣️ Plantea el problema:** «Queremos saber cuánto tarda cada operación de negocio y si terminó bien.
   ¿Dónde ponemos el cronómetro?» Deja que propongan meterlo en el método. «¿Y en los otros cuarenta
   métodos de la aplicación?»

2. **🗣️ Vocabulario, con la pizarra** (cuatro palabras, no más):
   - *join point*: un punto donde se puede intervenir (en Spring: **la ejecución de un método público de un bean**).
   - *pointcut*: la expresión que selecciona los *join points* (aquí, «los métodos anotados con `@Auditado`»).
   - *advice*: el código que se ejecuta (`@Around`, `@Before`, `@AfterReturning`, `@AfterThrowing`).
   - *weaving*: cómo se une todo. **Spring lo hace en tiempo de ejecución, con proxies.**

3. **🗣️ Vuelve al dibujo de la pizarra** y sé explícito: «Cuando pedís un `PedidoService`, Spring no os da
   vuestro objeto: os da **un envoltorio** que tiene dentro vuestro objeto. Las anotaciones actúan en el
   envoltorio. Esta frase explica el 80 % de los "pues a mí no me funciona" del resto del curso.»

4. **❓ Pregunta:** «¿Y si el método es privado?» → El proxy no puede interceptarlo. Igual que con las
   llamadas internas, que es lo que veremos en el paso 8.

---

## Paso 7 · Demo y ejercicio del aspecto (EJ 1.4) · 35 min

1. **Demo (15 min): escribe el aspecto en vivo, no lo pegues.** Son 20 líneas y se ve el mecanismo.
   Ve nombrando cada parte mientras la escribes, en este orden:

   ```java
   @Aspect                                  // «esto contiene advices»
   @Component                               // «y además es un bean, si no, no se aplica»
   public class AuditoriaAspect {

       @Around("@annotation(auditado)")     // «el pointcut; el parámetro se enlaza con la anotación»
       public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
           long inicio = System.nanoTime();
           boolean correcta = false;
           try {
               Object resultado = pjp.proceed();   // «AQUÍ se llama al método real»
               correcta = true;
               return resultado;
           }
           finally {                                // «finally: también hay que auditar los fallos»
               registro.registrar(...);
           }
       }
   }
   ```

   **🗣️ Insiste en dos puntos:** si olvidas `pjp.proceed()`, **el método de negocio no se ejecuta** (un
   `@Around` puede saltarse la llamada, y eso es tan potente como peligroso); y el `finally` es lo que hace
   que un fallo también quede registrado.

2. **⌨️ Ejecuta los dos tests del aspecto:**

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest='PedidoServiceTest#elAspectoAuditaLaLlamadaExterna+elAspectoRegistraTambienLosFallos'
   ```

3. **✏️ Enseña la primera línea del primer test**
   ([`PedidoServiceTest.java` línea 70](src/test/java/com/atech/curso/m1/PedidoServiceTest.java#L70)):

   ```java
   assertThat(AopUtils.isAopProxy(pedidos)).as("PedidoService debe ser un proxy").isTrue();
   ```

   **🗣️ Di:** «El test comprueba explícitamente que lo que le han inyectado es un proxy. Guardaos ese
   `AopUtils.isAopProxy(...)`: es la primera comprobación cuando algo anotado no se aplica.»

4. **Ejercicio (20 min):** la anotación `@Auditado`, el aspecto y anotar `PedidoService#confirmar`.
   Criterio: los dos tests anteriores en verde.

   **Fallo que verás en varios puestos:** el aspecto no se dispara porque falta `@Component` sobre la clase
   anotada con `@Aspect`. `@Aspect` sola no registra nada: hay que decirle al contenedor que ese objeto existe.

---

## Paso 8 · 🔴 Auto-invocación (EJ 1.5) · 20 min

**El momento más rentable del módulo. No lo abrevies.**

1. **✏️ Proyecta [`PedidoService#confirmarLote` líneas 54-56](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java#L54-L56):**

   ```java
   public List<PedidoConfirmado> confirmarLote(List<Pedido> pedidos) {
       return pedidos.stream().map(this::confirmar).toList();
   }
   ```

2. **❓ Pregunta antes de ejecutar nada:** «Confirmamos un lote de dos pedidos. ¿Cuántas entradas de
   auditoría se registran?» Pide que voten a mano alzada entre 0, 1 y 2. **La mayoría dirá 2.**

3. **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest=PedidoServiceTest#autoInvocacionNoPasaPorElProxy
   ```

4. **✏️ Proyecta el test** ([líneas 90-101](src/test/java/com/atech/curso/m1/PedidoServiceTest.java#L90-L101))
   y lee las dos aserciones en voz alta:

   ```java
   assertThat(eventos.stream(PedidoConfirmado.class)).hasSize(2);   // el código SÍ se ejecuta
   assertThat(auditoria.entradas()).isEmpty();                      // el aspecto NO se aplica
   ```

5. **Deja 2 minutos de silencio** para que lo expliquen ellos. Luego vuelve a la pizarra y dibuja la flecha
   que se queda dentro del objeto real, sin pasar por el proxy.

6. **🗣️ Las cuatro soluciones, con su veredicto** (escríbelas en la pizarra):

   | Solución | Cuándo |
   |---|---|
   | Mover el método a **otro bean** | Casi siempre la correcta. Además suele mejorar el diseño. |
   | Inyectarse **a sí mismo** (`@Lazy` o `ObjectProvider`) | Funciona, es feo, y delata que el diseño quiere partirse. |
   | `AopContext.currentProxy()` | Requiere `@EnableAspectJAutoProxy(exposeProxy = true)`. Acopla el código a Spring. |
   | **AspectJ** con tejido en carga o compilación | Intercepta todo, incluso privados. Potente y caro de montar. |

7. **🗣️ Cierra con el aviso que vale para todo el curso:** «Esto mismo pasa con `@Transactional` (módulo 3)
   y con `@PreAuthorize` (módulo 7). Es **un solo mecanismo**. Cuando algo anotado no funcione, lo primero
   que tenéis que preguntaros es: ¿estoy pasando por el proxy?»

---

## Paso 9 · Reintentos (EJ 1.6, solo demo) · 10 min

1. **✏️ Proyecta [`EmailNotificador` líneas 29-38](src/main/java/com/atech/curso/m1/notificacion/EmailNotificador.java#L29-L38):**

   ```java
   @Retryable(retryFor = IllegalStateException.class, maxAttempts = 3,
              backoff = @Backoff(delay = 50, multiplier = 2))
   ```

   Y [`M1Application` línea 23](src/main/java/com/atech/curso/m1/M1Application.java#L23): `@EnableRetry`.
   **🗣️ Di:** «Sin `@EnableRetry` no pasa nada en absoluto, y no avisa. Es el error típico.»

2. **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest=PedidoServiceTest#reintentaCuandoElServidorDeCorreoFalla
   ```

   El test simula 2 fallos y comprueba que hubo **3 intentos** y **1 mensaje enviado**.

3. **❓ Pregunta:** «¿Qué operaciones **no** debéis reintentar nunca?» → Las que no son idempotentes: cobrar
   una tarjeta, enviar el mismo correo tres veces. El reintento presupone que repetir es inofensivo.
   **🗣️ Adelanta:** «En el módulo 5 esto vuelve, ya con un broker de por medio, y ahí la idempotencia deja
   de ser opcional.»

4. **🗣️ Nota para Boot 4:** Spring Retry desaparece del proyecto; se usa
   `@EnableResilientMethods` + `org.springframework.resilience.annotation.Retryable`. Mismo concepto, otro paquete.

---

## Paso 10 · Pista de auditoría (EJ 1.7) · 35 min

**Objetivo:** el ejemplo de manual de interés transversal. Aquí se entiende **para qué** sirve la POA de verdad.

1. **🗣️ Distingue los dos aspectos (3 min).** «El del paso 7 **mide**: cuánto tardó, si fue bien. Esto otro
   es distinto: es el rastro funcional que pide una auditoría o el regulador. **Quién** hizo **qué**, sobre
   **qué entidad**, **cuándo** y con **qué resultado**. Ese registro se guarda durante años.»

2. **✏️ Proyecta [`PedidoService#reembolsar` líneas 66-75](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java#L66-L75):**

   ```java
   @PistaAuditoria(accion = "REEMBOLSAR", entidad = "Pedido")
   public Reembolso reembolsar(@IdEntidad String pedidoId, BigDecimal importe, @Sensible String iban) {
   ```

   **❓ Pregunta:** «¿Dónde está, en el cuerpo del método, el código que averigua qué usuario está conectado?»
   → **No está.** El método solo tiene reglas de negocio. De lo demás se ocupa el aspecto.

3. **✏️ Proyecta [`PistaAuditoriaAspect`](src/main/java/com/atech/curso/m1/pista/PistaAuditoriaAspect.java)**
   y explica las tres decisiones de diseño:
   - **Líneas 55-63:** `@AfterReturning` y `@AfterThrowing` en vez de `@Around`.
     **❓ ¿Por qué?** → Porque solo hay que registrar el desenlace. Al no rodear la llamada, el aspecto **no
     puede** tragarse la excepción ni cambiar el valor devuelto por descuido. El menor poder posible.
   - **Líneas 85-86:** el *quién* sale de `UsuarioActual` y el *cuándo* del `Clock` **inyectados en el aspecto**.
   - **Líneas 96-100:** `AopUtils.getMostSpecificMethod(...)`, porque con un proxy JDK la firma del *join point*
     puede ser la del interfaz, que no lleva las anotaciones de los parámetros.

4. **Ejercicio (20 min).** Criterio:

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest=PistaAuditoriaTest
   ```

   Son 5 tests: el registro correcto, el enmascarado, el fallo, el rastro por usuario y la comprobación de que
   `confirmar()` **no** deja pista (cada aspecto atiende a su interés y no se pisan).

5. **🔴 Rotura final (5 min): quita `@Sensible`.**

   **✏️ En [`PedidoService.java` línea 67](src/main/java/com/atech/curso/m1/pedidos/PedidoService.java#L67)**,
   borra `@Sensible` del parámetro `iban`.

   **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m1-core-aop test -Dtest=PistaAuditoriaTest#enmascaraLosDatosSensiblesYConservaElResto
   ```

   El test falla mostrando el IBAN completo donde esperaba `****1332`.

   **🗣️ Di:** «Ese registro se conserva siete años. Acabáis de guardar un IBAN en claro en un fichero que
   nadie va a volver a mirar hasta que lo mire quien no debe. Y fijaos en que **el test lo ha detectado**:
   la protección de datos también se puede probar.»

   **↩️ Deshaz:** vuelve a escribir `@Sensible` delante del parámetro `iban` (o `Ctrl+Z` en el IDE).
   **No uses `git checkout` en este fichero** si tienes cambios propios sin confirmar: te los llevarías por
   delante.

6. **❓ Pregunta para pensar (déjala abierta, no la cierres tú):** «Si el aspecto falla al escribir en el
   libro de auditoría, ¿debe impedir el reembolso?» → No hay respuesta única: para auditoría regulatoria, sí
   (si no se puede registrar, no se puede operar); para una métrica, no. Lo importante es que **es una
   decisión de negocio** y que hoy, tal como está el código, la respuesta es «sí» sin que nadie la haya tomado.

---

## Paso 11 · Cierre del módulo · 10 min

1. **⌨️ Que todos ejecuten, en su proyecto:**

   ```bash
   ./mvnw -pl m1-core-aop test
   ```

   Mínimo exigible: `PerfilSmsTest`, `PedidoServiceUnitTest` y
   `PedidoServiceTest#autoInvocacionNoPasaPorElProxy`.

2. **🗣️ Las tres frases del módulo** (dilas mirando la pizarra):
   - El contenedor ensambla, tú no.
   - Lo que no es lógica de negocio —medir, auditar, reintentar, transaccionar, autorizar— vive fuera.
   - Y todo eso ocurre en el proxy: si te saltas el proxy, te saltas todo.

3. **Enlaza con lo siguiente:** «Mañana empezamos viendo cómo se hacía todo esto con XML, que es lo que os
   vais a encontrar en las aplicaciones heredadas, y luego entramos en Spring Boot.»

4. **Termómetro (2 min):** que cada persona escriba en el chat **una cosa clara y una dudosa**. Las dudosas
   son el guión de tus primeros 10 minutos de mañana.

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 9 (reintentos) | 10 min | Menciónalo en una frase y enseña solo la anotación. |
| Paso 10, ejercicio | 20 min | Conviértelo en demo: proyecta el aspecto ya hecho y ejecuta `PistaAuditoriaTest`. |
| Paso 3, EJ 1.2 | 10 min | Da hechas las dos calculadoras de descuento y que solo hagan `PedidoService`. |

**No recortes nunca el paso 8** (auto-invocación): es el que evita más horas perdidas en el resto del curso.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Comprobación |
|---|---|---|
| El aspecto no se dispara | Auto-invocación, falta `@Component` sobre `@Aspect`, o el método no es público | `AopUtils.isAopProxy(bean)` |
| `NoUniqueBeanDefinitionException` | Dos implementaciones sin `@Primary` ni `@Qualifier` | El propio mensaje lista los candidatos |
| El perfil no cambia nada | Se activó en el sitio equivocado | Línea `The following 1 profile is active` del arranque |
| Los parámetros salen `arg0` en EJ 1.7 | Falta compilar con `-parameters` | Boot lo configura; un proyecto a mano, no |
| `@Retryable` no reintenta | Falta `@EnableRetry` | No da ningún aviso: hay que saberlo |
