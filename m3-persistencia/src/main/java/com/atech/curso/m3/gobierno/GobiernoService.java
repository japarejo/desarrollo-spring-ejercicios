package com.atech.curso.m3.gobierno;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * EJ 3.7 - La alternancia: el gobierno saliente cesa y se nombra un titular nuevo para cada órgano.
 * <p>
 * Es una operación <b>todo o nada</b>. Si al nombrar a cualquier titular estalla un escándalo
 * ({@link CasoplonException}, {@link CutreMasterException} o {@link JoyasOcultasException}), se deshace todo:
 * el gobierno entrante no llega a existir y el saliente sigue en el poder, con sus titulares de siempre.
 */
@Service
@Transactional(readOnly = true)
public class GobiernoService {

    private static final Logger log = LoggerFactory.getLogger(GobiernoService.class);

    /** Lo que hay en la base de datos en un momento dado: sirve para comparar antes y después. */
    public record Recuento(int legislaturaVigente, long gobiernos, long nombramientos) {
    }

    private final GobiernoRepository gobiernos;
    private final OrganoRepository organos;
    private final NombramientoRepository nombramientos;
    private final GeneradorNombresHeroicos generador;
    private final DetectorEscandalos detector;

    public GobiernoService(GobiernoRepository gobiernos, OrganoRepository organos,
            NombramientoRepository nombramientos, GeneradorNombresHeroicos generador,
            DetectorEscandalos detector) {
        this.gobiernos = gobiernos;
        this.organos = organos;
        this.nombramientos = nombramientos;
        this.generador = generador;
        this.detector = detector;
    }

    /**
     * La versión correcta. EscandaloException es comprobada, así que sin {@code rollbackFor}
     * Spring haría COMMIT al propagarse (ver {@link #alternanciaSinRollbackFor()}).
     */
    @Transactional(rollbackFor = EscandaloException.class)
    public Gobierno alternancia() throws EscandaloException {
        return formarGobierno();
    }

    /**
     * La versión chapucera, para verlo fallar: con un {@code @Transactional} a secas, una excepción
     * comprobada NO provoca rollback, y se hace commit de un gobierno a medias.
     */
    @Transactional
    public Gobierno alternanciaSinRollbackFor() throws EscandaloException {
        return formarGobierno();
    }

    private Gobierno formarGobierno() throws EscandaloException {
        LocalDateTime ahora = LocalDateTime.now();
        FinDeTransaccion fin = new FinDeTransaccion();
        TransactionSynchronizationManager.registerSynchronization(fin);

        // 1. El saliente cesa. Es una entidad gestionada: dirty checking, sin save()
        Optional<Gobierno> saliente = gobiernos.findByVigenteTrue();
        saliente.ifPresent(g -> {
            g.cesar(ahora);
            log.info("Cesa el gobierno de la legislatura {}", g.getLegislatura());
        });

        // 2. El entrante se guarda ya (IDENTITY hace el INSERT en el acto)...
        Gobierno entrante = gobiernos.save(new Gobierno(gobiernos.ultimaLegislatura() + 1, ahora));
        log.info("Toma posesión (provisional) el gobierno de la legislatura {}", entrante.getLegislatura());

        // 3. ...y se nombra un titular por órgano. Cualquier escándalo interrumpe el bucle
        List<Organo> puestos = organos.findAllByOrderByOrdenAsc();
        Set<String> usados = new HashSet<>();
        for (Organo organo : puestos) {
            String titular = generador.generarDistinto(usados);
            try {
                detector.investigar(titular, organo);
            } catch (EscandaloException e) {
                fin.escandalo = e;
                fin.nombrados = usados.size();
                log.warn("  {}. {} -> {}   ¡¡¡ ESCÁNDALO: {} !!!", organo.getOrden(), organo.getNombre(), titular,
                        e.getClass().getSimpleName());
                log.warn("La alternancia se interrumpe con {} de {} nombramientos ya insertados. La excepción sale"
                        + " del método @Transactional y el proxy decide: ¿commit o rollback?", usados.size(),
                        puestos.size());
                throw e;
            }
            entrante.nombrar(organo, titular); // se guarda en cascada con el gobierno
            // flush: fuerza el INSERT ahora para que se vea en el log que el rollback también lo deshace
            gobiernos.flush();
            usados.add(titular);
            log.info("  {}. {} -> {}", organo.getOrden(), organo.getNombre(), titular);
        }
        fin.nombrados = usados.size();
        log.info("Gobierno de la legislatura {} completo: {} nombramientos", entrante.getLegislatura(),
                usados.size());
        return entrante;
    }

    /**
     * Avisa en el log de cómo ha terminado <b>de verdad</b> la transacción: lo cuenta el propio gestor de
     * transacciones al acabar, no el código que llama. Así se ve la diferencia entre las dos alternancias.
     */
    private static final class FinDeTransaccion implements TransactionSynchronization {

        EscandaloException escandalo;
        int nombrados;

        @Override
        public void afterCompletion(int estado) {
            if (estado == STATUS_ROLLED_BACK) {
                log.warn("<<< ROLLBACK. Se deshacen el cese del saliente, el INSERT del entrante y sus {}"
                        + " nombramientos: la base de datos queda como estaba", nombrados);
            } else if (estado == STATUS_COMMITTED && escandalo != null) {
                log.warn("<<< COMMIT a pesar del escándalo ({} es comprobada y no hay rollbackFor). Queda guardado"
                        + " un gobierno A MEDIAS con {} nombramientos", escandalo.getClass().getSimpleName(),
                        nombrados);
            } else if (estado == STATUS_COMMITTED) {
                log.info("<<< COMMIT. El gobierno nuevo queda guardado con sus {} nombramientos", nombrados);
            }
        }
    }

    /** El gobierno en el poder con sus nombramientos ya cargados (se puede usar fuera de la transacción). */
    public Optional<Gobierno> vigente() {
        return gobiernos.buscarVigenteConNombramientos();
    }

    public List<ResumenGobierno> historico() {
        return gobiernos.historico();
    }

    public Recuento recuento() {
        int legislatura = gobiernos.findByVigenteTrue().map(Gobierno::getLegislatura).orElse(0);
        return new Recuento(legislatura, gobiernos.count(), nombramientos.count());
    }

    public long numeroDeOrganos() {
        return organos.count();
    }

    /** Probabilidad de que una alternancia completa salga bien con las probabilidades actuales. */
    public double probabilidadDeExito() {
        return detector.probabilidadDeExito((int) organos.count());
    }
}
