package com.atech.curso.m2;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** EJ 2.1/2.4 - Aplicación completa con MockMvc (perfil dev por defecto, H2). */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void creaUnaReservaValida() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                {"sala":"Turing","usuario":"ana@atech.es","fecha":"2099-01-15","inicio":"09:00","fin":"11:00"}
                """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/reservas/")))
            .andExpect(jsonPath("$.sala").value("Turing"));
    }

    @Test
    void rechazaSalasQueNoEstanEnLaConfiguracion() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                {"sala":"Babbage","usuario":"ana@atech.es","fecha":"2099-01-15","inicio":"09:00","fin":"10:00"}
                """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rechazaReservasMasLargasQueLaDuracionMaxima() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                {"sala":"Hopper","usuario":"ana@atech.es","fecha":"2099-01-15","inicio":"08:00","fin":"13:00"}
                """))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void validaElCuerpoDeLaPeticion() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                {"sala":"","usuario":"ana@atech.es"}
                """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void actuatorExponeSaludSondasYEndpointPropio() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.salas.details.salasConfiguradas").value(3));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/reservas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.porSala.Turing").exists());
        mvc.perform(get("/actuator/info"))
            .andExpect(jsonPath("$.curso.modulo").value(2));
    }
}
