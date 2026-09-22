package com.atech.curso.m7.usuarios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioLocalRepository extends JpaRepository<UsuarioLocal, Long> {

    Optional<UsuarioLocal> findByProveedorAndIdExterno(String proveedor, String idExterno);
}
