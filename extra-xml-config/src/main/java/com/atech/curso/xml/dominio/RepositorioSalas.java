package com.atech.curso.xml.dominio;

import java.util.List;
import java.util.Optional;

public interface RepositorioSalas {

    List<Sala> todas();

    Optional<Sala> porId(String id);
}
