# Guión de clase · Módulo 7 · Spring Security, OAuth 2.0 y OpenID Connect

**Duración:** 3 h 15 min netas · **Sesión 6** del curso · Enunciados en [README.md](README.md)

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.
>
> ## ⚠️ Lo primero de la sesión, antes de saludar
>
> ```bash
> docker compose -f m7-security-oauth2/compose.yaml up -d
> ```
>
> Keycloak tarda en arrancar e importar el realm `atech`. Hasta que no esté, **no hay demo posible**.
> Mientras arranca, haz las presentaciones o repasa el módulo anterior.
>
> Comprueba que está listo:
>
> ```bash
> curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8180/realms/atech
> ```
>
> Cuando devuelva `200`, ya puedes empezar.

---

## Paso 0 · Antes de entrar en el aula (15 min, el día anterior)

```bash
docker pull quay.io/keycloak/keycloak:26.3
docker compose -f m7-security-oauth2/compose.yaml up -d
./mvnw -pl m7-security-oauth2 test        # 4 clases en verde, sin necesidad de Keycloak
```

**Prepara las credenciales de GitHub** (tuyas, para la demo del paso 4):

1. GitHub → *Settings* → *Developer settings* → *OAuth Apps* → *New OAuth App*.
2. **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github` (exacta, sin barra final).
3. Exporta las variables antes de arrancar:

   ```bash
   export GITHUB_CLIENT_ID=...
   export GITHUB_CLIENT_SECRET=...
   ```

> **Plan B si no tienes credenciales o no hay internet:** el paso 4 se cuenta con el diagrama y se prueba
> con `ProvisionadorUsuariosTest`; los pasos 5 y 7 **no necesitan ni Keycloak ni internet**. Dilo en clase
> sin dramatismo y sigue.

Pestañas del IDE:

1. [`src/main/java/com/atech/curso/m7/config/SecurityConfig.java`](src/main/java/com/atech/curso/m7/config/SecurityConfig.java) ← la clase central
2. [`src/main/java/com/atech/curso/m7/config/RolesJwtConverter.java`](src/main/java/com/atech/curso/m7/config/RolesJwtConverter.java)
3. [`src/main/java/com/atech/curso/m7/api/ReservaService.java`](src/main/java/com/atech/curso/m7/api/ReservaService.java)
4. [`src/main/java/com/atech/curso/m7/usuarios/ProvisionadorUsuarios.java`](src/main/java/com/atech/curso/m7/usuarios/ProvisionadorUsuarios.java)
5. [`src/test/java/com/atech/curso/m7/ApiSeguridadTest.java`](src/test/java/com/atech/curso/m7/ApiSeguridadTest.java)

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | 🔴 Gancho: 401, 200 y 403 en tres comandos | 10 | 0:10 |
| 2 | Concepto: la cadena de filtros y las dos cadenas | 20 | 0:30 |
| 3 | Concepto: OAuth 2.0, OIDC y el token por dentro | 20 | 0:50 |
| 4 | Login social y alta automática (EJ 7.1) | 40 | 1:30 |
| 5 | Resource Server, roles y seguridad de método (EJ 7.2) | 50 | 2:20 |
| 6 | Consumir una API protegida (EJ 7.3) | 25 | 2:45 |
| 7 | Pruebas de seguridad (EJ 7.4) | 20 | 3:05 |
| 8 | Cierre del módulo y del curso | 10 | 3:15 |

---

## Paso 1 · 🔴 Gancho: 401, 200 y 403 en tres comandos · 10 min

1. **⌨️ Arranca la aplicación** (déjala corriendo todo el módulo):

   ```bash
   ./mvnw -q -pl m7-security-oauth2 spring-boot:run
   ```

2. **⌨️ Primer comando: sin token.**

   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/reservas
   ```

   → `401`

3. **⌨️ Segundo: pide un token a Keycloak y úsalo.**

   ```bash
   TOKEN=$(curl -s -d grant_type=password -d client_id=reservas-cli -d username=admin -d password=admin \
     http://localhost:8180/realms/atech/protocol/openid-connect/token | jq -r .access_token)

   curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/me
   ```

   → Los datos del usuario y **sus autoridades**.

   > Si no tienes `jq`, usa `grep -o '"access_token":"[^"]*' | cut -d'"' -f4`.

4. **⌨️ Tercero: el mismo comando con el token de `ana`, borrando una reserva que no es suya.**

   ```bash
   TOKEN_ANA=$(curl -s -d grant_type=password -d client_id=reservas-cli -d username=ana -d password=ana \
     http://localhost:8180/realms/atech/protocol/openid-connect/token | jq -r .access_token)

   curl -s -o /dev/null -w "%{http_code}\n" -X DELETE -H "Authorization: Bearer $TOKEN_ANA" \
     localhost:8080/api/reservas/2
   ```

   → `403`

5. **🗣️ Cierra el gancho señalando los tres códigos:** «`401`: no sé quién eres. `200`: sé quién eres y
   puedes. `403`: sé perfectamente quién eres, **y no puedes**. Esas dos preguntas —quién eres y qué puedes
   hacer— son autenticación y autorización, y son el módulo de hoy. Fijaos en que la última no depende del
   rol, sino de **de quién es la reserva**: eso no se resuelve con una regla de URL.»

---

## Paso 2 · Concepto: la cadena de filtros y las dos cadenas · 20 min

1. **Dibuja esto en la pizarra y déjalo toda la sesión:**

   ```
   petición ─► [ cadena de filtros ] ─► ¿autenticado? ─► ¿autorizado? ─► controlador ─► @PreAuthorize
                @Order(1)  /api/**  : JWT · sin estado · sin CSRF
                @Order(2)  el resto : oauth2Login · sesión HTTP · CSRF
   ```

2. **🗣️ La idea:** «Spring Security es **una cadena de filtros de servlet** que se ejecuta antes que vuestro
   controlador. No es magia y no está dentro de vuestro código: está delante.»

3. **✏️ Proyecta [`SecurityConfig.java`](src/main/java/com/atech/curso/m7/config/SecurityConfig.java)** y
   recorre las dos cadenas señalando **por qué son dos y no una**:

   | Líneas | Qué señalar |
   |---|---|
   | 32-35 | `@Order(1)` + `securityMatcher("/api/**")`: esta cadena **solo** atiende a la API. |
   | 41 | `SessionCreationPolicy.STATELESS`: la API no crea sesión. Cada petición trae su token. |
   | 43 | `csrf.disable()`: sin cookie de sesión, CSRF no aplica. |
   | 47-53 | `@Order(2)`: la cadena web, con sesión, CSRF activo y reglas por URL. |
   | 52 | `.requestMatchers("/admin/**").hasRole("ADMIN")`: autorización **por URL**. |

4. **🗣️ El orden importa, y mucho:** «Se evalúan por `@Order` y **gana la primera que encaja**. Si la web
   fuera la primera, capturaría también `/api/**` y vuestras peticiones con token acabarían redirigidas al
   formulario de login.» (Lo vais a ver en directo en el paso 5.)

5. **❓ Pregunta:** «¿Por qué no una sola cadena con reglas para todo?» → Porque las dos mitades tienen
   **modelos distintos**: una guarda sesión en una cookie y necesita CSRF; la otra no guarda nada y se
   identifica en cada petición. Mezclarlas obliga a poner excepciones por todas partes.

---

## Paso 3 · Concepto: OAuth 2.0, OIDC y el token por dentro · 20 min

1. **Dibuja el *authorization code flow*** con los tres papeles bien separados:

   ```
   [usuario] ──1. quiero entrar──► [nuestra app = CLIENTE]
        ▲                                │ 2. te redirijo al proveedor
        │ 3. me identifico               ▼
   [ SERVIDOR DE AUTORIZACIÓN: Google / GitHub / Keycloak ]
        │ 4. code ──► app  ──5. code+secret──► token
        ▼
   [nuestra API = SERVIDOR DE RECURSOS] ◄──6. Bearer token──
   ```

2. **🗣️ La distinción que más se confunde, dicha explícitamente:**
   - **OAuth 2.0 es autorización de acceso:** «te dejo usar esta API en nombre de alguien».
   - **OpenID Connect es identidad, montada encima:** añade el `id_token` y el endpoint `userinfo`, que
     responden a «**quién** eres».
   - «Google es OIDC; GitHub es OAuth 2.0 puro. Por eso en el código hay **dos servicios de usuario
     distintos**, como veréis en el paso siguiente.»

3. **⌨️ Ahora abre el token en canal.** Con el `$TOKEN` del paso 1:

   ```bash
   echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq .
   ```

   **✏️ Proyecta los *claims* y señálalos uno a uno:** `iss` (quién lo emitió), `exp` (cuándo caduca),
   `preferred_username`, `realm_access.roles`.

4. **🗣️ Las dos frases que hay que decir aquí, sin falta:**
   - «Un JWT **está firmado, no cifrado**. Cualquiera que lo intercepte lo lee. **Nada de datos sensibles
     dentro.**»
   - «La firma es lo que hace que nuestra API pueda confiar en él **sin llamar a Keycloak en cada
     petición**: se valida con la clave pública del emisor. Por eso `issuer-uri` es toda la configuración
     que hace falta (ver [`application.yml` líneas 21-24](src/main/resources/application.yml#L21-L24)).»

5. **❓ Pregunta:** «¿Y si hay que revocar el acceso de alguien ahora mismo?» → Con JWT autocontenidos, hasta
   que caduque, no hay forma directa: por eso los *access token* duran minutos y existen los *refresh
   token*. Es el precio de no preguntar al emisor en cada petición.

---

## Paso 4 · Login social y alta automática (EJ 7.1) · 40 min

1. **✏️ La configuración, y lo que NO hay que hacer**
   ([`application.yml` líneas 8-20](src/main/resources/application.yml#L8-L20)):

   ```yaml
   github:
     client-id: ${GITHUB_CLIENT_ID:configurar-github-client-id}
     client-secret: ${GITHUB_CLIENT_SECRET:configurar}
   ```

   **🗣️ Di:** «Variables de entorno, **nunca** el secreto en el repositorio. Y fijaos en que no hay ni una
   URL de Google o GitHub: Boot conoce los proveedores comunes. Para uno propio, basta el `issuer-uri` y el
   resto se descubre solo.»

2. **Demo en vivo (10 min).** Abre <http://localhost:8080/perfil> en una ventana de incógnito → te redirige
   al login → entras con GitHub → vuelves a `/perfil` ya identificado.

   **✏️ Mientras ocurre, ve narrando la barra de direcciones:** el `client_id` y el `redirect_uri` en la ida,
   el `?code=...` en la vuelta. «Ese `code` no sirve de nada por sí solo: la aplicación lo cambia por el
   token **por detrás**, con su secreto. Por eso el secreto nunca viaja al navegador.»

3. **🗣️ Aviso antes de que les pase:** «El error número uno de este bloque es `redirect_uri_mismatch`. La
   URL registrada en GitHub tiene que ser **exactamente**
   `http://localhost:8080/login/oauth2/code/github`. Ni `https`, ni `127.0.0.1`, ni barra final.»

4. **✏️ El alta automática**
   ([`ProvisionadorUsuarios.java`](src/main/java/com/atech/curso/m7/usuarios/ProvisionadorUsuarios.java)):

   ```java
   return usuarios.findByProveedorAndIdExterno(proveedor, idExterno)
           .map(existente -> { existente.registrarAcceso(); return existente; })
           .orElseGet(() -> usuarios.save(new UsuarioLocal(..., administradores.contains(email) ? "ADMIN" : "USER")));
   ```

   **❓ Pregunta:** «Si Google ya sabe quién es, ¿para qué guardamos nada?» → Porque la **identidad** es de
   Google, pero los **datos de negocio** (sus reservas, sus permisos, sus preferencias) son nuestros y
   necesitan una fila. Es el patrón *just-in-time provisioning*.

   **🗣️ Y el detalle fino:** «La clave es `(proveedor, idExterno)`, no el correo. Un correo se puede cambiar
   e incluso reasignar; el identificador del proveedor, no.»

5. **✏️ Y por qué hay dos servicios**
   ([`ServiciosUsuarioOAuth2`](src/main/java/com/atech/curso/m7/usuarios/ServiciosUsuarioOAuth2.java), usado
   en [`SecurityConfig` líneas 54-58](src/main/java/com/atech/curso/m7/config/SecurityConfig.java#L54-L58)):
   `oidcUserService()` para Google y `oauth2UserService()` para GitHub. «Se **envuelven** los servicios por
   defecto: se deja que hagan su trabajo y luego se añade el nuestro.»

6. **Ejercicio (20 min).** El provisionador y el envoltorio de los dos servicios.

   **⌨️ Criterio (no necesita internet):**

   ```bash
   ./mvnw -pl m7-security-oauth2 test -Dtest=ProvisionadorUsuariosTest
   ```

---

## Paso 5 · Resource Server, roles y seguridad de método (EJ 7.2) · 50 min

**El bloque más importante del módulo.**

### 5a · Los roles y la rotura que más tiempo hace perder (20 min)

1. **✏️ Proyecta [`RolesJwtConverter.java`](src/main/java/com/atech/curso/m7/config/RolesJwtConverter.java)**
   y explica qué hace: lee el claim plano `roles` y, si no está, `realm_access.roles` (el formato por defecto
   de Keycloak), y los convierte en autoridades `ROLE_*`.

2. **🔴 Ahora quítalo.** **✏️ En
   [`SecurityConfig.java` línea 66](src/main/java/com/atech/curso/m7/config/SecurityConfig.java#L66)**,
   comenta la línea:

   ```java
   // converter.setJwtGrantedAuthoritiesConverter(new RolesJwtConverter());
   ```

   **⌨️ Reinicia y repite la llamada del paso 1 con el token de `admin`:**

   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" localhost:8080/api/reservas
   ```

   → `403`, **con un token perfectamente válido**.

3. **🗣️ Explícalo despacio, porque es el fallo real que más horas cuesta en un proyecto:** «El token es
   válido: está firmado, no ha caducado, el emisor es correcto. Está **autenticado**. Lo que pasa es que
   Spring Security, por defecto, solo mira los *claims* `scope`/`scp` y les pone el prefijo `SCOPE_`. Los
   roles de Keycloak viven en `realm_access.roles` y **nadie los está leyendo**. Resultado: un usuario
   autenticado y sin ninguna autoridad.»

   **🗣️ Y el síntoma que tienen que reconocer:** «`401` es *no sé quién eres*; `403` con un token bueno es
   casi siempre *el conversor de autoridades*.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m7-security-oauth2/src/main/java/com/atech/curso/m7/config/SecurityConfig.java
   ```

4. **❓ Pregunta:** «`ROLE_` o `SCOPE_`, ¿cuál usamos?» → `hasRole("ADMIN")` busca la autoridad `ROLE_ADMIN`;
   los *scopes* del token llegan como `SCOPE_...`. Conceptualmente, **el rol es de la persona y el scope es
   de la aplicación** que actúa en su nombre. Lo importante: decidirlo en el equipo y escribirlo, porque
   mezclarlos en silencio abre agujeros.

### 5b · Autorización por URL y por método (30 min)

1. **✏️ Enseña las dos capas juntas**, que es lo que hay que entender:

   ```java
   // por URL, en SecurityConfig (línea 38)
   .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("USER", "ADMIN")
   ```

   ```java
   // por método, en ReservaService (línea 49)
   @PreAuthorize("hasRole('ADMIN') or @reservaService.esPropietario(#id, authentication.name)")
   public void cancelar(long id) { ... }
   ```

   **🗣️ Di:** «La regla de URL dice *para borrar hay que estar dentro de casa*. La de método dice *y además,
   tiene que ser tuya*. La primera no puede saber lo segundo: **no conoce el dato**. Por eso hacen falta las
   dos.»

2. **✏️ Señala el `@...` del SpEL:** `@reservaService` es **un bean**. «Podéis llamar a vuestra propia
   lógica desde la expresión. Eso evita escribir reglas imposibles de leer dentro de una cadena de texto.»

3. **✏️ Y `@PostFilter`**
   ([`ReservaService` líneas 110-114](src/main/java/com/atech/curso/m7/api/ReservaService.java#L110-L114)):

   ```java
   @PostFilter("filterObject.propietario() == authentication.name")
   public List<Reserva> mias() { return new ArrayList<>(listar()); }
   ```

   **🗣️ Con su advertencia:** «Filtra la lista **después** de calcularla, así que hay que traérselo todo de
   la base de datos para tirar la mitad. Para diez elementos, perfecto; para diez mil, filtrad en la
   consulta. Y fijaos en el `new ArrayList<>(...)`: la lista tiene que ser **mutable** o revienta.»

4. **⌨️ Demuéstralo en directo con los dos tokens** (`ana` y `admin`):

   ```bash
   curl -s -H "Authorization: Bearer $TOKEN_ANA" localhost:8080/api/reservas/mias
   curl -s -o /dev/null -w "%{http_code}\n" -X DELETE -H "Authorization: Bearer $TOKEN_ANA" localhost:8080/api/reservas/2
   curl -s -o /dev/null -w "%{http_code}\n" -X DELETE -H "Authorization: Bearer $TOKEN" localhost:8080/api/reservas/2
   ```

   → La lista de `ana` solo trae las suyas · `403` al borrar la ajena · `204` con el token de `admin`.

5. **🔴 Rotura del `@Order` (5 min), si va bien de tiempo.** **✏️ Cambia el `@Order(1)` de la cadena de la
   API por `@Order(3)`** y reinicia. La petición con token a `/api/reservas` devuelve **`302`** hacia el
   formulario de login en vez de `200`.

   **🗣️ «Un `302` donde esperabais un `401` o un `200` significa casi siempre que la cadena equivocada ha
   capturado la petición.»**

   **↩️ Deshaz:** `git checkout -- m7-security-oauth2/src/main/java/com/atech/curso/m7/config/SecurityConfig.java`

6. **Ejercicio (15 min):** las dos cadenas, el conversor de roles y las reglas por URL y por método.

   **⌨️ Criterio:**

   ```bash
   ./mvnw -pl m7-security-oauth2 test -Dtest='ApiSeguridadTest,SeguridadMetodosTest'
   ```

7. **🗣️ Recuerda la pizarra del módulo 1 antes de cerrar el bloque:** «`@PreAuthorize` funciona **en el
   proxy**. Si llamáis a `cancelar(...)` desde otro método del mismo servicio, **no se comprueba nada**. Es
   el mismo EJ 1.5 de la primera mañana, ahora con un agujero de seguridad de premio.»

---

## Paso 6 · Consumir una API protegida (EJ 7.3) · 25 min

1. **🗣️ Cambia el punto de vista:** «Hasta ahora éramos los que protegían. Ahora somos los que **consumen**
   una API ajena con el token del usuario que está conectado.»

2. **✏️ Proyecta [`GitHubClient.java`](src/main/java/com/atech/curso/m7/github/GitHubClient.java):**

   ```java
   this.rest = builder.baseUrl("https://api.github.com")
           .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(clientesAutorizados))
           .build();
   ```

   ```java
   rest.get().uri("/user/repos?sort=updated&per_page=10")
       .attributes(clientRegistrationId("github"))
       .retrieve()...
   ```

   **🗣️ Di:** «Ni un `setHeader("Authorization", ...)`. El interceptor busca el token del usuario en el
   `OAuth2AuthorizedClientManager`, lo pone, y **si ha caducado lo renueva con el *refresh token***. Comparad
   esto con guardar el token a mano en la sesión, que es lo que se ve en muchos proyectos.»

3. **Demo (si tienes credenciales):** entra con GitHub y abre <http://localhost:8080/perfil/repos>.

4. **✏️ Y de dónde sale ese manager**
   ([`SecurityConfig` líneas 72-83](src/main/java/com/atech/curso/m7/config/SecurityConfig.java#L72-L83)):
   `authorizationCode()` + `refreshToken()`. «Esos dos métodos son los que declaran **cómo** se consiguen y
   se renuevan los tokens.»

5. **🗣️ Cierra con la nota de Boot 4** (README, apartado extra): cliente HTTP declarativo con
   `@HttpExchange` + `@ClientRegistrationId("github")`, sin escribir el `RestClient`. «Mismo concepto, menos
   fontanería.»

---

## Paso 7 · Pruebas de seguridad (EJ 7.4) · 20 min

1. **⌨️ Ejecuta las cuatro clases y proyecta el resultado:**

   ```bash
   ./mvnw -pl m7-security-oauth2 test
   ```

   **🗣️ Señala lo importante antes de nada:** «Acabáis de probar toda la seguridad **sin Keycloak, sin
   Google, sin GitHub y sin internet**. Si probar la seguridad exige levantar el proveedor de identidad,
   nadie la prueba.»

2. **✏️ Recorre las tres técnicas, una por fichero:**

   | Test | Técnica | Qué demuestra |
   |---|---|---|
   | [`ApiSeguridadTest`](src/test/java/com/atech/curso/m7/ApiSeguridadTest.java) | `.with(jwt().authorities(USER))` | Construye la autenticación directamente, **sin decodificar ningún token** |
   | [`WebSeguridadTest`](src/test/java/com/atech/curso/m7/WebSeguridadTest.java) | `oidcLogin()`, `oauth2Login()` | Simula el login social sin salir a la red |
   | [`SeguridadMetodosTest`](src/test/java/com/atech/curso/m7/SeguridadMetodosTest.java) | `@WithMockUser` | Prueba `@PreAuthorize`/`@PostFilter` sin pasar por HTTP |

3. **✏️ Y enseña el par de tests que de verdad importa**, en `ApiSeguridadTest`:

   ```java
   @Test void conRolUserPuedeListar()  { ...with(jwt().authorities(USER))...isOk(); }
   @Test void sinRolesNoPuedeListar()  { ...with(jwt())...isForbidden(); }
   ```

   **🗣️ Di:** «El segundo es el que vale oro. **Probar que alguien NO puede** es lo que impide que un
   refactor abra una puerta sin que nadie se entere. Si vuestros tests de seguridad solo comprueban el
   camino feliz, no estáis probando la seguridad.»

4. **✏️ Enseña también `elConversorLeeElClaimRoles`**: prueba el `RolesJwtConverter` de verdad, con un token
   simulado que trae `roles`. «Es la red que os protege de la rotura del paso 5a.»

---

## Paso 8 · Cierre del módulo y del curso · 10 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m7-security-oauth2 test
   ```

   Mínimo exigible: `ApiSeguridadTest` y `SeguridadMetodosTest`.

2. **↩️ Limpieza:**

   ```bash
   docker compose -f m7-security-oauth2/compose.yaml down
   ```

3. **🗣️ Las tres frases del módulo:**
   - La seguridad es configuración declarativa, no `if`s repartidos por los servicios.
   - Un token dice quién eres y qué te han concedido; lo que puedes hacer con **tus** datos lo decide tu
     aplicación.
   - Y la seguridad se prueba: si no hay un test que falle cuando alguien la abra, la abrirán.

4. **🗣️ Cierre del curso (vuelve a la pizarra del proxy del primer día):** «Mirad todo lo que ha pasado por
   esa caja esta semana: `@Auditado`, `@Transactional`, `@Retryable`, `@PreAuthorize`. **Es un solo
   mecanismo**, no cuatro cosas distintas. Y una reserva ha recorrido los siete módulos: entra por el
   controlador (M4), se valida con la configuración (M2), se guarda en una transacción (M3), publica un
   evento (M1) que sale al broker (M5), lo procesa un flujo (M6), y todo ello detrás de una cadena de
   filtros (M7).»

5. **Trabajo posterior:** la práctica de migración de [`docs/MIGRACION-BOOT4.md`](../docs/MIGRACION-BOOT4.md)
   — crear la rama `boot4` y migrar un módulo. Es el mejor ejercicio de consolidación porque obliga a releer
   todo el código con otros ojos.

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 6 (consumir GitHub) | 20 min | Proyecta `GitHubClient` y cuenta el interceptor en dos frases. |
| Paso 4, ejercicio | 20 min | Demo del login + `ProvisionadorUsuariosTest`, sin escribir código. |
| Paso 5b, punto 5 (rotura del `@Order`) | 5 min | Cuéntalo sin ejecutarlo. |

**No recortes el paso 5a** (el conversor de roles) **ni el punto 1 del paso 5b** (URL frente a método): son
los dos que evitan incidentes reales.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Comprobación |
|---|---|---|
| `redirect_uri_mismatch` | La URL registrada no coincide exactamente | `http://localhost:8080/login/oauth2/code/{registrationId}` |
| La aplicación no arranca | Faltan `GITHUB_CLIENT_ID`/`SECRET` | Trabajar solo con Keycloak (pasos 5 y 7) |
| `403` con un token válido | Falta el conversor de roles | `GET /api/me` y mirar `autoridades` |
| `401` con un token recién pedido | `issuer-uri` que no coincide (`localhost` frente a `127.0.0.1`), o reloj desfasado | Comparar el claim `iss` con la configuración |
| `302` al login en vez de `401` | Orden de las cadenas o `securityMatcher` mal puesto | Los `@Order` de `SecurityConfig` |
| `@PreAuthorize` no se aplica | Auto-invocación: no pasa por el proxy | Módulo 1, EJ 1.5 |
| `POST` a la API devuelve `403` | CSRF activo en una cadena sin estado | `csrf.disable()` solo en la cadena de la API |
| `UnsupportedOperationException` en `@PostFilter` | La lista devuelta es inmutable | `new ArrayList<>(...)` |
