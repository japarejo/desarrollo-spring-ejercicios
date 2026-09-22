package com.atech.curso.m4;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.atech.curso.m4.api.ReservaRequest;
import com.atech.curso.m4.api.ReservaResponse;
import com.atech.curso.m4.api.ReservaRestController;
import com.atech.curso.m4.servicio.RecursoNoEncontradoException;
import com.atech.curso.m4.servicio.ReservaService;

import io.restassured.config.DecoderConfig;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** EJ 4.4 - @WebMvcTest + REST Assured (módulo spring-mock-mvc) con estilo given/when/then. */
@WebMvcTest(ReservaRestController.class)
class ReservaRestControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReservaService servicio;

    @BeforeEach
    void configurar() {
        RestAssuredMockMvc.mockMvc(mvc);
        // application/problem+json no declara charset: forzamos UTF-8 para leer bien las tildes
        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config()
                .decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset("UTF-8"));
    }

    @Test
    void crearDevuelve201ConLocation() {
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 10, 9, 0);
        when(servicio.crear(any(ReservaRequest.class)))
                .thenReturn(new ReservaResponse(7L, 1L, "Turing", "ana@atech.es", inicio, inicio.plusHours(2)));

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"salaId":1,"usuario":"ana@atech.es","inicio":"2030-01-10T09:00:00","fin":"2030-01-10T11:00:00"}
                """)
        .when()
            .post("/api/v1/reservas")
        .then()
            .statusCode(201)
            .header("Location", endsWith("/api/v1/reservas/7"))
            .body("sala", equalTo("Turing"));
    }

    @Test
    void recursoInexistenteDevuelveProblemDetail404() {
        when(servicio.buscar(99L)).thenThrow(new RecursoNoEncontradoException("Reserva", 99L));

        given()
        .when()
            .get("/api/v1/reservas/99")
        .then()
            .statusCode(404)
            .contentType("application/problem+json")
            .body("title", equalTo("Recurso no encontrado"))
            .body("detail", equalTo("Reserva con id 99 no encontrado"));
    }

    @Test
    void peticionInvalidaDevuelve400ConErroresPorCampo() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"usuario":"","inicio":"2000-01-01T09:00:00","fin":"2030-01-10T11:00:00"}
                """)
        .when()
            .post("/api/v1/reservas")
        .then()
            .statusCode(400)
            .body("title", equalTo("Datos no válidos"))
            .body("errores.salaId", notNullValue())
            .body("errores.usuario", notNullValue())
            .body("errores.inicio", notNullValue());
    }
}
