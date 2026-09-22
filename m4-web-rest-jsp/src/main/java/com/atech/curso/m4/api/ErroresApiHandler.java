package com.atech.curso.m4.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.atech.curso.m4.servicio.RecursoNoEncontradoException;
import com.atech.curso.m4.servicio.ReglaNegocioException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * EJ 4.2 - Errores de la API en formato ProblemDetail (RFC 9457). Sólo se aplica a los
 * controladores del paquete api; las vistas JSP siguen usando la página de error normal.
 */
@RestControllerAdvice(basePackageClasses = ReservaRestController.class)
public class ErroresApiHandler extends ResponseEntityExceptionHandler {

    private static final String BASE = "https://api.atech.es/problemas/";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(BASE + "no-encontrado"));
        pd.setTitle("Recurso no encontrado");
        return pd;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create(BASE + "regla-negocio"));
        pd.setTitle("Regla de negocio incumplida");
        return pd;
    }

    /** Añade al ProblemDetail estándar el detalle de los campos erróneos. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = ex.getBody();
        pd.setType(URI.create(BASE + "validacion"));
        pd.setTitle("Datos no válidos");
        pd.setProperty("errores", errores);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }
}
