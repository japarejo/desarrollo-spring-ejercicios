# Extra · Configuración XML frente a anotaciones

**Objetivo:** ver el **mismo grafo de objetos** ensamblado de tres formas distintas —XML, `@Configuration`/`@Bean` y una mezcla de ambas— para entender qué cambia realmente entre los estilos de configuración de Spring.

> Este módulo **no corresponde a ningún tema numerado del curso**. Es material de apoyo para el módulo 1 (diapositiva «¿Y la configuración XML?»), pensado para quien se encuentre aplicaciones heredadas.

```bash
./mvnw -pl extra-xml-config test                       # las tres configuraciones, comparadas
./mvnw -pl extra-xml-config exec:java \
  -Dexec.mainClass=com.atech.curso.xml.demo.DemoXml    # el contenedor SIN Spring Boot
./mvnw -pl extra-xml-config spring-boot:run            # Boot + @ImportResource (híbrido)
```

## La idea

El paquete `dominio`, `tarifas` y `reservas` no contiene **ni una sola anotación de Spring**: son POJOs. Toda la configuración vive fuera:

| | Fichero |
|---|---|
| XML puro | [`src/main/resources/beans.xml`](src/main/resources/beans.xml) |
| Java | [`ConfiguracionJava.java`](src/main/java/com/atech/curso/xml/configuracionjava/ConfiguracionJava.java) |
| Híbrido (migración) | [`AplicacionHibrida.java`](src/main/java/com/atech/curso/xml/boot/AplicacionHibrida.java) |

Los dos primeros declaran **los mismos beans con los mismos nombres**. `EquivalenciaTest` lo comprueba automáticamente: mismos nombres de bean y mismo presupuesto calculado.

## Equivalencias, una a una

| Concepto | XML | Java |
|---|---|---|
| Declarar un bean | `<bean id="x" class="C"/>` | `@Bean C x()` |
| Inyección por constructor | `<constructor-arg ref="y"/>` | parámetro del método `@Bean` |
| Inyección por *setter* | `<property name="p" value="5"/>` | llamar al *setter* dentro del `@Bean` |
| Desambiguar | `primary="true"` / `ref="nombre"` | `@Primary` / `@Qualifier` |
| Ámbito | `scope="prototype"` | `@Scope("prototype")` |
| Ciclo de vida | `init-method` / `destroy-method` | `@Bean(initMethod=..., destroyMethod=...)` |
| Clase de terceros sin constructor | `factory-method="systemUTC"` | `return Clock.systemUTC();` |
| Lista de beans | `<list><bean .../></list>` | `List.of(new ..., new ...)` |
| Mezclar ambos | — | `@ImportResource("classpath:beans.xml")` |

## Qué observar

1. **El código de negocio es idéntico.** `ServicioReservas` no cambia una línea entre las tres configuraciones. El estilo de configuración es un detalle del ensamblado, no del diseño.
2. **Cuándo falla cada uno.** Renombra `TarifaPorHora` o cámbiala de paquete: la configuración Java deja de compilar al instante; el XML compila igual y **falla al arrancar**, porque el nombre de la clase es una cadena de texto.
3. **Lo que añade el contenedor de anotaciones.** `DemoXml` imprime los beans del contexto XML: salen exactamente los 6 declarados. El contexto de anotaciones registra además sus propios post-procesadores (`internalConfigurationAnnotationProcessor`, etc.), que son los que hacen funcionar `@Autowired` y compañía.
4. **`@ImportResource` es la vía de migración.** En `AplicacionHibrida`, `InformeArranque` es un `@Component` que recibe por constructor un bean declarado en XML. Al cliente le da exactamente igual de dónde viene.
5. **Por qué el XML cayó en desuso.** Su gran ventaja era recablear sin recompilar; hoy eso se cubre mejor con perfiles, `@ConditionalOn...` y configuración externalizada (módulo 2), sin renunciar a la comprobación de tipos.

## Ejercicios propuestos

1. Cambia la tarifa que usa `servicioReservas` de `tarifaPorHora` a `tarifaPlana`. Hazlo **en el XML** y luego **en Java**. ¿Cuál te ha dado más confianza?
2. Añade una cuarta sala en ambas configuraciones y comprueba que `EquivalenciaTest` sigue en verde.
3. Quita `primary="true"` del XML y observa el mensaje de error: ¿en qué momento aparece, al compilar o al arrancar?
4. Añade `<context:annotation-config/>` al XML y vuelve a listar los beans con `DemoXml`. ¿Qué ha cambiado?
5. Traduce `beans.xml` entero a un segundo `@Configuration` **sin mirar** `ConfiguracionJava`, y compáralos después.

## Extra Spring Boot 4

- Spring Framework 7 **depreca varios espacios de nombres XML** (por ejemplo `<mvc:*>`). El `<beans>` básico que se usa aquí sigue soportado, pero la dirección del proyecto es clara: la configuración XML es legado.
- `@ImportResource` **sigue existiendo** en Spring Framework 7, así que la estrategia de migración por partes continúa siendo válida.
- Al migrar, aprovecha para sustituir los `<bean>` de infraestructura por *starters* y autoconfiguración: casi siempre el XML heredado declara a mano cosas que hoy Boot ya configura solo.
