package com.atech.curso.m6;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atech.curso.m6.dominio.SolicitudReserva;
import com.atech.curso.m6.flujos.FlujosReservas;
import com.atech.curso.m6.flujos.ReservasGateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI y el contrato OpenAPI de la entrada web que se usa en clase. No necesita RabbitMQ: la
 * pasarela es un doble y los pollers no arrancan.
 */
@SpringBootTest
@SpringIntegrationTest(noAutoStartup = { FlujosReservas.ENDPOINT_JDBC, FlujosReservas.ENDPOINT_FICHEROS })
@AutoConfigureMockMvc
class SwaggerUiTest {

    static final String RUTA = "/api/solicitudes";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReservasGateway gateway;

    @Test
    void swaggerUiEstaPublicada() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void elContratoIncluyeLosEjemplosDelGuion() throws Exception {
        String ejemplos = "$.paths['" + RUTA + "'].post.requestBody.content['application/json'].examples";
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Solicitudes de reserva (Spring Integration)"))
            .andExpect(jsonPath(ejemplos, hasKey("Una línea corta y una larga (S-1)")))
            .andExpect(jsonPath(ejemplos, hasKey("Solicitud sin líneas (S-VACIA)")))
            .andExpect(jsonPath("$.paths['/tarifas']").doesNotExist());
    }

    @Test
    void enviarDevuelve202YPasaLaSolicitudALaPasarela() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"id":"S-1","lineas":[{"sala":"Turing","fecha":"2030-03-01","horas":2}]}"""))
            .andExpect(status().isAccepted());

        verify(gateway).enviar(any(SolicitudReserva.class));
    }
}
