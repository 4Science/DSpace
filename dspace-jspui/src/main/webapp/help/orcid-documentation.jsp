<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
	String dspaceName = ConfigurationManager.getProperty("dspace.name");
%>

<!DOCTYPE html>
<html>
<head>
<title><fmt:message key="jsp.help.orcid-documentation.title" /></title>
<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css" type="text/css" />
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css" type="text/css"/>
</head>
<body class="help">
	<table>
		<tr>
			<td class="leftAlign"><strong><fmt:message key="jsp.help.orcid-documentation.title" /></strong></td>
			<td class="rightAlign"><a href="index.html#contents"><fmt:message key="jsp.help.orcid-documentation.top" /></a></td>
		</tr>
	</table>
	<br>
	<p>
		<a title="orcid.org" href="https://orcid.org" target="_blank"><img class="pull-left" style="margin-right:10px" src="<%= request.getContextPath() %>/image/orcid_128x128.png"></a>
		<fmt:message key="jsp.help.orcid-documentation.introduction.paragraph1">
			<fmt:param><%= request.getContextPath() %></fmt:param>
			<fmt:param><%= dspaceName %></fmt:param>
		</fmt:message>
	</p>
	<p><fmt:message key="jsp.help.orcid-documentation.introduction.paragraph2" /></p>
	<p><img title="ORCID iD display example" alt="ORCID iD display example" src="<%= request.getContextPath() %>/image/orcid-rp.png" width="500"></p>
	<br>
	<h3><fmt:message key="jsp.help.orcid-documentation.section1.title" /></h3>
	<p>
		<fmt:message key="jsp.help.orcid-documentation.section1.paragraph1">
			<fmt:param><%= dspaceName %></fmt:param>
		</fmt:message>
	</p>
	<p><img style="display:block; margin-left:auto; margin-right:auto;" title="Claim Profile Button" alt="Claim Profile Button" src="<%= request.getContextPath() %>/image/orcid-claim-button.png" width="238" height="36"></p>
	<p>
		<fmt:message key="jsp.help.orcid-documentation.section1.paragraph2">
			<fmt:param><%= request.getContextPath() %></fmt:param>
			<fmt:param><%= dspaceName %></fmt:param>
		</fmt:message>
	</p>
	<p><img style="display:block; margin-left:auto; margin-right:auto;" title="Connect to ORCID" alt="Connect to ORCID" src="<%= request.getContextPath() %>/image/orcid-connect-rp.png" width="392" height="167"></p>
	<p><fmt:message key="jsp.help.orcid-documentation.section1.paragraph3" /></p>
	<br>
	<h3><fmt:message key="jsp.help.orcid-documentation.section2.title" /></h3>
	<p>
		<fmt:message key="jsp.help.orcid-documentation.section2.paragraph1">
			<fmt:param><%= dspaceName %></fmt:param>
		</fmt:message>
	</p>
	<p><img style="display:block; margin-left:auto; margin-right:auto;" title="Selection list" alt="Selection list" src="<%= request.getContextPath() %>/image/orcid-lookup.png" width="392" height="68"></p>
	<br>
	<h3><fmt:message key="jsp.help.orcid-documentation.section3.title" /></h3>
	<p><fmt:message key="jsp.help.orcid-documentation.section3.paragraph1" /></p>
	<p><fmt:message key="jsp.help.orcid-documentation.section3.paragraph2" /></p>
	<fmt:message key="jsp.help.orcid-documentation.section3.paragraph3" />
	<p>
		<fmt:message key="jsp.help.orcid-documentation.section3.paragraph4">
			<fmt:param><%= dspaceName %></fmt:param>
		</fmt:message>
	</p>
	<p><img title="Certified ORCID Service Provider" alt="Certified ORCID Service Provider" src="<%= request.getContextPath() %>/image/orcid-badge.png" width="250" height="100"></p>
</body>