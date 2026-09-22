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
<h1><spring:message code="sala.formulario"/></h1>

<%-- EJ 4.1 - Etiquetas form de Spring: enlazan con el objeto "sala" y muestran los errores de validación --%>
<form:form modelAttribute="sala" method="post" action="${pageContext.request.contextPath}/salas">
    <form:hidden path="id"/>
    <form:errors path="*" element="div" cssClass="errores"/>

    <p>
        <form:label path="nombre">Nombre</form:label>
        <form:input path="nombre" cssErrorClass="error"/>
        <form:errors path="nombre" cssClass="error"/>
    </p>
    <p>
        <form:label path="capacidad">Capacidad</form:label>
        <form:input path="capacidad" type="number" cssErrorClass="error"/>
        <form:errors path="capacidad" cssClass="error"/>
    </p>
    <p>
        <form:checkbox path="proyector" label="Tiene proyector"/>
    </p>
    <button type="submit">Guardar</button>
    <a href="<c:url value='/salas'/>">Cancelar</a>
</form:form>
</body>
</html>
