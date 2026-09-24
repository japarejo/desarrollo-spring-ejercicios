package com.atech.curso.xml.dominio;

import java.util.List;
import java.util.Optional;

/**
 * Se configura por <b>constructor</b> y tiene métodos de ciclo de vida con nombres arbitrarios.
 *
 * <p>Ni {@code inicializar} ni {@code cerrar} llevan {@code @PostConstruct}/{@code @PreDestroy}:
 * se declaran desde fuera con {@code init-method}/{@code destroy-method} en XML o con
 * {@code @Bean(initMethod=..., destroyMethod=...)} en Java. Es el caso típico de una clase de
 * terceros que no podemos anotar.
 */
public class RepositorioSalasEnMemoria implements RepositorioSalas {

    private final List<Sala> salas;

    private boolean iniciado;
    private boolean cerrado;

    public RepositorioSalasEnMemoria(List<Sala> salas) {
        this.salas = List.copyOf(salas);
    }

    public void inicializar() {
        this.iniciado = true;
    }

    public void cerrar() {
        this.cerrado = true;
    }

    public boolean iniciado() {
        return iniciado;
    }

    public boolean cerrado() {
        return cerrado;
    }

    @Override
    public List<Sala> todas() {
        return salas;
    }

    @Override
    public Optional<Sala> porId(String id) {
        return salas.stream().filter(s -> s.id().equals(id)).findFirst();
    }
}
