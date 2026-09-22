package com.atech.curso.m3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.repositorio.ReservaRepository;
import com.atech.curso.m3.servicio.ConflictoReservaException;
import com.atech.curso.m3.servicio.ReservaService;
import com.atech.curso.m3.servicio.ReservaService.Solicitud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * EJ 3.5 - Transacciones reales (sin @Transactional en el test, para ver commits y rollbacks).
 * Cada test usa franjas horarias distintas para no interferir con los demás.
 */
@SpringBootTest
class ReservaServiceTest {

    @Autowired
    ReservaService servicio;

    @Autowired
    ReservaRepository reservas;

    private static LocalDateTime t(String hora) {
        return LocalDateTime.parse(hora);
    }

    @Test
    void rechazaReservasSolapadas() {
        servicio.reservar(new Solicitud("Lovelace", "luis@atech.es", t("2031-03-01T09:00"), t("2031-03-01T10:00")));

        assertThatThrownBy(() -> servicio.reservar(
                new Solicitud("Lovelace", "ana@atech.es", t("2031-03-01T09:30"), t("2031-03-01T11:00"))))
                .isInstanceOf(ConflictoReservaException.class);
    }

    @Test
    void reservarVariasEsTodoONada() {
        long antes = reservas.count();
        List<Solicitud> lote = List.of(
                new Solicitud("Hopper", "ana@atech.es", t("2031-04-01T09:00"), t("2031-04-01T10:00")),
                new Solicitud("Hopper", "luis@atech.es", t("2031-04-01T09:30"), t("2031-04-01T10:30")));

        assertThatThrownBy(() -> servicio.reservarVarias(lote)).isInstanceOf(ConflictoReservaException.class);

        // La primera reserva también se ha deshecho (rollback de toda la transacción)
        assertThat(reservas.count()).isEqualTo(antes);
    }

    @Test
    void cancelarUsaDirtyChecking() {
        Reserva r = servicio.reservar(
                new Solicitud("Turing", "marta@atech.es", t("2031-05-01T09:00"), t("2031-05-01T10:00")));

        servicio.cancelar(r.getId());

        assertThat(reservas.findById(r.getId())).get()
                .extracting(Reserva::getEstado).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void bloqueoOptimistaDetectaActualizacionesConcurrentes() {
        Reserva creada = servicio.reservar(
                new Solicitud("Turing", "ana@atech.es", t("2031-06-01T09:00"), t("2031-06-01T10:00")));

        // Dos "usuarios" leen la misma versión de la reserva
        Reserva copiaA = reservas.findById(creada.getId()).orElseThrow();
        Reserva copiaB = reservas.findById(creada.getId()).orElseThrow();

        copiaA.setNotas("Cambio de A");
        reservas.save(copiaA);

        copiaB.setNotas("Cambio de B");
        assertThatThrownBy(() -> reservas.save(copiaB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
