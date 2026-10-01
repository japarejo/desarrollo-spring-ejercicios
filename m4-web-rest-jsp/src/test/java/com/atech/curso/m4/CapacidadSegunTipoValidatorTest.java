package com.atech.curso.m4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.atech.curso.m4.dominio.TipoSala;
import com.atech.curso.m4.web.SalaForm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** EJ 4.5 - Una restricción propia se prueba sin Spring: basta con Bean Validation. */
class CapacidadSegunTipoValidatorTest {

    static ValidatorFactory fabrica;
    static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static SalaForm sala(TipoSala tipo, int capacidad) {
        SalaForm form = new SalaForm();
        form.setNombre("Babbage");
        form.setEmailResponsable("ana@atech.es");
        form.setTipo(tipo);
        form.setCapacidad(capacidad);
        return form;
    }

    @Test
    void dentroDelAforoDelTipoEsValida() {
        assertThat(validador.validate(sala(TipoSala.REUNIONES, 20))).isEmpty();
    }

    @Test
    void porEncimaDelAforoDelTipoFallaEnElCampoCapacidad() {
        Set<ConstraintViolation<SalaForm>> errores = validador.validate(sala(TipoSala.REUNIONES, 21));

        assertThat(errores).singleElement().satisfies(error -> {
            assertThat(error.getPropertyPath()).hasToString("capacidad");
            assertThat(error.getMessageTemplate()).isEqualTo("{sala.capacidad.segunTipo}");
        });
    }

    @Test
    void sinTipoNoComparaYSoloAvisaNotNull() {
        Set<ConstraintViolation<SalaForm>> errores = validador.validate(sala(null, 300));

        assertThat(errores).singleElement()
            .satisfies(error -> assertThat(error.getPropertyPath()).hasToString("tipo"));
    }
}
