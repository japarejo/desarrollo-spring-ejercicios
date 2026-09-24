package com.atech.curso.m1.pista;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

/**
 * EJ 1.7 - La auditabilidad como aspecto.
 *
 * <p>Compárese con {@code AuditoriaAspect} (EJ 1.4):
 * <ul>
 *   <li>aquél usa <b>un único {@code @Around}</b> porque necesita <em>rodear</em> la llamada para
 *       cronometrarla;</li>
 *   <li>éste usa <b>{@code @AfterReturning} y {@code @AfterThrowing}</b>, que expresan mejor la
 *       intención: registrar el desenlace. Al no envolver la ejecución, el aspecto no puede
 *       tragarse la excepción ni alterar el valor devuelto por descuido.</li>
 * </ul>
 *
 * <p>El aspecto obtiene por su cuenta el <em>quién</em> ({@link UsuarioActual}) y el <em>cuándo</em>
 * ({@link Clock}): el servicio de negocio no recibe ni propaga esos datos. Ése es justamente el
 * beneficio de la POA: sin ella, cada método tendría que repetir el mismo bloque de registro.
 *
 * <p><b>Atención:</b> como todo aspecto basado en proxies, no intercepta las auto-invocaciones
 * ({@code this.metodo()}) ni los métodos no públicos. Véase el EJ 1.5.
 */
@Aspect
@Component
public class PistaAuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(PistaAuditoriaAspect.class);

    private final LibroAuditoria libro;
    private final UsuarioActual usuario;
    private final Clock clock;

    public PistaAuditoriaAspect(LibroAuditoria libro, UsuarioActual usuario, Clock clock) {
        this.libro = libro;
        this.usuario = usuario;
        this.clock = clock;
    }

    /** El parámetro {@code pista} se enlaza con la anotación encontrada en el método interceptado. */
    @AfterReturning("@annotation(pista)")
    public void operacionCorrecta(JoinPoint jp, PistaAuditoria pista) {
        anotar(jp, pista, ApunteAuditoria.OK, null);
    }

    @AfterThrowing(value = "@annotation(pista)", throwing = "error")
    public void operacionFallida(JoinPoint jp, PistaAuditoria pista, Throwable error) {
        anotar(jp, pista, ApunteAuditoria.ERROR, error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    private void anotar(JoinPoint jp, PistaAuditoria pista, String resultado, String detalle) {
        Method metodo = metodoReal(jp);
        Parameter[] parametros = metodo.getParameters();
        Object[] argumentos = jp.getArgs();

        String idEntidad = null;
        Map<String, String> datos = new LinkedHashMap<>();
        for (int i = 0; i < parametros.length; i++) {
            Parameter p = parametros[i];
            if (p.isAnnotationPresent(IdEntidad.class)) {
                idEntidad = String.valueOf(argumentos[i]);
            }
            else if (p.isAnnotationPresent(Sensible.class)) {
                datos.put(p.getName(), enmascarar(argumentos[i]));
            }
            else {
                datos.put(p.getName(), String.valueOf(argumentos[i]));
            }
        }

        ApunteAuditoria apunte = new ApunteAuditoria(clock.instant(), usuario.nombre(), pista.accion(),
                pista.entidad(), idEntidad, Map.copyOf(datos), resultado, detalle);
        libro.anotar(apunte);
        log.info("PISTA usuario={} accion={} {}#{} resultado={} datos={}", apunte.usuario(), apunte.accion(),
                apunte.entidad(), apunte.idEntidad(), apunte.resultado(), apunte.datos());
    }

    /**
     * Con un proxy JDK la firma del <em>join point</em> puede ser la del interfaz, que no lleva las
     * anotaciones de los parámetros. Resolvemos siempre el método de la clase destino.
     */
    private Method metodoReal(JoinPoint jp) {
        Method metodo = ((MethodSignature) jp.getSignature()).getMethod();
        Object destino = jp.getTarget();
        return destino == null ? metodo : AopUtils.getMostSpecificMethod(metodo, destino.getClass());
    }

    /** Deja visibles solo los últimos 4 caracteres: {@code ****6789}. */
    private String enmascarar(Object valor) {
        String texto = String.valueOf(valor);
        if (texto.length() <= 4) {
            return "****";
        }
        return "****" + texto.substring(texto.length() - 4);
    }
}
