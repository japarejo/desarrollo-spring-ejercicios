package com.atech.curso.m3.consola;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.atech.curso.m3.dominio.EstadoReserva;
import com.atech.curso.m3.dominio.Reserva;
import com.atech.curso.m3.dominio.Sala;
import com.atech.curso.m3.dominio.Usuario;
import com.atech.curso.m3.gobierno.DetectorEscandalos;
import com.atech.curso.m3.gobierno.EscandaloException;
import com.atech.curso.m3.gobierno.GeneradorNombresHeroicos;
import com.atech.curso.m3.gobierno.Gobierno;
import com.atech.curso.m3.gobierno.GobiernoService;
import com.atech.curso.m3.gobierno.Nombramiento;
import com.atech.curso.m3.gobierno.NombramientoRepository;
import com.atech.curso.m3.gobierno.ProbabilidadesEscandalo;
import com.atech.curso.m3.gobierno.ResumenGobierno;
import com.atech.curso.m3.repositorio.OcupacionSala;
import com.atech.curso.m3.repositorio.ReservaRepository;
import com.atech.curso.m3.repositorio.ReservaResumen;
import com.atech.curso.m3.repositorio.ReservaSpecs;
import com.atech.curso.m3.repositorio.SalaRepository;
import com.atech.curso.m3.repositorio.UsuarioRepository;
import com.atech.curso.m3.servicio.ReservaService;
import com.atech.curso.m3.servicio.ReservaService.Solicitud;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Menú de texto para usar la aplicación: reservas de salas y el gobierno de superhéroes (EJ 3.7).
 * Cada opción indica qué repositorio o servicio usa, para seguir en el log el SQL y las transacciones.
 * <p>
 * Se desactiva con {@code m3.consola.activa=false} (los tests lo hacen en {@code src/test/resources/config}).
 * La consola no es transaccional: todo lo que muestra llega ya cargado de los repositorios y servicios.
 */
@Component
@ConditionalOnProperty(name = "m3.consola.activa", havingValue = "true", matchIfMissing = true)
public class ConsolaInteractiva implements CommandLineRunner {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ENTRADA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String LOG_SQL = "org.hibernate.SQL";
    private static final String LOG_TX = "org.springframework.orm.jpa.JpaTransactionManager";
    private static final int TAMANO_PAGINA = 4;

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private record Opcion(String texto, String usa, Accion accion) {
    }

    /** Se lanza al cerrarse la entrada estándar (Ctrl+D / Ctrl+Z o entrada redirigida que se acaba). */
    private static final class FinDeEntrada extends RuntimeException {
    }

    private final SalaRepository salas;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final ReservaService reservaService;
    private final GobiernoService gobiernoService;
    private final NombramientoRepository nombramientos;
    private final DetectorEscandalos detector;
    private final GeneradorNombresHeroicos generador;
    private final LoggingSystem logging;

    private final PrintStream out = System.out;
    private final Scanner entrada = new Scanner(System.in);
    private final Map<String, Opcion> opciones = new LinkedHashMap<>();

    public ConsolaInteractiva(SalaRepository salas, UsuarioRepository usuarios, ReservaRepository reservas,
            ReservaService reservaService, GobiernoService gobiernoService, NombramientoRepository nombramientos,
            DetectorEscandalos detector, GeneradorNombresHeroicos generador, LoggingSystem logging) {
        this.salas = salas;
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.reservaService = reservaService;
        this.gobiernoService = gobiernoService;
        this.nombramientos = nombramientos;
        this.detector = detector;
        this.generador = generador;
        this.logging = logging;

        opciones.put("1", new Opcion("Salas y usuarios", "SalaRepository.findAll, UsuarioRepository.findAll",
                this::salasYUsuarios));
        opciones.put("2", new Opcion("Buscar reservas (filtros opcionales, paginado)",
                "ReservaService.buscar: Specifications + @EntityGraph", this::buscarReservas));
        opciones.put("3", new Opcion("Reservas de un usuario",
                "findByUsuarioEmailOrderByInicioAsc: proyección por interfaz", this::reservasDeUsuario));
        opciones.put("4", new Opcion("Ocupación por sala", "ocupacionPorSala: proyección DTO",
                this::ocupacion));
        opciones.put("5", new Opcion("Nueva reserva", "ReservaService.reservar: @Transactional",
                this::nuevaReserva));
        opciones.put("6", new Opcion("Cancelar una reserva", "ReservaService.cancelar: dirty checking",
                this::cancelarReserva));
        opciones.put("7", new Opcion("Ver el gobierno vigente", "buscarVigenteConNombramientos: @EntityGraph",
                this::verGobierno));
        opciones.put("8", new Opcion("Histórico de gobiernos", "GobiernoRepository.historico: DTO con count",
                this::historico));
        opciones.put("9", new Opcion("¡Alternancia! (todo o nada)", "@Transactional(rollbackFor = ...)",
                () -> alternancia(true)));
        opciones.put("10", new Opcion("Alternancia chapucera", "@Transactional sin rollbackFor",
                () -> alternancia(false)));
        opciones.put("11", new Opcion("Trayectoria de un superhéroe", "NombramientoRepository.trayectoria",
                this::trayectoria));
        opciones.put("12", new Opcion("Probabilidades de escándalo (ver y ajustar)", "DetectorEscandalos",
                this::probabilidades));
        opciones.put("13", new Opcion("Generar nombres de superhéroe", "GeneradorNombresHeroicos",
                this::generarNombres));
        opciones.put("14", new Opcion("Traza de SQL (Hibernate)", LOG_SQL, () -> alternarLog(LOG_SQL)));
        opciones.put("15", new Opcion("Traza de transacciones", LOG_TX, () -> alternarLog(LOG_TX)));
    }

    @Override
    public void run(String... args) {
        out.println("""

                ================================================================
                 M3 · Persistencia con Spring Data JPA · aplicación de consola
                ================================================================
                 Cada opción dice qué repositorio o servicio usa: mira el log.
                 Consejo: activa la traza de transacciones (15) y lanza una
                 alternancia (9) hasta que estalle un escándalo.""");
        try {
            while (true) {
                mostrarMenu();
                String opcion = leer("Opción");
                if (opcion.equals("0")) {
                    break;
                }
                if (opcion.isEmpty()) {
                    continue;
                }
                Opcion elegida = opciones.get(opcion);
                if (elegida == null) {
                    out.println("Opción desconocida: " + opcion);
                    continue;
                }
                out.println();
                out.println(">> " + elegida.texto() + "   [" + elegida.usa() + "]");
                try {
                    elegida.accion().ejecutar();
                } catch (FinDeEntrada e) {
                    throw e;
                } catch (Exception e) {
                    out.println("[ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (FinDeEntrada e) {
            out.println();
        }
        out.println("¡Hasta la próxima legislatura!");
    }

    private void mostrarMenu() {
        out.println();
        out.println("--- Reservas de salas ----------------------------------------");
        linea("1", "2", "3", "4", "5", "6");
        out.println("--- Gobierno de superhéroes (transacciones) -------------------");
        linea("7", "8", "9", "10", "11", "12", "13");
        out.println("--- Trazas ------------------------------------------------------");
        out.printf("  14. %-44s [%s]%n", opciones.get("14").texto(), activo(LOG_SQL) ? "ON" : "off");
        out.printf("  15. %-44s [%s]%n", opciones.get("15").texto(), activo(LOG_TX) ? "ON" : "off");
        out.println("   0. Salir");
    }

    private void linea(String... claves) {
        for (String clave : claves) {
            out.printf("  %2s. %s%n", clave, opciones.get(clave).texto());
        }
    }

    // ---------------------------------------------------------------- Reservas de salas

    private void salasYUsuarios() {
        out.println("Salas:");
        for (Sala s : salas.findAll(Sort.by("nombre"))) {
            out.printf("  %-10s %3d plazas  %s (%s)%n", s.getNombre(), s.getCapacidad(),
                    s.getDireccion().calle(), s.getDireccion().ciudad());
        }
        out.println("Usuarios:");
        for (Usuario u : usuarios.findAll(Sort.by("email"))) {
            out.printf("  %-18s %s%n", u.getEmail(), u.getNombre());
        }
    }

    private void buscarReservas() {
        out.println("Deja en blanco los filtros que no quieras aplicar.");
        String sala = opcional(leer("Sala"));
        String ciudad = opcional(leer("Ciudad"));
        String estado = leer("Estado (C = confirmada, X = cancelada)").toUpperCase(Locale.ROOT);
        EstadoReserva filtroEstado = switch (estado) {
            case "C" -> EstadoReserva.CONFIRMADA;
            case "X" -> EstadoReserva.CANCELADA;
            default -> null;
        };
        var filtro = new ReservaSpecs.Filtro(sala, ciudad, filtroEstado, null, null);

        int numero = 0;
        while (true) {
            Page<Reserva> pagina = reservaService.buscar(filtro,
                    PageRequest.of(numero, TAMANO_PAGINA, Sort.by("inicio")));
            if (pagina.isEmpty()) {
                out.println("No hay reservas con esos filtros.");
                return;
            }
            out.printf("Página %d de %d (%d reservas en total)%n", numero + 1, pagina.getTotalPages(),
                    pagina.getTotalElements());
            pagina.forEach(this::imprimir);
            if (!pagina.hasNext() || !leer("[Enter] siguiente página, [q] terminar").isEmpty()) {
                return;
            }
            numero++;
        }
    }

    private void reservasDeUsuario() {
        String email = leer("Email (p. ej. ana@atech.es)");
        List<ReservaResumen> suyas = reservas.findByUsuarioEmailOrderByInicioAsc(email);
        if (suyas.isEmpty()) {
            out.println("Sin reservas para " + email);
        }
        suyas.forEach(r -> out.printf("  #%-3d %s  %s%n", r.getId(), FECHA.format(r.getInicio()),
                r.getSala().getNombre()));
    }

    private void ocupacion() {
        for (OcupacionSala o : reservas.ocupacionPorSala()) {
            out.printf("  %-10s %3d %s%n", o.sala(), o.reservas(), "#".repeat((int) o.reservas()));
        }
    }

    private void nuevaReserva() {
        String sala = leer("Sala (Turing, Lovelace, Hopper)");
        String email = leer("Email");
        LocalDateTime inicio = fecha(leer("Inicio (aaaa-mm-dd hh:mm)"));
        LocalDateTime fin = fecha(leer("Fin    (aaaa-mm-dd hh:mm)"));
        Reserva r = reservaService.reservar(new Solicitud(sala, email, inicio, fin));
        out.println("[OK] Reserva creada con id " + r.getId());
    }

    private void cancelarReserva() {
        Long id = Long.valueOf(leer("Id de la reserva"));
        Reserva r = reservaService.cancelar(id);
        out.println("[OK] Reserva " + r.getId() + " -> " + r.getEstado()
                + " (sin llamar a save: mira el UPDATE en el log)");
    }

    private void imprimir(Reserva r) {
        out.printf("  #%-3d %s - %s  %-9s %-16s %s%n", r.getId(), FECHA.format(r.getInicio()),
                FECHA.format(r.getFin()).substring(11), r.getSala().getNombre(), r.getUsuario().getEmail(),
                r.getEstado());
    }

    // ---------------------------------------------------------------- Gobierno de superhéroes

    private void verGobierno() {
        gobiernoService.vigente().ifPresentOrElse(this::imprimir, () -> out.println("No hay gobierno vigente."));
    }

    private void imprimir(Gobierno g) {
        long total = gobiernoService.numeroDeOrganos();
        out.printf("Gobierno de la legislatura %d (desde %s) · %d de %d cargos%s%n", g.getLegislatura(),
                FECHA.format(g.getTomaPosesion()), g.getNombramientos().size(), total,
                g.getNombramientos().size() < total ? "  <<< ¡GOBIERNO A MEDIAS!" : "");
        for (Nombramiento n : g.getNombramientos()) {
            out.printf("  %2d. %-62s %s%n", n.getOrgano().getOrden(), n.getOrgano().getNombre(), n.getTitular());
        }
    }

    private void historico() {
        long total = gobiernoService.numeroDeOrganos();
        for (ResumenGobierno r : gobiernoService.historico()) {
            out.printf("  Legislatura %-3d %s -> %-16s %2d/%d cargos %s%n", r.legislatura(),
                    FECHA.format(r.tomaPosesion()), r.cese() == null ? "..." : FECHA.format(r.cese()),
                    r.cargos(), total, r.vigente() ? "(vigente)" : "");
        }
    }

    private void alternancia(boolean conRollbackFor) {
        long organos = gobiernoService.numeroDeOrganos();
        if (conRollbackFor) {
            out.println("""
                    Versión TODO O NADA: @Transactional(rollbackFor = EscandaloException.class).
                    Si estalla un escándalo, Spring hace ROLLBACK y la base de datos queda como estaba.""");
        } else {
            out.println("""
                    Versión CHAPUCERA: @Transactional a secas. EscandaloException es comprobada, y Spring sólo
                    deshace con RuntimeException y Error: si estalla un escándalo, hace COMMIT de lo que haya.""");
        }
        out.println("""
                Sin escándalos, las dos versiones hacen exactamente lo mismo: la diferencia sólo se ve cuando
                algo falla. Para no depender de la suerte, puedes filtrar un escándalo a la prensa.""");
        String orden = leer(String.format("Nº de órgano donde estallará seguro (1-%d; 5 = Defensa). Enter = al azar"
                + " [%.1f %% de salir bien]", organos,
                gobiernoService.probabilidadDeExito() * 100));
        if (!orden.isEmpty()) {
            int n = Integer.parseInt(orden);
            if (n < 1 || n > organos) {
                throw new IllegalArgumentException("No hay ningún órgano con el número " + n);
            }
            detector.filtrarChivatazo(n);
        }

        GobiernoService.Recuento antes = gobiernoService.recuento();
        out.println();
        out.println("====================== ARRANCA LA ALTERNANCIA (sigue el log) ======================");
        EscandaloException escandalo = null;
        try {
            if (conRollbackFor) {
                gobiernoService.alternancia();
            } else {
                gobiernoService.alternanciaSinRollbackFor();
            }
        } catch (EscandaloException e) {
            escandalo = e;
        } finally {
            detector.olvidarChivatazo();
        }
        GobiernoService.Recuento despues = gobiernoService.recuento();

        out.println("====================== RESULTADO ==================================================");
        if (escandalo == null) {
            out.println("  Sin escándalos: COMMIT y gobierno nuevo completo.");
        } else {
            out.println("  ¡ESCÁNDALO! " + escandalo.getClass().getSimpleName());
            out.println("  " + escandalo.getMessage());
        }
        out.println();
        out.println("  En la base de datos      ANTES   DESPUÉS");
        out.printf("  Legislatura vigente   %8d  %8d%n", antes.legislaturaVigente(), despues.legislaturaVigente());
        out.printf("  Filas en gobierno     %8d  %8d%n", antes.gobiernos(), despues.gobiernos());
        out.printf("  Filas en nombramiento %8d  %8d%n", antes.nombramientos(), despues.nombramientos());
        out.println();
        if (escandalo == null) {
            out.printf("  +1 gobierno y +%d nombramientos: el cambio completo.%n",
                    despues.nombramientos() - antes.nombramientos());
        } else if (despues.equals(antes)) {
            out.println("  ROLLBACK: la base de datos está EXACTAMENTE igual que antes. El cese del saliente, el");
            out.println("  gobierno entrante y los nombramientos que llegaron a insertarse se han deshecho.");
            out.println("  Sigue en el poder la legislatura " + antes.legislaturaVigente() + " con sus titulares.");
        } else {
            out.printf("  COMMIT DE UN GOBIERNO A MEDIAS: +1 gobierno con sólo %d de %d nombramientos, y el%n",
                    despues.nombramientos() - antes.nombramientos(), organos);
            out.println("  saliente cesado. Esto es lo que pasa con una excepción comprobada sin rollbackFor.");
        }
        out.println("===================================================================================");
        out.println();
        verGobierno();
    }

    private void trayectoria() {
        String texto = leer("Nombre o parte del nombre (p. ej. Holístico)");
        List<NombramientoRepository.Trayectoria> t = nombramientos.trayectoria(texto);
        if (t.isEmpty()) {
            out.println("Ningún superhéroe con ese nombre ha pasado por el gobierno.");
        }
        t.forEach(n -> out.printf("  Legislatura %-3d %-22s %s%n", n.getLegislatura(), n.getTitular(),
                n.getOrgano()));
    }

    private void probabilidades() {
        ProbabilidadesEscandalo p = detector.getProbabilidades();
        long organos = gobiernoService.numeroDeOrganos();
        out.printf("Por cada nombramiento: Casoplón %.3f · CutreMaster %.3f · Joyas ocultas %.3f  (total %.3f)%n",
                p.casoplon(), p.cutreMaster(), p.joyasOcultas(), p.total());
        out.printf("Con %d órganos, P(alternancia sin escándalos) = (1 - %.3f)^%d = %.1f %%%n", organos, p.total(),
                organos, gobiernoService.probabilidadDeExito() * 100);
        if (!leer("¿Cambiarlas? [s/N]").equalsIgnoreCase("s")) {
            return;
        }
        detector.ajustar(new ProbabilidadesEscandalo(
                probabilidad("Casoplón", p.casoplon()),
                probabilidad("CutreMaster", p.cutreMaster()),
                probabilidad("Joyas ocultas", p.joyasOcultas())));
        out.printf("[OK] Nueva probabilidad de éxito: %.1f %%%n", gobiernoService.probabilidadDeExito() * 100);
    }

    private void generarNombres() {
        Set<String> usados = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            String nombre = generador.generarDistinto(usados);
            usados.add(nombre);
            out.println("  " + nombre);
        }
        out.println("(" + generador.combinacionesPosibles() + " combinaciones posibles)");
    }

    // ---------------------------------------------------------------- Trazas y entrada

    private boolean activo(String logger) {
        LogLevel nivel = logging.getLoggerConfiguration(logger).getEffectiveLevel();
        return nivel.ordinal() <= LogLevel.DEBUG.ordinal();
    }

    private void alternarLog(String logger) {
        boolean ahora = !activo(logger);
        logging.setLogLevel(logger, ahora ? LogLevel.DEBUG : LogLevel.INFO);
        out.println("Traza de " + logger + (ahora ? " activada" : " desactivada"));
    }

    private String leer(String mensaje) {
        out.print(mensaje + ": ");
        out.flush();
        if (!entrada.hasNextLine()) {
            throw new FinDeEntrada();
        }
        return entrada.nextLine().trim();
    }

    private static String opcional(String valor) {
        return valor.isEmpty() ? null : valor;
    }

    private static LocalDateTime fecha(String texto) {
        try {
            return LocalDateTime.parse(texto, ENTRADA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida, usa el formato 2030-01-10 09:00");
        }
    }

    private double probabilidad(String nombre, double actual) {
        String valor = leer(String.format(Locale.ROOT, "%s [%.3f]", nombre, actual));
        return valor.isEmpty() ? actual : Double.parseDouble(valor.replace(',', '.'));
    }
}
