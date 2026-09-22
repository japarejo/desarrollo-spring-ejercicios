package com.atech.curso.m7.usuarios;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** EJ 7.1 - Alta automática en el primer acceso ("just-in-time provisioning"). */
@Service
public class ProvisionadorUsuarios {

    private final UsuarioLocalRepository usuarios;
    private final List<String> administradores;

    public ProvisionadorUsuarios(UsuarioLocalRepository usuarios,
            @Value("${atech.seguridad.admins:}") List<String> administradores) {
        this.usuarios = usuarios;
        this.administradores = administradores;
    }

    @Transactional
    public UsuarioLocal registrarAcceso(String proveedor, String idExterno, String email, String nombre) {
        return usuarios.findByProveedorAndIdExterno(proveedor, idExterno)
                .map(existente -> {
                    existente.registrarAcceso();
                    return existente;
                })
                .orElseGet(() -> usuarios.save(new UsuarioLocal(proveedor, idExterno, email, nombre,
                        email != null && administradores.contains(email) ? "ADMIN" : "USER")));
    }
}
