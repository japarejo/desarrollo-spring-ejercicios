package com.atech.curso.m4.web;

import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.servicio.SalaService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * EJ 4.1 - Controlador MVC clásico con vistas JSP. Patrón Post/Redirect/Get con flash attributes.
 */
@Controller
@RequestMapping("/salas")
public class SalaWebController {

    static final String VISTA_LISTA = "salas/lista";
    static final String VISTA_FORM = "salas/formulario";

    private final SalaService salas;

    public SalaWebController(SalaService salas) {
        this.salas = salas;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("salas", salas.listar());
        return VISTA_LISTA;
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("sala", new SalaForm());
        return VISTA_FORM;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Sala sala = salas.buscar(id);
        SalaForm form = new SalaForm();
        form.setId(sala.getId());
        form.setNombre(sala.getNombre());
        form.setCapacidad(sala.getCapacidad());
        form.setProyector(sala.isProyector());
        model.addAttribute("sala", form);
        return VISTA_FORM;
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("sala") SalaForm form, BindingResult errores,
            RedirectAttributes redirect) {
        if (form.getId() == null && form.getNombre() != null && salas.existeNombre(form.getNombre())) {
            errores.rejectValue("nombre", "sala.nombre.duplicado", "Ya existe una sala con ese nombre");
        }
        if (errores.hasErrors()) {
            return VISTA_FORM;
        }
        Sala sala = salas.guardar(form);
        redirect.addFlashAttribute("mensaje", "Sala " + sala.getNombre() + " guardada");
        return "redirect:/salas";
    }
}
