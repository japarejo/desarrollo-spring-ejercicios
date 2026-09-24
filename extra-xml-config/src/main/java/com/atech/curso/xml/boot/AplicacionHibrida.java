package com.atech.curso.xml.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * El camino realista de migración: una aplicación Spring Boot moderna que <b>reutiliza</b> un
 * {@code beans.xml} heredado mediante {@code @ImportResource}.
 *
 * <p>Permite migrar por partes: los beans nuevos se escriben con anotaciones y los antiguos siguen
 * en XML hasta que toque traducirlos. Los dos estilos conviven en el mismo contenedor y se inyectan
 * entre sí sin saber de dónde viene cada uno (véase {@link InformeArranque}).
 *
 * <p>Esta clase vive en su propio paquete a propósito: {@code @SpringBootApplication} escanea desde
 * aquí, así que <b>no</b> recoge {@code ConfiguracionJava}. De lo contrario habría dos definiciones
 * para cada bean y el contexto fallaría al arrancar.
 */
@SpringBootApplication
@ImportResource("classpath:beans.xml")
public class AplicacionHibrida {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionHibrida.class, args);
    }
}
