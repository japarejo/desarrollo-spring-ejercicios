package com.atech.curso.m1.pista;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EJ 1.7 - El valor del parámetro se enmascara antes de guardarlo.
 *
 * <p>Una pista de auditoría se conserva durante años: nunca debe contener IBAN, tarjetas,
 * contraseñas ni datos personales en claro.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensible {
}
