package com.atech.curso.m4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.TipoSala;
import com.atech.curso.m4.servicio.SalaService;
import com.atech.curso.m4.web.SalaForm;
import com.atech.curso.m4.web.SalaWebController;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;

/**
 * EJ 4.4 - Slice web: sólo la capa MVC. MockMvc no renderiza JSP, pero comprueba
 * la vista resuelta, el modelo y los errores de validación.
 */
@WebMvcTest(SalaWebController.class)
class SalaWebControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SalaService salas;

    /** Un POST con todos los campos obligatorios correctos; cada test cambia solo lo que le interesa. */
    private static MockHttpServletRequestBuilder postSala(String nombre, String tipo, int capacidad) {
        return post("/salas")
            .param("nombre", nombre)
            .param("tipo", tipo)
            .param("capacidad", String.valueOf(capacidad))
            .param("emailResponsable", "ana@atech.es");
    }

    @Test
    void listadoUsaLaVistaJspConLasSalas() throws Exception {
        given(salas.listar(null)).willReturn(List.of(
            new Sala("Turing", TipoSala.REUNIONES, 12, true), new Sala("Hopper", TipoSala.FORMACION, 30, true)));

        mvc.perform(get("/salas"))
            .andExpect(status().isOk())
            .andExpect(view().name("salas/lista"))
            .andExpect(forwardedUrl("/WEB-INF/jsp/salas/lista.jsp"))
            .andExpect(model().attribute("salas", hasSize(2)))
            .andExpect(model().attribute("tipos", TipoSala.values()));
    }

    @Test
    void elListadoSeFiltraPorTipo() throws Exception {
        given(salas.listar(TipoSala.FORMACION)).willReturn(List.of(new Sala("Hopper", TipoSala.FORMACION, 30, true)));

        mvc.perform(get("/salas").param("tipo", "FORMACION"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("salas", hasSize(1)));
        verify(salas).listar(TipoSala.FORMACION);
    }

    @Test
    void formularioInvalidoVuelveALaVistaConErrores() throws Exception {
        mvc.perform(post("/salas").param("nombre", "").param("capacidad", "0"))
            .andExpect(status().isOk())
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "nombre", "NotBlank"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "capacidad", "Min"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "tipo", "NotNull"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "emailResponsable", "NotBlank"));
        verify(salas, never()).guardar(any());
    }

    @Test
    void emailMalFormadoEsUnErrorDeCampo() throws Exception {
        mvc.perform(postSala("Babbage", "REUNIONES", 8).param("emailResponsable", "ana-en-atech"))
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "emailResponsable", "Email"));
    }

    @Test
    void capacidadMayorQueLaDelTipoEsUnErrorEnElCampoCapacidad() throws Exception {
        MvcResult resultado = mvc.perform(postSala("Babbage", "REUNIONES", 120))
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "capacidad", "CapacidadSegunTipo"))
            .andReturn();

        // El mensaje sale de messages.properties con los parámetros que pone el validador
        BindingResult errores = (BindingResult) resultado.getModelAndView().getModel()
            .get(BindingResult.MODEL_KEY_PREFIX + "sala");
        assertThat(errores.getFieldError("capacidad").getDefaultMessage())
            .isEqualTo("Una sala de tipo Reuniones admite como máximo 20 personas");
        verify(salas, never()).guardar(any());
    }

    @Test
    void laMismaCapacidadEsValidaEnUnAuditorio() throws Exception {
        given(salas.guardar(any(SalaForm.class))).willReturn(new Sala("Babbage", TipoSala.AUDITORIO, 120, true));

        mvc.perform(postSala("Babbage", "AUDITORIO", 120))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/salas"));
    }

    @Test
    void lasCasillasDeEquipamientoLleganComoConjuntoDeIds() throws Exception {
        given(salas.guardar(any(SalaForm.class))).willReturn(new Sala("Babbage", TipoSala.REUNIONES, 8, false));

        mvc.perform(postSala("Babbage", "REUNIONES", 8).param("equipamiento", "1", "3"))
            .andExpect(status().is3xxRedirection());

        ArgumentCaptor<SalaForm> form = ArgumentCaptor.forClass(SalaForm.class);
        verify(salas).guardar(form.capture());
        assertThat(form.getValue().getEquipamiento()).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void nombreDuplicadoEsUnErrorDeCampo() throws Exception {
        given(salas.existeNombre("Turing", null)).willReturn(true);

        mvc.perform(postSala("Turing", "REUNIONES", 10))
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "nombre", "sala.nombre.duplicado"));
    }

    @Test
    void editarUnaSalaConservandoSuNombreNoEsDuplicado() throws Exception {
        given(salas.existeNombre("Turing", 1L)).willReturn(false);
        given(salas.guardar(any(SalaForm.class))).willReturn(new Sala("Turing", TipoSala.REUNIONES, 14, true));

        mvc.perform(postSala("Turing", "REUNIONES", 14).param("id", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/salas"));
        verify(salas).existeNombre("Turing", 1L);
    }

    @Test
    void formularioValidoRedirigeConMensajeFlash() throws Exception {
        given(salas.guardar(any(SalaForm.class))).willReturn(new Sala("Babbage", TipoSala.REUNIONES, 8, false));

        mvc.perform(postSala("Babbage", "REUNIONES", 8))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/salas"))
            .andExpect(flash().attribute("mensaje", "Sala Babbage guardada"));
    }
}
