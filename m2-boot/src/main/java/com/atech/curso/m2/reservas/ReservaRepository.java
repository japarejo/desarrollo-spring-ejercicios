package com.atech.curso.m2.reservas;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countBySala(String sala);
}
