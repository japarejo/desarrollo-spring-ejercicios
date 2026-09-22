package com.atech.curso.m3.repositorio;

import java.util.Optional;

import com.atech.curso.m3.dominio.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
