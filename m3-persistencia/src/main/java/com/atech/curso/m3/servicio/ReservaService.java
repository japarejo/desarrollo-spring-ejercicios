package com.atech.curso.m3.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.dominio.Sala;
import com.atech.curso.m3.dominio.Usuario;
import com.atech.curso.m3.repositorio.ReservaRepository;
import com.atech.curso.m3.repositorio.ReservaSpecs;
import com.atech.curso.m3.repositorio.SalaRepository;
import com.atech.curso.m3.repositorio.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EJ 3.5 - Reglas de negocio transaccionales. Por defecto sólo las RuntimeException
 * provocan rollback; las consultas usan readOnly = true (optimiza el flush de Hibernate).
 */
@Service
@Transactional(readOnly = true)
public class ReservaService {

    public record Solicitud(String sala, String email, LocalDateTime inicio, LocalDateTime fin) {
    }

    private final ReservaRepository reservas;
    private final SalaRepository salas;
    private final UsuarioRepository usuarios;

    public ReservaService(ReservaRepository reservas, SalaRepository salas, UsuarioRepository usuarios) {
        this.reservas = reservas;
        this.salas = salas;
        this.usuarios = usuarios;
    }

    @Transactional
    public Reserva reservar(Solicitud s) {
        if (!s.fin().isAfter(s.inicio())) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
        Sala sala = salas.findByNombre(s.sala())
                .orElseThrow(() -> new NoSuchElementException("Sala no encontrada: " + s.sala()));
        Usuario usuario = usuarios.findByEmail(s.email())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + s.email()));
        if (reservas.existeSolape(sala.getId(), s.inicio(), s.fin(), EstadoReserva.CONFIRMADA)) {
            throw new ConflictoReservaException("La sala " + sala.getNombre() + " ya está reservada en ese horario");
        }
        return reservas.save(new Reserva(sala, usuario, s.inicio(), s.fin()));
    }

    /** Todo o nada: si una de las reservas falla, se deshacen todas. */
    @Transactional
    public List<Reserva> reservarVarias(List<Solicitud> solicitudes) {
        return solicitudes.stream().map(this::reservar).toList();
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = reservas.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + id));
        reserva.cancelar(); // dirty checking: no hace falta save()
        return reserva;
    }

    public Page<Reserva> buscar(ReservaSpecs.Filtro filtro, Pageable pagina) {
        return reservas.findAll(ReservaSpecs.de(filtro), pagina);
    }
}
