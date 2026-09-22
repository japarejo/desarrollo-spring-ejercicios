package com.atech.curso.m7.api;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EJ 7.2 - API protegida con JWT. */
@RestController
@RequestMapping("/api")
public class ReservasApiController {

    public record NuevaReserva(String sala, LocalDateTime inicio) {
    }

    private final ReservaService servicio;

    public ReservasApiController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/publico/salas")
    public List<String> salas() {
        return List.of("Turing", "Lovelace", "Hopper");
    }

    /** Quién soy según el token. */
    @GetMapping("/me")
    public Map<String, Object> yo(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", auth.getName());
        datos.put("sub", jwt.getSubject());
        datos.put("emisor", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        datos.put("autoridades", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return datos;
    }

    @GetMapping("/reservas")
    public List<Reserva> listar() {
        return servicio.listar();
    }

    @GetMapping("/reservas/mias")
    public List<Reserva> mias() {
        return servicio.mias();
    }

    @PostMapping("/reservas")
    public ResponseEntity<Reserva> crear(@RequestBody NuevaReserva nueva, Authentication auth) {
        Reserva r = servicio.crear(nueva.sala(), nueva.inicio(), auth.getName());
        return ResponseEntity.created(URI.create("/api/reservas/" + r.id())).body(r);
    }

    @DeleteMapping("/reservas/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable long id) {
        servicio.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
