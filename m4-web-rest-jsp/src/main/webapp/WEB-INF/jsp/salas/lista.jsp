<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- EJ 4.1 - JSTL 3.0: los URI pasan a ser jakarta.tags.* --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="salas.titulo"/></title>
    <link rel="stylesheet" href="<c:url value='/css/estilo.css'/>">
</head>
<body>
<%@ include file="/WEB-INF/jsp/comun/cabecera.jspf" %>
<h1><spring:message code="salas.titulo"/></h1>

<c:if test="${not empty mensaje}">
    <p class="ok"><c:out value="${mensaje}"/></p>
</c:if>

<%-- EJ 4.5 - Filtro por GET: ${param.tipo} es el parámetro de la petición y conserva la opción elegida --%>
<form method="get" action="<c:url value='/salas'/>" class="filtro">
    <label for="tipo"><spring:message code="lista.filtro.tipo"/></label>
    <select id="tipo" name="tipo">
        <option value=""><spring:message code="lista.filtro.todos"/></option>
        <c:forEach var="t" items="${tipos}">
            <%-- EJ 4.6 - La clave del mensaje se construye con el valor del enum: tipo.REUNIONES... --%>
            <option value="${t}" ${param.tipo == t.name() ? 'selected' : ''}><spring:message code="tipo.${t}"/></option>
        </c:forEach>
    </select>
    <button type="submit"><spring:message code="lista.filtro.boton"/></button>
    <c:if test="${not empty param.tipo}">
        <a href="<c:url value='/salas'/>"><spring:message code="lista.filtro.quitar"/></a>
    </c:if>
</form>

<c:choose>
    <c:when test="${empty salas}">
        <p><spring:message code="${empty param.tipo ? 'lista.vacia' : 'lista.vacia.tipo'}"/></p>
    </c:when>
    <c:otherwise>
        <%-- EJ 4.5 - c:set como acumulador: se va sumando en cada vuelta del bucle --%>
        <c:set var="aforoTotal" value="0"/>
        <table>
            <thead>
            <tr><th><spring:message code="sala.nombre"/></th><th><spring:message code="sala.tipo"/></th>
                <th><spring:message code="sala.capacidad"/></th><th><spring:message code="sala.proyector"/></th>
                <th><spring:message code="sala.equipamiento"/></th><th><spring:message code="sala.responsable"/></th>
                <th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${salas}" varStatus="st">
                <c:set var="aforoTotal" value="${aforoTotal + s.capacidad}"/>
                <tr class="${st.index % 2 == 0 ? 'par' : 'impar'}">
                    <td><c:out value="${s.nombre}"/></td>
                    <td>
                        <%-- EJ 4.5 - c:url + c:param: añade el contexto y codifica el parámetro --%>
                        <c:url var="urlTipo" value="/salas"><c:param name="tipo" value="${s.tipo}"/></c:url>
                        <a href="${urlTipo}"><spring:message code="tipo.${s.tipo}"/></a>
                    </td>
                    <%-- EJ 4.6 - fmt:formatNumber usa el idioma de Spring: 1.500 en español, 1,500 en inglés --%>
                    <td><fmt:formatNumber value="${s.capacidad}"/> <spring:message code="comun.personas"/></td>
                    <td><spring:message code="${s.proyector ? 'comun.si' : 'comun.no'}"/></td>
                    <td>
                        <%-- EJ 4.5 - Bucle anidado sobre la colección de cada sala; varStatus.last evita la coma final.
                             EJ 4.6 - Los nombres de los equipos son datos de la base de datos: no se traducen. --%>
                        <c:forEach var="e" items="${s.equipamiento}" varStatus="est">
                            <span class="etiqueta"><c:out value="${e.nombre}"/></span>${est.last ? '' : ','}
                        </c:forEach>
                        <c:if test="${empty s.equipamiento}">&mdash;</c:if>
                    </td>
                    <td>
                        <c:if test="${not empty s.emailResponsable}">
                            <a href="mailto:${fn:escapeXml(s.emailResponsable)}"><c:out value="${s.emailResponsable}"/></a>
                        </c:if>
                    </td>
                    <td><a href="<c:url value='/salas/${s.id}/editar'/>"><spring:message code="lista.editar"/></a></td>
                </tr>
            </c:forEach>
            </tbody>
            <tfoot>
            <%-- EJ 4.5 - fn:length funciona con colecciones, arrays y cadenas.
                 EJ 4.6 - Mensaje con argumentos. argumentSeparator=";" porque en inglés el número formateado
                 puede llevar comas (1,500) y la coma es el separador por defecto. --%>
            <fmt:formatNumber var="aforoFormateado" value="${aforoTotal}"/>
            <tr><td colspan="7"><spring:message code="lista.pie" argumentSeparator=";"
                    arguments="${fn:length(salas)};${aforoFormateado}"/></td></tr>
            </tfoot>
        </table>
    </c:otherwise>
</c:choose>
</body>
</html>
