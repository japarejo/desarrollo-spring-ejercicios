package com.atech.curso.xml.configuracionjava;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import com.atech.curso.xml.dominio.RepositorioSalas;
import com.atech.curso.xml.dominio.RepositorioSalasEnMemoria;
import com.atech.curso.xml.dominio.Sala;
import com.atech.curso.xml.reservas.BorradorReserva;
import com.atech.curso.xml.reservas.ServicioReservas;
import com.atech.curso.xml.tarifas.Tarifa;
import com.atech.curso.xml.tarifas.TarifaPlana;
import com.atech.curso.xml.tarifas.TarifaPorHora;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * El <b>mismo grafo de objetos</b> que {@code beans.xml}, con los mismos nombres de bean
 * (en configuración Java el nombre por defecto es el del método).
 *
 * <p>Diferencias que conviene señalar en clase:
 * <ul>
 *   <li><b>El compilador valida el cableado.</b> Si cambia la firma de un constructor, esto no
 *       compila; el XML seguiría pareciendo correcto y fallaría al arrancar.</li>
 *   <li><b>Refactorizar es seguro.</b> Renombrar una clase o moverla de paquete actualiza este
 *       fichero automáticamente; en XML son cadenas de texto que el IDE no siempre sigue.</li>
 *   <li><b>Es código normal.</b> Se pueden usar bucles, condicionales o constantes, mientras que
 *       en XML todo es declarativo y repetitivo.</li>
 *   <li>A cambio, el XML permitía <b>recablear sin recompilar</b>. Hoy esa necesidad se cubre mejor
 *       con perfiles, {@code @ConditionalOn...} y configuración externalizada (módulo 2).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionJava {

    /** Equivale a &lt;bean id="reloj" class="java.time.Clock" factory-method="systemUTC"/&gt; */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    /** Equivale al &lt;bean&gt; con &lt;constructor-arg&gt;&lt;list&gt; e init/destroy-method. */
    @Bean(initMethod = "inicializar", destroyMethod = "cerrar")
    RepositorioSalas repositorioSalas() {
        return new RepositorioSalasEnMemoria(List.of(
                new Sala("S-1", "Turing", 12),
                new Sala("S-2", "Lovelace", 30),
                new Sala("S-3", "Hopper", 6)));
    }

    /** @Primary es el equivalente de primary="true"; el setter sustituye a &lt;property&gt;. */
    @Bean
    @Primary
    Tarifa tarifaPorHora() {
        TarifaPorHora tarifa = new TarifaPorHora();
        tarifa.setPrecioHora(new BigDecimal("25.00"));
        return tarifa;
    }

    @Bean
    Tarifa tarifaPlana() {
        TarifaPlana tarifa = new TarifaPlana();
        tarifa.setPrecioDia(new BigDecimal("120.00"));
        return tarifa;
    }

    /**
     * Los parámetros se resuelven por tipo: al haber dos {@code Tarifa}, gana la marcada con
     * {@code @Primary}. En XML había que nombrarla explícitamente con {@code ref="tarifaPorHora"}.
     */
    @Bean
    ServicioReservas servicioReservas(RepositorioSalas repositorioSalas, Tarifa tarifa, Clock reloj) {
        return new ServicioReservas(repositorioSalas, tarifa, reloj);
    }

    /** Equivale a scope="prototype". */
    @Bean
    @Scope("prototype")
    BorradorReserva borradorReserva() {
        return new BorradorReserva();
    }
}
