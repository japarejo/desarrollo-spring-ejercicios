# Módulo 7 · Spring Security, OAuth 2.0 y OpenID Connect

**Objetivo:** proteger la aplicación de reservas con dos mecanismos que conviven en la misma aplicación:

- **Web:** login social con Google (OIDC) y GitHub (OAuth 2.0) y sesión HTTP.
- **API** (`/api/**`): *Resource Server* sin estado que valida los JWT emitidos por Keycloak.

```bash
cd m7-security-oauth2 && docker compose up -d          # Keycloak con el realm "atech"
export GITHUB_CLIENT_ID=... GITHUB_CLIENT_SECRET=...     # y/o GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET
mvn -pl m7-security-oauth2 spring-boot:run

# Token de pruebas (password grant, SÓLO para el curso)
TOKEN=$(curl -s -d grant_type=password -d client_id=reservas-cli -d username=admin -d password=admin \
  http://localhost:8180/realms/atech/protocol/openid-connect/token | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/me
curl -X DELETE -H "Authorization: Bearer $TOKEN" localhost:8080/api/reservas/4
```

Usuarios del realm: `ana/ana` (USER) y `admin/admin` (USER, ADMIN). El *mapper* `roles-planos` copia los roles del realm al claim `roles`.

## Enunciados

### EJ 7.1 · Login social y alta de usuarios
1. Registra la aplicación en Google Cloud Console y en GitHub (OAuth Apps) con la *redirect URI* `http://localhost:8080/login/oauth2/code/{registrationId}`.
2. Configura `spring.security.oauth2.client.registration.{google,github}` usando variables de entorno, nunca secretos en el repositorio.
3. Envuelve `OidcUserService` y `DefaultOAuth2UserService` para dar de alta el `UsuarioLocal` en el primer acceso y añadir su rol (`ROLE_ADMIN` para los correos de `atech.seguridad.admins`).

*Solución:* paquete `usuarios` · *Test:* `ProvisionadorUsuariosTest`.

### EJ 7.2 · Resource Server JWT y autorización
1. Define dos `SecurityFilterChain`: `/api/**` sin estado y sin CSRF, y la cadena web con `oauth2Login`. Usa `@Order` y `securityMatcher`.
2. Crea `RolesJwtConverter`, que lee el claim `roles` y también `realm_access.roles`, el formato por defecto de Keycloak.
3. Autoriza por URL (`requestMatchers`) y por método (`@PreAuthorize` con una llamada a un bean `@reservaService.esPropietario(#id, authentication.name)` y `@PostFilter`).

### EJ 7.3 · Consumir una API protegida con OAuth 2.0
`GitHubClient` lista los repositorios del usuario conectado con `RestClient` + `OAuth2ClientHttpRequestInterceptor`. El interceptor obtiene el token del `OAuth2AuthorizedClientManager` y lo renueva si hace falta. Entra con GitHub y abre `/perfil/repos`.

### EJ 7.4 · Pruebas de seguridad
- API: `jwt()`, con autoridades explícitas o usando el propio `RolesJwtConverter`.
- Web: `oidcLogin()` y `oauth2Login()`, sin contactar con los proveedores.
- Servicios: `@WithMockUser`.

*Tests:* `ApiSeguridadTest`, `WebSeguridadTest`, `SeguridadMetodosTest`.

## Extra Spring Boot 4 / Spring Security 7
- Starters renombrados: `spring-boot-starter-security-oauth2-client` y `spring-boot-starter-security-oauth2-resource-server`.
- Cliente HTTP declarativo con el token del usuario:
  ```java
  @HttpExchange
  @ClientRegistrationId("github")
  public interface GithubApi {
      @GetExchange("/user/repos") List<RepoGitHub> repositorios();
  }

  @Configuration
  @ImportHttpServices(group = "github", types = GithubApi.class)
  class ClientesConfig {
      @Bean
      OAuth2RestClientHttpServiceGroupConfigurer oauth2(OAuth2AuthorizedClientManager manager) {
          return OAuth2RestClientHttpServiceGroupConfigurer.from(manager);
      }
  }
  ```
  ```yaml
  spring.http.serviceclient.github.base-url: https://api.github.com
  ```
- **MFA:** `@EnableMultiFactorAuthentication(authorities = {FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.OTT_AUTHORITY})` con `formLogin()` + `oneTimeTokenLogin()`.
- **Passkeys (WebAuthn):** `http.webAuthn(w -> w.rpName("Atech").rpId("localhost").allowedOrigins("http://localhost:8080"))`.
- PKCE activado por defecto también para los clientes confidenciales.
