<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="sala.formulario"/></title>
    <link rel="stylesheet" href="<c:url value='/css/estilo.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/jsp/comun/cabecera.jspf" %>
<h1><spring:message code="sala.formulario"/></h1>

<%-- EJ 4.1 - Etiquetas form de Spring: enlazan con el objeto "sala" y muestran los errores de validación.
     EJ 4.5 - novalidate desactiva la validación del navegador (type="email") para ver la del servidor. --%>
<form:form modelAttribute="sala" method="post" action="${pageContext.request.contextPath}/salas" novalidate="novalidate">
    <form:hidden path="id"/>
    <form:errors path="*" element="div" cssClass="errores"/>

    <p>
        <form:label path="nombre">Nombre</form:label>
        <form:input path="nombre" cssErrorClass="error"/>
        <form:errors path="nombre" cssClass="error"/>
    </p>
    <p>
        <%-- EJ 4.5 - form:select + form:options: "tipos" lo pone en el modelo un método @ModelAttribute.
             Con un enum, el valor de cada opción es name() y la etiqueta, la propiedad itemLabel. --%>
        <form:label path="tipo">Tipo</form:label>
        <form:select path="tipo" cssErrorClass="error">
            <form:option value="" label="-- Elige el tipo --"/>
            <form:options items="${tipos}" itemLabel="descripcion"/>
        </form:select>
        <form:errors path="tipo" cssClass="error"/>
    </p>
    <p>
        <form:label path="capacidad">Capacidad</form:label>
        <form:input path="capacidad" type="number" cssErrorClass="error"/>
        <%-- EJ 4.5 - Aquí aparece también el error de @CapacidadSegunTipo, aunque se declare en la clase --%>
        <form:errors path="capacidad" cssClass="error"/>
    </p>
    <p>
        <form:label path="emailResponsable">Email del responsable</form:label>
        <form:input path="emailResponsable" type="email" cssErrorClass="error"/>
        <form:errors path="emailResponsable" cssClass="error"/>
    </p>
    <fieldset>
        <legend>Equipamiento</legend>
        <form:checkbox path="proyector" label="Proyector"/>
        <%-- EJ 4.5 - form:checkboxes: una casilla por equipo (entidad). itemValue="id" es lo que se envía y
             se compara con los ids del Set<Long> del formulario para marcar las casillas. --%>
        <form:checkboxes path="equipamiento" items="${equipos}" itemValue="id" itemLabel="nombre" element="span"/>
    </fieldset>
    <button type="submit">Guardar</button>
    <a href="<c:url value='/salas'/>">Cancelar</a>
</form:form>
</body>
</html>
