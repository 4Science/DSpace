<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
	String orcidService = ConfigurationManager.getProperty("cris","external.domainname.authority.service.orcid");
	if (StringUtils.isBlank(orcidService)) {
		orcidService = "https://sandbox.orcid.org/";
	}
	if (!StringUtils.endsWith(orcidService, "/")) {
		orcidService += "/";
	}
%>

<c:if test="${!empty anagraficaObject.anagrafica4view['orcid']}">
	<div class="dynaField" style="min-height:5em;min-width:5em;">
		<span class="dynaLabel" style="width:10em;">ORCID</span>
		<div id="orcidDiv" class="dynaFieldValue">
			<c:choose>
				<c:when test="${empty anagraficaObject.anagrafica4view['system-orcid-token-authenticate']}">
					<span style="min-width: 30em;">
						<a target="_blank" href="<%= orcidService %>${anagraficaObject.anagrafica4view['orcid'][0]}"> <%= orcidService %>${anagraficaObject.anagrafica4view['orcid'][0]}</a>
					</span>
				</c:when>
				<c:otherwise>
					<span style="min-width: 30em;">
						<a target="_blank" href="<%= orcidService %>${anagraficaObject.anagrafica4view['orcid'][0]}"><img src="<%= request.getContextPath() %>/image/orcid_16x16.png" /> <%= orcidService %>${anagraficaObject.anagrafica4view['orcid'][0]}</a>
					</span>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
	<div class="dynaClear">&nbsp;</div>
</c:if>
