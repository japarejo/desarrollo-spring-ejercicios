<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- EJ 4.1 - JSTL 3.0: los URI pasan a ser jakarta.tags.* --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="salas.titulo"/></title>
    <link rel="stylesheet" href="<c:url value='/css/estilo.css'/>">
</head>
<body>
<h1><spring:message code="salas.titulo"/></h1>

<c:if test="${not empty mensaje}">
    <p class="ok"><c:out value="${mensaje}"/></p>
</c:if>

<c:choose>
    <c:when test="${empty salas}">
        <p>No hay salas registradas.</p>
    </c:when>
    <c:otherwise>
        <table>
            <thead>
            <tr><th>Nombre</th><th>Capacidad</th><th>Proyector</th><th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${salas}" varStatus="st">
                <tr class="${st.index % 2 == 0 ? 'par' : 'impar'}">
                    <td><c:out value="${s.nombre}"/></td>
                    <td><fmt:formatNumber value="${s.capacidad}"/> personas</td>
                    <td>${s.proyector ? 'Sí' : 'No'}</td>
                    <td><a href="<c:url value='/salas/${s.id}/editar'/>">Editar</a></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<p><a href="<c:url value='/salas/nueva'/>">Nueva sala</a> ·
   <a href="<c:url value='/swagger-ui.html'/>">API REST (Swagger UI)</a></p>
</body>
</html>
