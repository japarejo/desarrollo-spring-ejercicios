package com.atech.curso.m4.api;

import java.net.URI;

import com.atech.curso.m4.servicio.ReservaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * EJ 4.2 - API REST de reservas. La versión va en la URI (/api/v1); en Spring Boot 4
 * se puede usar el versionado nativo (cabecera API-Version, ver README).
 */
@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas", description = "Gestión de reservas de salas")
public class ReservaRestController {

    private final ReservaService servicio;

    public ReservaRestController(ReservaService servicio) {
        this.servicio = servicio;
    }

    /** EJ 4.3 - Paginación y ordenación: ?page=0&size=10&sort=inicio,desc */
    @GetMapping
    @Operation(summary = "Lista paginada de reservas")
    public PagedModel<ReservaResponse> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "inicio", direction = Sort.Direction.ASC) Pageable pagina) {
        return new PagedModel<>(servicio.listar(pagina));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una reserva")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    @ApiResponse(responseCode = "404", description = "No existe la reserva")
    public ReservaResponse buscar(@Parameter(description = "Identificador") @PathVariable Long id) {
        return servicio.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Crea una reserva")
    @ApiResponse(responseCode = "201", description = "Reserva creada; cabecera Location con su URI")
    @ApiResponse(responseCode = "400", description = "Datos no válidos (ProblemDetail)")
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest peticion) {
        ReservaResponse creada = servicio.crear(peticion);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica una reserva")
    public ReservaResponse modificar(@PathVariable Long id, @Valid @RequestBody ReservaRequest peticion) {
        return servicio.modificar(id, peticion);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borra una reserva")
    @ApiResponse(responseCode = "204", description = "Reserva borrada")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        servicio.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
