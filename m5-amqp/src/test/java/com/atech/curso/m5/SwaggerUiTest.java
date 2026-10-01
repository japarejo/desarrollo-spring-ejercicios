package com.atech.curso.m5;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;

import com.atech.curso.m5.eventos.ReservaConfirmada;
import com.atech.curso.m5.productor.PublicadorReservas;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI y el contrato OpenAPI del punto de entrada que se usa en clase para publicar eventos.
 * No necesita RabbitMQ: los listeners no arrancan y el publicador es un doble.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class SwaggerUiTest {

    static final String RUTA = "/api/eventos/reservas-confirmadas";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PublicadorReservas publicador;

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
            .andExpect(jsonPath("$.info.title").value("Eventos de reservas (RabbitMQ)"))
            .andExpect(jsonPath(ejemplos, hasKey("Reserva válida (R-1)")))
            .andExpect(jsonPath(ejemplos, hasKey("Importe negativo (R-400)")));
    }

    @Test
    void publicarDevuelve202CuandoElBrokerConfirma() throws Exception {
        given(publicador.publicar(any(ReservaConfirmada.class)))
            .willReturn(CompletableFuture.completedFuture(new CorrelationData.Confirm(true, null)));

        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"reservaId":"R-1","sala":"Turing","usuario":"ana@atech.es",
                 "inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00","importe":30}"""))
            .andExpect(status().isAccepted())
            .andExpect(content().string("Evento confirmado por el broker"));
    }
}
