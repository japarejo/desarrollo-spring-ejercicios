package com.atech.curso.xml.dominio;

/**
 * POJO del dominio: <b>ni una sola anotación de Spring</b>.
 *
 * <p>Todo este paquete es deliberadamente agnóstico del contenedor. Así las mismas clases se pueden
 * cablear con XML, con {@code @Configuration}/{@code @Bean} o a mano con {@code new}, y se ve que
 * la diferencia entre los estilos está <em>solo</em> en los metadatos de configuración.
 */
public record Sala(String id, String nombre, int aforo) {
}
