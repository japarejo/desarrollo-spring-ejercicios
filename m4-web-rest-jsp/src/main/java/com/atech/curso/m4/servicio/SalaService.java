package com.atech.curso.m4.servicio;

import java.util.List;

import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.SalaRepository;
import com.atech.curso.m4.web.SalaForm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaService {

    private final SalaRepository salas;

    public SalaService(SalaRepository salas) {
        this.salas = salas;
    }

    public List<Sala> listar() {
        return salas.findAllByOrderByNombreAsc();
    }

    public Sala buscar(Long id) {
        return salas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sala", id));
    }

    public boolean existeNombre(String nombre) {
        return salas.existsByNombreIgnoreCase(nombre);
    }

    @Transactional
    public Sala guardar(SalaForm form) {
        if (form.getId() == null) {
            return salas.save(new Sala(form.getNombre(), form.getCapacidad(), form.isProyector()));
        }
        Sala sala = buscar(form.getId());
        sala.actualizar(form.getNombre(), form.getCapacidad(), form.isProyector());
        return sala;
    }
}
