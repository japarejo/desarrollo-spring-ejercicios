# Guión de clase · Extra · Configuración XML frente a anotaciones

**Duración:** 40 min netos · **Principio de la sesión 2**, justo después del módulo 1 · Enunciados en [README.md](README.md)

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.
>
> **Para qué sirve este bloque.** No es un tema del temario numerado: es munición para quien mantiene
> aplicaciones heredadas. **Pregunta al aula el primer día** cuántos tienen `beans.xml` en producción; si no
> lo tiene nadie, recorta a 15 minutos (solo los pasos 2 y 4) o mándalo como lectura.
>
> **Por qué aquí y no al final del curso.** Su valor está en el contraste con el módulo 1, que acaban de
> terminar. Dado al final, es arqueología; dado ahora, es una comparación.

---

## Paso 0 · Antes de entrar en el aula (5 min)

```bash
./mvnw -pl extra-xml-config test
./mvnw -q -pl extra-xml-config exec:java -Dexec.mainClass=com.atech.curso.xml.demo.DemoXml
```

Ten abiertos, **en vista dividida, uno al lado del otro** (es todo el bloque):

- [`src/main/resources/beans.xml`](src/main/resources/beans.xml)
- [`src/main/java/com/atech/curso/xml/configuracionjava/ConfiguracionJava.java`](src/main/java/com/atech/curso/xml/configuracionjava/ConfiguracionJava.java)

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | Gancho y el contenedor sin Boot | 10 | 0:10 |
| 2 | La tabla de equivalencias | 10 | 0:20 |
| 3 | 🔴 Cuándo falla cada uno | 10 | 0:30 |
| 4 | `@ImportResource`: migrar por partes | 10 | 0:40 |

---

## Paso 1 · Gancho y el contenedor sin Boot · 10 min

1. **❓ Empieza preguntando:** «¿Cuántos mantenéis alguna aplicación con `beans.xml`?» Suele levantar la
   mano media clase, y eso marca el tono del bloque.

2. **⌨️ Ejecuta el contenedor **sin Spring Boot**:**

   ```bash
   ./mvnw -q -pl extra-xml-config exec:java -Dexec.mainClass=com.atech.curso.xml.demo.DemoXml
   ```

   **Salida esperada:**

   ```
   Beans declarados: reloj, repositorioSalas, tarifaPorHora, tarifaPlana, servicioReservas, borradorReserva
   Presupuesto: Lovelace (S-2) 3 h -> 75.00 EUR [tarifa POR_HORA]
   ```

3. **✏️ Proyecta [`DemoXml.java` líneas 22-32](src/main/java/com/atech/curso/xml/demo/DemoXml.java#L22-L32):**

   ```java
   try (ClassPathXmlApplicationContext contexto = new ClassPathXmlApplicationContext("beans.xml")) {
       ServicioReservas servicio = contexto.getBean(ServicioReservas.class);
   ```

   **🗣️ Di:** «Esto es todo lo que era Spring antes de Boot: **un objeto** que lee una configuración, crea
   los beans y los conecta. `SpringApplication.run(...)` hace esto mismo y, además, mira el classpath y
   aplica la autoconfiguración. Ver el contenedor desnudo una vez ayuda a que Boot deje de parecer magia.»

4. **✏️ Señala los seis nombres de la salida y compáralos con
   [`beans.xml`](src/main/resources/beans.xml):** salen **exactamente los seis declarados**, ni uno más.

   **🗣️ Guárdate esto para el paso 4:** «Recordad este número: seis. Volveremos a él.»

5. **🗣️ El dato que sitúa el módulo:** «El paquete de negocio —`dominio`, `tarifas`, `reservas`— **no tiene
   ni una anotación de Spring**. Son POJOs. Toda la configuración vive fuera. Eso, que hoy suena a
   restricción, era el argumento de venta original de Spring frente a los EJB.»

---

## Paso 2 · La tabla de equivalencias · 10 min

1. **✏️ Con los dos ficheros en vista dividida**, recorre la tabla del [README](README.md#equivalencias-una-a-una)
   señalando en pantalla cada pareja. Ve rápido, 30 segundos por fila:

   | Concepto | XML | Java |
   |---|---|---|
   | Declarar un bean | `<bean id="x" class="C"/>` | `@Bean C x()` |
   | Inyección por constructor | `<constructor-arg ref="y"/>` | parámetro del método `@Bean` |
   | Inyección por *setter* | `<property name="p" value="5"/>` | llamar al *setter* dentro del `@Bean` |
   | Desambiguar | `primary="true"` / `ref="nombre"` | `@Primary` / `@Qualifier` |
   | Ámbito | `scope="prototype"` | `@Scope("prototype")` |
   | Ciclo de vida | `init-method` / `destroy-method` | `@Bean(initMethod=..., destroyMethod=...)` |
   | Clase de terceros | `factory-method="systemUTC"` | `return Clock.systemUTC();` |

2. **✏️ Detente en dos filas, que son las que enseñan algo del módulo 1:**
   - **`primary="true"`** ([`beans.xml` línea 47](src/main/resources/beans.xml#L47)): «Es exactamente el
     `@Primary` que quitamos ayer en `SinDescuento`. Mismo mecanismo, otra sintaxis.»
   - **`factory-method`** ([línea 18](src/main/resources/beans.xml#L18)): «Para una clase de terceros sin
     constructor público. Es el caso del `Clock`, que ya conocéis.»

3. **⌨️ Y la prueba de que son equivalentes:**

   ```bash
   ./mvnw -pl extra-xml-config test -Dtest=EquivalenciaTest
   ```

   **✏️ Proyecta el test**: los dos contextos declaran **los mismos beans con los mismos nombres** y
   producen **el mismo presupuesto**.

   **🗣️ La moraleja, dicha tal cual:** «`ServicioReservas` no cambia **ni una línea** entre las tres
   configuraciones. El estilo de configuración es un detalle del ensamblado, **no del diseño**. Quien os
   diga que migrar de XML a anotaciones "mejora la arquitectura" os está vendiendo algo.»

---

## Paso 3 · 🔴 Cuándo falla cada uno · 10 min

**Este es el argumento real del bloque. Hazlo en directo.**

1. **✏️ Renombra la clase `TarifaPorHora`** (con el refactor del IDE: botón derecho → *Rename* →
   `TarifaPorHoras`). El IDE actualizará `ConfiguracionJava.java`… **y no tocará el `beans.xml`**.

2. **⌨️ Compila:**

   ```bash
   ./mvnw -q -pl extra-xml-config compile
   ```

   → **Compila sin problemas.**

3. **⌨️ Ahora arranca:**

   ```bash
   ./mvnw -q -pl extra-xml-config exec:java -Dexec.mainClass=com.atech.curso.xml.demo.DemoXml
   ```

   → Falla al arrancar: no encuentra la clase `com.atech.curso.xml.tarifas.TarifaPorHora`.

4. **🗣️ Di:** «Ese es el argumento, y no la estética. En la configuración Java, el nombre de la clase **es
   un tipo**: si lo cambiáis, el compilador os para en el sitio. En el XML **es una cadena de texto**: el
   refactor no la ve, el compilador no la mira, y os enteráis al arrancar. En un microservicio pequeño, eso
   son dos minutos; en una aplicación de mil beans que tarda cinco minutos en levantar, es una tarde.»

   **↩️ Deshaz** con el *Undo* del IDE (`Ctrl+Z`), que revierte el refactor completo, o vuelve a renombrar
   la clase a `TarifaPorHora`.

5. **❓ Y la pregunta de la otra dirección, para no hacer propaganda:** «¿Qué ganaba el XML?» → **Recablear
   sin recompilar.** Cambiar una implementación editando un fichero de texto en el servidor, sin pasar por
   el ciclo de compilación y despliegue. En 2005, con despliegues manuales de una hora, eso valía mucho.

   **🗣️ Y remata enlazando con el módulo que viene ahora:** «¿Qué lo sustituye hoy? Perfiles,
   `@ConditionalOn...` y configuración externalizada, **sin renunciar a la comprobación de tipos**. Es decir:
   el módulo 2, que empezamos en cuanto terminemos esto.»

---

## Paso 4 · `@ImportResource`: migrar por partes · 10 min

1. **✏️ Proyecta
   [`AplicacionHibrida.java`](src/main/java/com/atech/curso/xml/boot/AplicacionHibrida.java):** una
   aplicación Boot normal con `@ImportResource("classpath:beans.xml")`.

2. **✏️ Y [`InformeArranque.java`](src/main/java/com/atech/curso/xml/boot/InformeArranque.java):** un
   `@Component` que recibe **por constructor un bean declarado en el XML**.

   **🗣️ Di:** «Al cliente le da exactamente igual de dónde viene su dependencia. Son el mismo contenedor y
   el mismo grafo de objetos. Por eso **no hace falta un *big bang***: se puede migrar módulo a módulo, o
   incluso bean a bean, con la aplicación funcionando todo el tiempo.»

3. **⌨️ Compruébalo:**

   ```bash
   ./mvnw -pl extra-xml-config test -Dtest=ImportResourceTest
   ```

4. **🗣️ Vuelve a los seis beans del paso 1** (esto cierra el bloque y enlaza con el módulo 2): «En el
   contexto XML salían seis beans, los seis declarados. En un contexto de anotaciones aparecen además
   `internalConfigurationAnnotationProcessor` y compañía: **los post-procesadores que hacen funcionar
   `@Autowired`, `@Value` y las demás anotaciones**. No son magia: son beans de infraestructura que alguien
   tiene que registrar. En el XML se registraban a mano con `<context:annotation-config/>`.»

5. **🗣️ El consejo práctico para quien va a migrar de verdad** (es lo que se llevan al trabajo):
   «Al migrar, no traduzcáis el XML línea por línea. **La mitad de ese XML declara a mano cosas que hoy Boot
   configura solo**: `DataSource`, `EntityManagerFactory`, `TransactionManager`, conversores… Cambiadlo por
   *starters* y quedaos solo con lo que es vuestro. Un `beans.xml` de 800 líneas suele acabar en unas
   cuantas clases `@Configuration` muy cortas.»

6. **🗣️ Y el estado actual, en una frase:** «En Spring Framework 7 se deprecan varios espacios de nombres
   XML (`<mvc:*>`, por ejemplo), pero el `<beans>` básico y `@ImportResource` **siguen funcionando**. La
   dirección del proyecto es clara, pero no hay prisa forzada.»

---

## Anexo · Ejercicios para casa (del README)

Si alguien quiere seguir, los cinco ejercicios propuestos del [README](README.md#ejercicios-propuestos)
son buen material de repaso. Los dos que más enseñan:

- **Quitar `primary="true"`** del XML y ver **cuándo** aparece el error (al arrancar, no al compilar): es el
  paso 3 desde otro ángulo.
- **Traducir `beans.xml` a un segundo `@Configuration` sin mirar `ConfiguracionJava`** y comparar después.

## Errores frecuentes

| Síntoma | Causa |
|---|---|
| `BeanDefinitionStoreException` al arrancar | Nombre de clase mal escrito o movido de paquete en el XML |
| `NoUniqueBeanDefinitionException` | Falta `primary="true"` o la referencia explícita por `ref` |
| El `init-method` no se ejecuta | El nombre del método en el XML no coincide (otra vez, una cadena de texto) |
| `@Autowired` no funciona en un contexto XML puro | Falta `<context:annotation-config/>` |
