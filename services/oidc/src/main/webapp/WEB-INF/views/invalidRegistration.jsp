<%@ page import="jakarta.servlet.http.HttpServletRequest" %>
<%@ page import="org.apache.cxf.fediz.service.oidc.clients.InvalidRegistration" %>

<%
	InvalidRegistration invalidReg = (InvalidRegistration)request.getAttribute("data");
    String basePath = request.getContextPath() + request.getServletPath();
    if (!basePath.endsWith("/")) {
        basePath += "/";
    }
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Invalid Client Registration</title>
    <link rel="stylesheet" href="<%= basePath %>static/styles.css">
</head>
<body>
<div class="padded">
<h2><%= invalidReg.getMessage() %></h2>
<br/>
<p>Return to <a href="<%=basePath%>console/clients/register">Client registration</a></p>
<p>Return to <a href="<%=basePath%>console/clients">registered Clients</a></p>
</div>
</body>
</html>

