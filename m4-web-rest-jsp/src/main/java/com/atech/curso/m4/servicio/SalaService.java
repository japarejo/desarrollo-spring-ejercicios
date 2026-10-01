package com.atech.curso.m4.servicio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.atech.curso.m4.dominio.Equipo;
import com.atech.curso.m4.dominio.EquipoRepository;
import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.SalaRepository;
import com.atech.curso.m4.dominio.TipoSala;
import com.atech.curso.m4.web.SalaForm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaService {

    private final SalaRepository salas;
    private final EquipoRepository equipos;

    public SalaService(SalaRepository salas, EquipoRepository equipos) {
        this.salas = salas;
        this.equipos = equipos;
    }

    /** EJ 4.5 - Con {@code tipo} nulo devuelve todas las salas. */
    public List<Sala> listar(TipoSala tipo) {
        return tipo == null ? salas.findAllByOrderByNombreAsc() : salas.findByTipoOrderByNombreAsc(tipo);
    }

    public List<Equipo> listarEquipos() {
        return equipos.findAllByOrderByNombreAsc();
    }

    public Sala buscar(Long id) {
        return salas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sala", id));
    }

    public boolean existeNombre(String nombre, Long id) {
        if (id == null) {
            return salas.existsByNombreIgnoreCase(nombre);
        }
        return salas.existsByNombreIgnoreCaseAndIdNot(nombre, id);
    }

    @Transactional
    public Sala guardar(SalaForm form) {
        // EJ 4.5 - Los ids que llegan del formulario se convierten en entidades gestionadas
        Set<Equipo> equipamiento = new LinkedHashSet<>(equipos.findAllById(form.getEquipamiento()));
        if (form.getId() == null) {
            return salas.save(new Sala(form.getNombre(), form.getTipo(), form.getCapacidad(), form.isProyector(),
                    form.getEmailResponsable(), equipamiento));
        }
        Sala sala = buscar(form.getId());
        sala.actualizar(form.getNombre(), form.getTipo(), form.getCapacidad(), form.isProyector(),
                form.getEmailResponsable(), equipamiento);
        return sala;
    }
}
