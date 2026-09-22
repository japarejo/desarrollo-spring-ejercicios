package com.atech.curso.m1.auditoria;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** EJ 1.4 - Marca los métodos cuya ejecución debe auditarse. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditado {

    /** Nombre lógico de la operación; si se omite se usa la firma del método. */
    String value() default "";
}
