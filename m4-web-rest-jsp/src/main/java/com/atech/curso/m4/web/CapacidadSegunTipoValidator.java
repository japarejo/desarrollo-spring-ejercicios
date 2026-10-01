package com.atech.curso.m4.web;

import com.atech.curso.m4.dominio.TipoSala;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/** EJ 4.5 - Implementación de {@link CapacidadSegunTipo}. */
public class CapacidadSegunTipoValidator implements ConstraintValidator<CapacidadSegunTipo, SalaForm> {

    @Override
    public boolean isValid(SalaForm sala, ConstraintValidatorContext context) {
        TipoSala tipo = sala.getTipo();
        // Sin tipo no hay nada que comparar: de ese error ya avisa @NotNull en el campo
        if (tipo == null || sala.getCapacidad() <= tipo.getCapacidadMaxima()) {
            return true;
        }
        // Parámetro {max} del mensaje (extensión de Hibernate Validator). Solo se pasan datos, no textos:
        // la frase completa se traduce en messages*.properties
        context.unwrap(HibernateConstraintValidatorContext.class)
            .addMessageParameter("max", tipo.getCapacidadMaxima());
        // El error se cuelga del campo "capacidad", no del objeto: así form:errors lo pinta junto al campo
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode("capacidad")
            .addConstraintViolation();
        return false;
    }
}
