package com.atech.curso.m1.pista;

/**
 * EJ 1.7 - Quién está ejecutando la operación.
 *
 * <p>En una aplicación real lo resolvería Spring Security desde el {@code SecurityContextHolder}
 * (es justo lo que hace el bean {@code AuditorAware} de Spring Data JPA en el módulo 3).
 */
public interface UsuarioActual {

    String nombre();
}
