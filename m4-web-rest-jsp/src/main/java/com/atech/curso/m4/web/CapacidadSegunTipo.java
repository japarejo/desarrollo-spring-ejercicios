package com.atech.curso.m4.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * EJ 4.5 - Restricción propia a nivel de clase: la capacidad no puede superar el aforo máximo del tipo de
 * sala. Va en la clase y no en un campo porque necesita ver dos campos a la vez.
 * <p>
 * El mensaje {@code {sala.capacidad.segunTipo}} se busca en messages*.properties, en el idioma de la
 * petición: Spring Boot conecta su {@code MessageSource} con Bean Validation.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CapacidadSegunTipoValidator.class)
public @interface CapacidadSegunTipo {

    String message() default "{sala.capacidad.segunTipo}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
