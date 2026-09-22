package com.atech.curso.m3.dominio;

import jakarta.persistence.Embeddable;

/** EJ 3.1 - Hibernate 6.2+ admite records como tipos embebidos (valores inmutables). */
@Embeddable
public record Direccion(String calle, String ciudad, String codigoPostal) {
}
