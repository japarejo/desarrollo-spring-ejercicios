package com.atech.curso.m4;

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
import com.atech.curso.m4.servicio.SalaService;
import com.atech.curso.m4.web.SalaForm;
import com.atech.curso.m4.web.SalaWebController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void listadoUsaLaVistaJspConLasSalas() throws Exception {
        given(salas.listar()).willReturn(List.of(new Sala("Turing", 12, true), new Sala("Hopper", 30, true)));

        mvc.perform(get("/salas"))
            .andExpect(status().isOk())
            .andExpect(view().name("salas/lista"))
            .andExpect(forwardedUrl("/WEB-INF/jsp/salas/lista.jsp"))
            .andExpect(model().attribute("salas", hasSize(2)));
    }

    @Test
    void formularioInvalidoVuelveALaVistaConErrores() throws Exception {
        mvc.perform(post("/salas").param("nombre", "").param("capacidad", "0"))
            .andExpect(status().isOk())
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "nombre", "NotBlank"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "capacidad", "Min"));
        verify(salas, never()).guardar(any());
    }

    @Test
    void nombreDuplicadoEsUnErrorDeCampo() throws Exception {
        given(salas.existeNombre("Turing")).willReturn(true);

        mvc.perform(post("/salas").param("nombre", "Turing").param("capacidad", "10"))
            .andExpect(view().name("salas/formulario"))
            .andExpect(model().attributeHasFieldErrorCode("sala", "nombre", "sala.nombre.duplicado"));
    }

    @Test
    void formularioValidoRedirigeConMensajeFlash() throws Exception {
        given(salas.guardar(any(SalaForm.class))).willReturn(new Sala("Babbage", 8, false));

        mvc.perform(post("/salas").param("nombre", "Babbage").param("capacidad", "8"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/salas"))
            .andExpect(flash().attribute("mensaje", "Sala Babbage guardada"));
    }
}
