package com.atech.curso.m7.usuarios;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** EJ 7.1 - Copia local del usuario externo (Google/GitHub) con los datos propios de la aplicación. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "proveedor", "id_externo" }))
public class UsuarioLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String proveedor;

    @Column(name = "id_externo", nullable = false)
    private String idExterno;

    private String email;
    private String nombre;

    @Column(nullable = false)
    private String rol;

    private Instant alta;
    private Instant ultimoAcceso;
    private int accesos;

    protected UsuarioLocal() {
    }

    public UsuarioLocal(String proveedor, String idExterno, String email, String nombre, String rol) {
        this.proveedor = proveedor;
        this.idExterno = idExterno;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.alta = Instant.now();
        registrarAcceso();
    }

    public void registrarAcceso() {
        this.ultimoAcceso = Instant.now();
        this.accesos++;
    }

    public Long getId() {
        return id;
    }

    public String getProveedor() {
        return proveedor;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public Instant getAlta() {
        return alta;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }

    public int getAccesos() {
        return accesos;
    }
}
