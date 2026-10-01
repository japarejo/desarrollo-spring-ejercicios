<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- EJ 4.1 - JSTL 3.0: los URI pasan a ser jakarta.tags.* --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="es">
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
    <label for="tipo">Tipo</label>
    <select id="tipo" name="tipo">
        <option value="">Todos</option>
        <c:forEach var="t" items="${tipos}">
            <option value="${t}" ${param.tipo == t.name() ? 'selected' : ''}><c:out value="${t.descripcion}"/></option>
        </c:forEach>
    </select>
    <button type="submit">Filtrar</button>
    <c:if test="${not empty param.tipo}">
        <a href="<c:url value='/salas'/>">Quitar filtro</a>
    </c:if>
</form>

<c:choose>
    <c:when test="${empty salas}">
        <p>${empty param.tipo ? 'No hay salas registradas.' : 'No hay salas de ese tipo.'}</p>
    </c:when>
    <c:otherwise>
        <%-- EJ 4.5 - c:set como acumulador: se va sumando en cada vuelta del bucle --%>
        <c:set var="aforoTotal" value="0"/>
        <table>
            <thead>
            <tr><th>Nombre</th><th>Tipo</th><th>Capacidad</th><th>Proyector</th><th>Equipamiento</th>
                <th>Responsable</th><th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${salas}" varStatus="st">
                <c:set var="aforoTotal" value="${aforoTotal + s.capacidad}"/>
                <tr class="${st.index % 2 == 0 ? 'par' : 'impar'}">
                    <td><c:out value="${s.nombre}"/></td>
                    <td>
                        <%-- EJ 4.5 - c:url + c:param: añade el contexto y codifica el parámetro --%>
                        <c:url var="urlTipo" value="/salas"><c:param name="tipo" value="${s.tipo}"/></c:url>
                        <a href="${urlTipo}"><c:out value="${s.tipo.descripcion}"/></a>
                    </td>
                    <td><fmt:formatNumber value="${s.capacidad}"/> personas</td>
                    <td>${s.proyector ? 'Sí' : 'No'}</td>
                    <td>
                        <%-- EJ 4.5 - Bucle anidado sobre la colección de cada sala; varStatus.last evita la coma final --%>
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
                    <td><a href="<c:url value='/salas/${s.id}/editar'/>">Editar</a></td>
                </tr>
            </c:forEach>
            </tbody>
            <tfoot>
            <%-- EJ 4.5 - fn:length funciona con colecciones, arrays y cadenas --%>
            <tr><td colspan="7">${fn:length(salas)} salas &middot; aforo total:
                <fmt:formatNumber value="${aforoTotal}"/> personas</td></tr>
            </tfoot>
        </table>
    </c:otherwise>
</c:choose>
</body>
</html>
