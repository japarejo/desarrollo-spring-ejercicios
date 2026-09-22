package com.atech.curso.m4.servicio;

import com.atech.curso.m4.api.ReservaRequest;
import com.atech.curso.m4.api.ReservaResponse;
import com.atech.curso.m4.dominio.Reserva;
import com.atech.curso.m4.dominio.ReservaRepository;
import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.SalaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Devuelve DTOs: las entidades no salen de la capa de servicio (open-in-view desactivado). */
@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository reservas;
    private final SalaRepository salas;

    public ReservaService(ReservaRepository reservas, SalaRepository salas) {
        this.reservas = reservas;
        this.salas = salas;
    }

    public Page<ReservaResponse> listar(Pageable pagina) {
        return reservas.findAll(pagina).map(ReservaResponse::de);
    }

    public ReservaResponse buscar(Long id) {
        return ReservaResponse.de(obtener(id));
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest peticion) {
        validar(peticion);
        Sala sala = obtenerSala(peticion.salaId());
        Reserva reserva = reservas.save(new Reserva(sala, peticion.usuario(), peticion.inicio(), peticion.fin()));
        return ReservaResponse.de(reserva);
    }

    @Transactional
    public ReservaResponse modificar(Long id, ReservaRequest peticion) {
        validar(peticion);
        Reserva reserva = obtener(id);
        reserva.modificar(obtenerSala(peticion.salaId()), peticion.inicio(), peticion.fin());
        return ReservaResponse.de(reserva);
    }

    @Transactional
    public void borrar(Long id) {
        reservas.delete(obtener(id));
    }

    private void validar(ReservaRequest peticion) {
        if (!peticion.fin().isAfter(peticion.inicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private Reserva obtener(Long id) {
        return reservas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    private Sala obtenerSala(Long id) {
        return salas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sala", id));
    }
}
