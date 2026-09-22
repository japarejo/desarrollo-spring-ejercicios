package com.atech.curso.m1.auditoria;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EJ 1.4 - Aspecto {@code @Around}: mide el tiempo y registra si la ejecución terminó bien.
 * El parámetro {@code auditado} se enlaza con la anotación del método interceptado.
 */
@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final RegistroAuditoria registro;

    public AuditoriaAspect(RegistroAuditoria registro) {
        this.registro = registro;
    }

    @Around("@annotation(auditado)")
    public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
        String metodo = pjp.getSignature().toShortString();
        String operacion = auditado.value().isBlank() ? metodo : auditado.value();
        long inicio = System.nanoTime();
        boolean correcta = false;
        try {
            Object resultado = pjp.proceed();
            correcta = true;
            return resultado;
        }
        finally {
            Duration duracion = Duration.ofNanos(System.nanoTime() - inicio);
            registro.registrar(new RegistroAuditoria.Entrada(operacion, metodo, duracion, correcta));
            log.info("AUDITORIA op={} metodo={} ok={} ms={}", operacion, metodo, correcta, duracion.toMillis());
        }
    }
}
