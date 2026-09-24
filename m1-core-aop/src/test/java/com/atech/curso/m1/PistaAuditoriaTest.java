package com.atech.curso.m1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.atech.curso.m1.pedidos.PedidoService;
import com.atech.curso.m1.pista.ApunteAuditoria;
import com.atech.curso.m1.pista.LibroAuditoria;
import com.atech.curso.m1.pista.UsuarioActualEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** EJ 1.7 - La auditabilidad como aspecto: quién, qué, sobre qué y con qué resultado. */
@SpringBootTest
class PistaAuditoriaTest {

    private static final Instant AHORA = Instant.parse("2026-09-24T10:15:30Z");

    /**
     * Al sustituir el {@code Clock} por uno fijo, el momento del apunte es determinista.
     * Es la razón de inyectar el reloj en vez de llamar a {@code Instant.now()} (EJ 1.2).
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    PedidoService pedidos;

    @Autowired
    LibroAuditoria libro;

    @Autowired
    UsuarioActualEnMemoria usuario;

    @BeforeEach
    void limpiar() {
        libro.limpiar();
        usuario.autenticar("ana@atech.es");
    }

    @Test
    void registraQuienQueYCuandoCuandoLaOperacionVaBien() {
        pedidos.reembolsar("P-77", new BigDecimal("250.00"), "ES9121000418450200051332");

        assertThat(libro.apuntes()).singleElement().satisfies(apunte -> {
            assertThat(apunte.usuario()).isEqualTo("ana@atech.es");
            assertThat(apunte.accion()).isEqualTo("REEMBOLSAR");
            assertThat(apunte.entidad()).isEqualTo("Pedido");
            assertThat(apunte.idEntidad()).isEqualTo("P-77");
            assertThat(apunte.momento()).isEqualTo(AHORA);
            assertThat(apunte.correcto()).isTrue();
            assertThat(apunte.detalle()).isNull();
        });
    }

    @Test
    void enmascaraLosDatosSensiblesYConservaElResto() {
        pedidos.reembolsar("P-77", new BigDecimal("250.00"), "ES9121000418450200051332");

        ApunteAuditoria apunte = libro.apuntes().get(0);
        assertThat(apunte.datos()).containsEntry("importe", "250.00");
        // El IBAN no puede quedar en claro en un registro que se conserva durante años
        assertThat(apunte.datos()).containsEntry("iban", "****1332");
        assertThat(apunte.datos().values()).noneMatch(v -> v.contains("ES9121000418450200051332"));
        // El identificador de la entidad va en su propio campo, no entre los datos
        assertThat(apunte.datos()).doesNotContainKey("pedidoId");
    }

    @Test
    void registraElFalloSinTragarseLaExcepcion() {
        assertThatThrownBy(() -> pedidos.reembolsar("P-88", new BigDecimal("5000"), "ES9121000418450200051332"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(libro.apuntes()).singleElement().satisfies(apunte -> {
            assertThat(apunte.idEntidad()).isEqualTo("P-88");
            assertThat(apunte.correcto()).isFalse();
            assertThat(apunte.detalle()).contains("IllegalStateException").contains("por encima del límite");
        });
    }

    @Test
    void cadaUsuarioDejaSuPropioRastro() {
        pedidos.reembolsar("P-1", BigDecimal.TEN, "ES9121000418450200051332");
        usuario.autenticar("supervisor@atech.es");
        pedidos.reembolsar("P-2", BigDecimal.ONE, "ES9121000418450200051332");

        assertThat(libro.apuntes()).extracting(ApunteAuditoria::usuario)
                .containsExactly("ana@atech.es", "supervisor@atech.es");
    }

    @Test
    void confirmarNoDejaPistaDeAuditoria() {
        // confirmar() lleva @Auditado (medición), no @PistaAuditoria: cada aspecto atiende
        // a su propio interés transversal y no se pisan.
        pedidos.confirmar(new com.atech.curso.m1.pedidos.Pedido("P-9", "ana@atech.es", BigDecimal.TEN));

        assertThat(libro.apuntes()).isEmpty();
    }
}
