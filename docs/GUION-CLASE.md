# Guión de impartición · Desarrollo de aplicaciones con Spring

El [README](../README.md) describe **qué** contiene el repositorio. Este documento es el **plan del curso**:
preparación, ritmo, reparto en sesiones, evaluación y recortes.

> **El detalle paso a paso está en el guión de cada módulo**, junto a su código: comandos exactos, ficheros
> y líneas que proyectar, roturas provocadas con su salida real y preguntas para el aula. Los tienes
> enlazados en [§4](#4-guiones-por-módulo). Si vas a dar una clase concreta, ve directamente a su fichero;
> esta página se lee una vez, al preparar el curso.
>
> **Supuestos de partida** (ajústalos a tu convocatoria): 30 horas repartidas en **6 sesiones de 5 h**.
> Los tiempos de este guión son **netos de contenido**: suman ~24 h 10 min y dejan ~1 h de colchón por
> sesión para preguntas, incidencias de entorno y pausas. Si tu curso es de 25 h, aplica los recortes
> de la sección [§8](#8-si-vas-con-retraso-qué-se-recorta).

---

## 1. Antes del curso

### Una semana antes

- [ ] Envía al alumnado los **requisitos**: JDK 21, Docker Desktop, IDE y **cuenta de GitHub** (módulo 7).
- [ ] Pide que ejecuten `./mvnw verify` **en su máquina** y avisen de problemas. La primera compilación
      descarga ~400 MB de dependencias: que no ocurra el primer día a las 9:00 con 15 personas a la vez.
- [ ] Módulo 7: registra **tu** OAuth App de GitHub (`http://localhost:8080/login/oauth2/code/github`) y,
      si vas a enseñar Google, las credenciales de Google Cloud Console. No las metas en el repositorio.

### El día anterior

```bash
./mvnw -q verify                              # todo en verde, con Docker levantado
docker pull postgres:17-alpine
docker pull rabbitmq:4.1-management
docker pull quay.io/keycloak/keycloak:26.3
```

- [ ] Deja las tres imágenes descargadas: en clase, con el wifi compartido, una descarga de Keycloak
      te come 20 minutos de sesión.
- [ ] Ten abiertos y probados: consola de RabbitMQ (`:15672`), consola de Keycloak (`:8180`),
      Swagger UI (`:8080/swagger-ui.html`) y la consola H2.
- [ ] Prepara el IDE para proyectar: fuente grande, tema claro, paneles laterales cerrados,
      terminal integrada visible.

### Los primeros 20 minutos del día 1

1. **Presentaciones rápidas** (5'): nombre, qué versión de Spring usan hoy en su trabajo y qué les duele
   de ella. Apunta las respuestas: son los ejemplos que usarás toda la semana.
2. **Recorrido por el repositorio** (10'): un módulo por tema, mismo dominio (reservas de salas),
   enunciados en cada `README.md`, solución en `src/main` con comentarios `EJ n.m` y **tests que son el
   criterio de aceptación**.
3. **Regla de juego** (5'): *«El test en verde es el enunciado resuelto. Si tu código pasa el test,
   está bien aunque no se parezca al mío.»*

> **Cómo quieres que trabajen.** Recomendado: cada persona crea su proyecto en
> [start.spring.io](https://start.spring.io) siguiendo el enunciado y usa este repositorio para comparar.
> Alternativa más rápida y con menos fricción: clonan el repositorio, **borran las clases de solución**
> del módulo en curso y las reescriben con los tests como guía. Decide **antes** de empezar y anúncialo:
> cambiar de modelo a mitad de curso desordena mucho.

---

## 2. El ritmo de cada bloque

Todos los bloques de este guión siguen el mismo ciclo. Interiorízalo y no tendrás que mirar el reloj:

| Tiempo | Fase | Qué haces |
|---|---|---|
| ~5' | **Gancho** | Una pregunta o una demo que *crea la necesidad*. Nunca empieces por la solución. |
| 10–15' | **Concepto** | Pizarra + diapositiva. Máximo 15 minutos hablando seguido. |
| ~10' | **Demo** | Tú escribes código en vivo, o ejecutas un test y lo lees en voz alta. |
| 20–30' | **Ejercicio** | Ellos. Tú paseas (o miras las pantallas compartidas) y desatascas. |
| ~10' | **Puesta en común** | Una solución del aula proyectada, la de referencia, y las diferencias. |

Cuatro reglas que salvan la clase:

1. **Escribe en vivo sólo lo que enseña algo.** Las entidades JPA, los `pom.xml` y los YAML se dan hechos.
   Lo que se escribe delante es el aspecto, el listener, la `Specification`, la `SecurityFilterChain`.
2. **Rompe antes de arreglar.** Cada módulo tiene su «rotura provocada» marcada más abajo. El error en
   rojo en pantalla enseña más que tres diapositivas.
3. **Usa los tests como enunciado.** «Abrid `NMasUnoTest`, leed el nombre de los dos métodos y contadme
   qué creéis que va a pasar» es mejor apertura que cualquier explicación.
4. **Cuando algo tarda** (compilación, `docker pull`, arranque de Keycloak), **lanza una pregunta**.
   Tienes preguntas preparadas en cada módulo justo para esos huecos.

---

## 3. Mapa de sesiones

| Sesión | Contenido | Neto |
|---|---|---|
| **1** | Apertura y entorno (30') · **M1 · Núcleo y AOP** completo (3h45) | 4h15 |
| **2** | **Extra · XML vs anotaciones** (40') · **M2 · Boot** (2h45) · **M3 · EJ 3.1** (45') | 4h10 |
| **3** | **M3 · EJ 3.2–3.6** (3h) · **M4 · EJ 4.1** (1h15) | 4h15 |
| **4** | **M4 · EJ 4.2–4.4** (2h) · **M5 · EJ 5.1–5.2** (1h30) | 3h30 |
| **5** | **M5 · EJ 5.3–5.5** (1h30) · **M6 · Integration** completo (2h30) | 4h00 |
| **6** | **M7 · Security/OAuth2** completo (3h15) · Cierre e integrador (45') | 4h00 |

Dos criterios para ordenar así:

- **M1 el primer día completo.** Todo lo demás se apoya en proxies, perfiles y ciclo de vida. Si M1 queda
  flojo, M3 (transacciones) y M7 (`@PreAuthorize`) se vuelven magia negra.
- **El extra de XML al principio de la sesión 2**, en caliente tras M1, no al final del curso: su valor es
  contrastar con lo que acaban de ver y dar herramientas a quien mantiene aplicaciones heredadas.

---

## 4. Guiones por módulo

**El detalle paso a paso vive en cada módulo**, no aquí. Cada fichero lleva el cronograma minuto a minuto,
los comandos exactos, los ficheros y líneas que hay que proyectar, las roturas provocadas con su salida
esperada y cómo deshacerlas, las preguntas para el aula con su respuesta, los errores frecuentes y qué
recortar si vas con retraso.

| Módulo | Guión | Duración | Los dos momentos que no se recortan |
|---|---|---|---|
| **M1 · Núcleo y AOP** | [`m1-core-aop/GUION-CLASE.md`](../m1-core-aop/GUION-CLASE.md) | 3h45 | La auto-invocación (EJ 1.5) y la pista de auditoría (EJ 1.7) |
| **Extra · XML** | [`extra-xml-config/GUION-CLASE.md`](../extra-xml-config/GUION-CLASE.md) | 40' | Renombrar la clase: Java no compila, el XML falla al arrancar |
| **M2 · Boot** | [`m2-boot/GUION-CLASE.md`](../m2-boot/GUION-CLASE.md) | 2h45 | El arranque que falla por configuración inválida y el informe de autoconfiguración |
| **M3 · Persistencia** | [`m3-persistencia/GUION-CLASE.md`](../m3-persistencia/GUION-CLASE.md) | 3h45 | El N+1 medido (4 consultas frente a 1) y el `@Transactional` que se quita |
| **M4 · Web y REST** | [`m4-web-rest-jsp/GUION-CLASE.md`](../m4-web-rest-jsp/GUION-CLASE.md) | 3h15 | El F5 que duplica la sala (Post/Redirect/Get) y los `ProblemDetail` |
| **M5 · RabbitMQ** | [`m5-amqp/GUION-CLASE.md`](../m5-amqp/GUION-CLASE.md) | 3h00 | El mensaje que acaba en `reservas.errores` y la idempotencia |
| **M6 · Integration** | [`m6-integration/GUION-CLASE.md`](../m6-integration/GUION-CLASE.md) | 2h30 | Las cabeceras de secuencia del agregador y el `errorChannel` |
| **M7 · Security** | [`m7-security-oauth2/GUION-CLASE.md`](../m7-security-oauth2/GUION-CLASE.md) | 3h15 | El `403` con token válido (conversor de roles) y URL frente a método |

**Todos los guiones siguen la misma estructura**, así que basta con aprenderse una:

1. **Paso 0 · Antes de entrar en el aula:** qué ejecutar el día anterior, qué imágenes de Docker descargar y
   qué pestañas dejar abiertas en el IDE, en orden de uso.
2. **Cronograma:** tabla de pasos con minutos y acumulado.
3. **Pasos numerados:** cada uno con su objetivo, sus comandos, lo que se proyecta y lo que se dice.
4. **Anexo A · Si vas con retraso** y **Anexo B · Errores frecuentes**.

**Iconos que usan todos los guiones:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así ·
**❓** pregunta al aula (con la respuesta que buscas) · **🔴** rotura provocada · **↩️** cómo deshacerla.

> **Las salidas que aparecen en los guiones son reales**: están copiadas de una ejecución de este
> repositorio, no escritas de memoria. Si en tu máquina sale otra cosa, revisa la versión del JDK o el
> estado de Docker antes de dar la clase.

### El hilo conductor, por si solo lees esta página

- **La pizarra del proxy se dibuja en M1 y no se borra.** Vuelve en M3 (`@Transactional`), en M4 (aparece en
  una traza de error como `SpringCGLIB`) y en M7 (`@PreAuthorize`). El último día se cierra el círculo:
  **es un solo mecanismo**, no cinco cosas distintas.
- **El mismo dominio atraviesa los siete módulos.** Una reserva entra por el controlador (M4), se valida con
  la configuración (M2), se guarda en una transacción (M3), publica un evento (M1) que sale al broker (M5),
  lo procesa un flujo de integración (M6), y todo ello detrás de una cadena de filtros (M7).
- **El reintento aparece tres veces** (M1 con `@Retryable`, M5 en el listener, M6 en el *advice* HTTP).
  Nómbralo cada vez: la repetición deliberada es lo que fija el concepto.

---

## 5. Cuando algo falla en clase

| Situación | Qué haces |
|---|---|
| **Alguien no tiene Docker** (política de empresa, portátil ajeno) | No es bloqueante: los tests con Testcontainers se omiten solos (`@Testcontainers(disabledWithoutDocker = true)`) y el resto funciona con H2. Lo pierde en M3 (EJ 3.6), M5 (EJ 5.5) y M7. Emparéjalo con alguien que sí lo tenga y haz tú las demos de esos bloques en pantalla. |
| **El wifi va mal el primer día** | Por eso se pide `./mvnw verify` desde casa. Lleva un repositorio local de Maven (`~/.m2`) en un USB o comparte tu carpeta: desatasca a 15 personas en 5 minutos. |
| **Keycloak no arranca o el realm no importa** | `docker compose down -v` y arriba otra vez: casi siempre es un volumen a medio escribir. Mientras tanto, M7 puede darse entero contra los tests (`ApiSeguridadTest`, `WebSeguridadTest`), que no necesitan el contenedor. |
| **No tienes credenciales de Google/GitHub** | Da EJ 7.1 como demo grabada o comentada y céntrate en EJ 7.2. El provisionador se prueba igual con `ProvisionadorUsuariosTest`. |
| **Un puerto ocupado (8080, 5432, 5672, 8180)** | Suele ser una demo anterior sin parar. `docker compose down` del módulo previo al terminar cada bloque: acostúmbrate y evitarás el 90 % de los sustos. |
| **Alguien va muy por delante** | Tienes material: el apartado «Extra Spring Boot 4» de cada módulo y los ejercicios propuestos de `extra-xml-config`. Que lo intente y lo cuente en la puesta en común. |
| **Alguien va muy por detrás** | Dale la solución de referencia del ejercicio en curso y que siga desde ahí. Perder el hilo del módulo cuesta mucho más que saltarse un ejercicio. |

---

## 6. Evaluación y seguimiento

No hace falta examen: **el criterio de aceptación es el test**. Al cerrar cada módulo, proyecta el comando y
que cada persona lo ejecute en su proyecto:

```bash
./mvnw -pl m1-core-aop test        # y así con cada módulo
```

Checklist de mínimos por módulo (si esto está en verde, el módulo está dado):

| Módulo | Mínimo exigible |
|---|---|
| M1 | `PerfilSmsTest`, `PedidoServiceUnitTest`, `PedidoServiceTest#autoInvocacionNoPasaPorElProxy` |
| M2 | `ReservasPropertiesTest`, `RelojAutoConfigurationTest` |
| M3 | `RepositoriosTest`, `NMasUnoTest`, `ReservaServiceTest#reservarVariasEsTodoONada` |
| M4 | `SalaWebControllerTest`, `ReservaRestControllerTest` |
| M5 | `TopologiaTest`, `FacturacionListenerTest#ignoraDuplicados` |
| M6 | `TransformacionesTest`, `FlujosReservasTest#divideEnrutaTarificaYAgrega` |
| M7 | `ApiSeguridadTest`, `SeguridadMetodosTest` |

**Termómetro de 30 segundos** al final de cada sesión (mejor que preguntar «¿alguna duda?», que nunca
funciona): «Escribid en el chat una cosa que os haya quedado clara y una que no.» Las segundas son el guión de
los primeros 10 minutos del día siguiente.

---

## 7. Cierre del curso — 45'

1. **Recorrido por la pizarra del proxy** (10'). Vuelve al dibujo del primer día y enumera todo lo que ha
   pasado por ahí durante la semana: `@Auditado`, `@Transactional`, `@Retryable`, `@PreAuthorize`. Cierra el
   círculo: es **un solo mecanismo**, no cinco cosas distintas.
2. **El dominio completo** (10'). Recorre el camino de una reserva atravesando los siete módulos: entra por el
   controlador (M4), se valida con la configuración (M2), se persiste en una transacción (M3), publica un
   evento (M1) que sale al broker (M5), lo procesa un flujo de integración (M6) y todo ello detrás de una
   cadena de filtros (M7). Es la diapositiva que justifica que el curso use un único dominio.
3. **Propuesta de trabajo posterior** (15'): la práctica de migración de [`docs/MIGRACION-BOOT4.md`](MIGRACION-BOOT4.md).
   Crear la rama `boot4`, pasar un módulo, ejecutar `verify` y comparar. Es el mejor ejercicio de consolidación
   porque obliga a releer todo el código con otros ojos.
4. **Qué mirar a continuación** (10'): documentación oficial de Spring frente a las respuestas de hace ocho
   años en foros; las notas de versión; y el consejo práctico: **fijar la versión de Boot y leer la guía de
   migración antes de subir de mayor**, no después.

---

## 8. Si vas con retraso: qué se recorta

Recortes ordenados **a nivel de curso**: aplica de arriba abajo hasta recuperar el tiempo. Nunca recortes al
revés. Para recortar **dentro** de una clase que ya ha empezado, cada guión de módulo tiene su propio
«Anexo A · Si vas con retraso» con los recortes de ese día y lo que no se toca.

| Orden | Recorte | Ganas | Coste |
|---|---|---|---|
| 1 | `extra-xml-config` completo (o lo mandas leído a casa) | 40' | Ninguno si nadie mantiene aplicaciones heredadas. Pregúntalo el primer día. |
| 2 | M2 · EJ 2.5 (hilos virtuales e imagen OCI) como comentario de 3' | 12' | Bajo: es demostración, no ejercicio. |
| 3 | M4 · EJ 4.3 (springdoc y paginación) reducido a demo | 20' | Medio: enseña Swagger UI funcionando y deja el ejercicio para casa. |
| 4 | M6 · EJ 6.1 (*poller* JDBC) como demo, sin ejercicio | 20' | Medio: el *gateway* es lo importante del bloque. |
| 5 | M5 · EJ 5.4 (*publisher confirms*) como demo | 15' | Medio: concepto importante, pero se entiende viéndolo. |
| 6 | M1 · EJ 1.6 (reintentos) solo mencionado | 10' | Bajo: reaparece en M6 con el *advice* de reintento. |
| 7 | M3 · EJ 3.6 (Testcontainers) como demo tuya | 15' | Medio, y si hay problemas de Docker en el aula es gratis. |
| | **Total recortable** | **≈ 2h10** | |

**Lo que no se recorta nunca**, porque es donde está el valor del curso: EJ 1.5 (auto-invocación),
EJ 3.4 (N+1), EJ 3.5 (transacciones), EJ 4.2 (errores de la API), EJ 5.3 (errores y reintentos) y
EJ 7.2 (las dos cadenas y los roles). Si solo te quedan seis bloques, son esos seis.

---

## 9. Chuleta de comandos para proyectar

```bash
# Infraestructura (levántala antes de empezar el módulo, no durante)
docker compose -f m2-boot/compose.yaml up -d              # PostgreSQL
docker compose -f m5-amqp/compose.yaml up -d              # RabbitMQ  → :15672 (guest/guest)
docker compose -f m7-security-oauth2/compose.yaml up -d   # Keycloak  → :8180 (admin/admin)
docker compose -f <módulo>/compose.yaml down              # y bájala al terminar

# Ejecutar y probar
./mvnw -pl m1-core-aop spring-boot:run -Dspring-boot.run.profiles=sms
./mvnw -pl m3-persistencia test
./mvnw -pl m3-persistencia test -Dtest=NMasUnoTest        # un solo test en pantalla
./mvnw verify                                             # todo, antes de cada sesión

# Trucos de demostración
./mvnw -pl m2-boot spring-boot:run -Dspring-boot.run.arguments=--debug   # informe de autoconfiguración
```

> En Windows, `mvnw.cmd` en lugar de `./mvnw`. Si en el aula hay máquinas de los dos tipos, proyecta siempre
> las dos formas la primera vez y luego usa la tuya.
