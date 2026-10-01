<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
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
        <form:label path="nombre"><spring:message code="sala.nombre"/></form:label>
        <form:input path="nombre" cssErrorClass="error"/>
        <form:errors path="nombre" cssClass="error"/>
    </p>
    <p>
        <%-- EJ 4.5 - form:select + form:option: "tipos" lo pone en el modelo un método @ModelAttribute.
             EJ 4.6 - En vez de form:options con itemLabel, un form:option por tipo con su texto traducido. --%>
        <form:label path="tipo"><spring:message code="sala.tipo"/></form:label>
        <form:select path="tipo" cssErrorClass="error">
            <form:option value=""><spring:message code="sala.tipo.elegir"/></form:option>
            <c:forEach var="t" items="${tipos}">
                <form:option value="${t}"><spring:message code="tipo.${t}"/></form:option>
            </c:forEach>
        </form:select>
        <form:errors path="tipo" cssClass="error"/>
    </p>
    <p>
        <form:label path="capacidad"><spring:message code="sala.capacidad"/></form:label>
        <form:input path="capacidad" type="number" cssErrorClass="error"/>
        <%-- EJ 4.5 - Aquí aparece también el error de @CapacidadSegunTipo, aunque se declare en la clase --%>
        <form:errors path="capacidad" cssClass="error"/>
    </p>
    <p>
        <form:label path="emailResponsable"><spring:message code="sala.emailResponsable"/></form:label>
        <form:input path="emailResponsable" type="email" cssErrorClass="error"/>
        <form:errors path="emailResponsable" cssClass="error"/>
    </p>
    <fieldset>
        <legend><spring:message code="sala.equipamiento"/></legend>
        <%-- EJ 4.6 - El atributo label no admite etiquetas dentro: el texto se guarda antes en una variable --%>
        <spring:message code="sala.proyector" var="textoProyector"/>
        <form:checkbox path="proyector" label="${textoProyector}"/>
        <%-- EJ 4.5 - form:checkboxes: una casilla por equipo (entidad). itemValue="id" es lo que se envía y
             se compara con los ids del Set<Long> del formulario para marcar las casillas. --%>
        <form:checkboxes path="equipamiento" items="${equipos}" itemValue="id" itemLabel="nombre" element="span"/>
    </fieldset>
    <button type="submit"><spring:message code="sala.guardar"/></button>
    <a href="<c:url value='/salas'/>"><spring:message code="sala.cancelar"/></a>
</form:form>
</body>
</html>
