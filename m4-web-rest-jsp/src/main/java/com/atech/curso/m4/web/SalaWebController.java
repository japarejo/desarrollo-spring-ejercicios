package com.atech.curso.m4.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.atech.curso.m4.dominio.Equipo;
import com.atech.curso.m4.dominio.Sala;
import com.atech.curso.m4.dominio.TipoSala;
import com.atech.curso.m4.servicio.SalaService;

import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * EJ 4.1 - Controlador MVC clásico con vistas JSP. Patrón Post/Redirect/Get con flash attributes.
 */
@Controller
@RequestMapping("salas")
public class SalaWebController {

    static final String VISTA_LISTA = "salas/lista";
    static final String VISTA_FORM = "salas/formulario";

    private final SalaService salas;
    private final MessageSource mensajes;

    public SalaWebController(SalaService salas, MessageSource mensajes) {
        this.salas = salas;
        this.mensajes = mensajes;
    }

    // EJ 4.5 - Un método @ModelAttribute se ejecuta antes de CADA handler de este controlador: las opciones
    // de los desplegables están en el modelo también cuando el POST vuelve al formulario con errores
    @ModelAttribute("tipos")
    public TipoSala[] tipos() {
        return TipoSala.values();
    }

    @ModelAttribute("equipos")
    public List<Equipo> equipos() {
        return salas.listarEquipos();
    }

    // EJ 4.5 - ?tipo=FORMACION se convierte solo al enum; ?tipo= (vacío) llega como null
    @GetMapping
    public String listar(@RequestParam(required = false) TipoSala tipo, Model model) {
        model.addAttribute("salas", salas.listar(tipo));
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
        form.setTipo(sala.getTipo());
        form.setCapacidad(sala.getCapacidad());
        form.setProyector(sala.isProyector());
        form.setEmailResponsable(sala.getEmailResponsable());
        form.setEquipamiento(sala.getEquipamiento().stream().map(Equipo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        model.addAttribute("sala", form);
        return VISTA_FORM;
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("sala") SalaForm form, BindingResult errores,  RedirectAttributes redirect,
            Locale idioma) {
        if (form.getNombre() != null && salas.existeNombre(form.getNombre(), form.getId())) {
            errores.rejectValue("nombre", "sala.nombre.duplicado", "Ya existe una sala con ese nombre");
        }
        if (errores.hasErrors()) {
            return VISTA_FORM;
        }
        Sala sala = salas.guardar(form);
        // EJ 4.6 - Spring inyecta el Locale de la petición (el que decide el LocaleResolver)
        redirect.addFlashAttribute("mensaje",
                mensajes.getMessage("sala.guardada", new Object[] { sala.getNombre() }, idioma));
        return "redirect:/salas";
    }
}
