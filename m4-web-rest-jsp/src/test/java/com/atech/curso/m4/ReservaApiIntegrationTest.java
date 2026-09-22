package com.atech.curso.m4;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** EJ 4.4 - Prueba de extremo a extremo sobre Tomcat real en un puerto aleatorio. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservaApiIntegrationTest {

    @LocalServerPort
    int puerto;

    @BeforeEach
    void configurar() {
        RestAssured.port = puerto;
    }

    @Test
    void cicloDeVidaCompletoDeUnaReserva() {
        String location = given()
                .contentType(ContentType.JSON)
                .body("""
                    {"salaId":1,"usuario":"ana@atech.es","inicio":"2030-02-01T09:00:00","fin":"2030-02-01T10:00:00"}
                    """)
            .when()
                .post("/api/v1/reservas")
            .then()
                .statusCode(201)
                .extract().header("Location");

        given().when().get(location).then().statusCode(200).body("usuario", equalTo("ana@atech.es"));

        given().queryParam("size", 5).queryParam("sort", "inicio,desc")
            .when().get("/api/v1/reservas")
            .then().statusCode(200)
                .body("page.size", equalTo(5))
                .body("page.totalElements", greaterThanOrEqualTo(1));

        given().when().delete(location).then().statusCode(204);
        given().when().get(location).then().statusCode(404);
    }

    @Test
    void reglaDeNegocioDevuelve422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"salaId":1,"usuario":"ana@atech.es","inicio":"2030-02-01T12:00:00","fin":"2030-02-01T10:00:00"}
                """)
        .when()
            .post("/api/v1/reservas")
        .then()
            .statusCode(422)
            .body("title", equalTo("Regla de negocio incumplida"));
    }

    @Test
    void publicaElContratoOpenApi() {
        given().when().get("/v3/api-docs")
            .then().statusCode(200)
                .body("info.title", equalTo("API de Reservas"))
                .body("paths.keySet()", org.hamcrest.Matchers.hasItem("/api/v1/reservas"));
    }

    @Test
    void laVistaJspSeRenderiza() {
        given().when().get("/salas")
            .then().statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Turing"));
    }
}
