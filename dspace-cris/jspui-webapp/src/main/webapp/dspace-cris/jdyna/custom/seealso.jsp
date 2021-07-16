<%--
 
    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="org.dspace.app.webui.cris.dto.ComponentInfoDTO"%>
<%@page import="it.cilea.osd.jdyna.web.Box"%>
<%@page import="org.dspace.app.cris.model.ACrisObject"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
	<%
	
	Box holder = (Box)request.getAttribute("holder");
	//ComponentInfoDTO info = ((Map<String, ComponentInfoDTO>)(request.getAttribute("componentinfomap"))).get(holder.getShortName());
	//String crisID = info.getCrisID();
	//If some error occurs, check, if *detailscontroller delivers entity object for your entity type
	ACrisObject aco = (ACrisObject) request.getAttribute("entity");
	String prefix = (String) aco.getAuthorityPrefix();
	String gnd = "";
	//define the gnd identifier of your entity	
	if(prefix.contentEquals("rp")){
		gnd = (String) aco.getMetadata("gnd");
	}
	
	//Replace URI of gnd identifier, if exist
	gnd = gnd.replace("http://d-nb.info/gnd/", "");
	gnd = gnd.replace("https://d-nb.info/gnd/", "");
	//Resource URI for rdfa (FIS returns rdf using content negotiation, also the dnb?)
	String resource = "https://professorenkatalog.uni-bamberg.de/cris/" + aco.getPublicPath() + "/" + aco.getCrisID() ;
	String gndresource = "http://d-nb.info/gnd/"+gnd;
	%>

<script>
/* Function to fetch Links from SeeAlso Links from Providers */
var fetchSeealso = function() {
	var boxtitle = '#'+'<%=holder.getShortName()%>';
	var loaded = j('#seealso'+'<%=holder.getShortName()%>').attr("data-loaded");
	if(loaded == "false"){
    j.ajax({
        url : "<%= request.getContextPath() %>/seealso?gnd=<%=gnd%>",
        data:{boxtitle: "<%=holder.getShortName()%>"},
        contentType: "text/html; charset=utf-8",
        beforeSend: function() {
            j('#update-seealso-indicator').show();
        },
        success: function(data) {
            j('#update-seealso-indicator'+'<%=holder.getShortName()%>').hide();
            j('#seealso'+'<%=holder.getShortName()%>').html(data);
            //#seealsoTable is delivered from servlet
        	j('#seealsoTable'+'<%=holder.getShortName()%>').DataTable({
        	    "searching":false,
        	    "info":false,
        	    "lengthChange":false,
        	    "paging":false
            });
        	j('#seealso'+'<%=holder.getShortName()%>').attr('data-loaded', true);
        	j('#seealso'+'<%=holder.getShortName()%>').data('loaded', true);
        	j(boxtitle).show();
        },
        error: function(err) {
        	console.log("no gnd data found");
            j('#update-seealso-indicator').hide();
            j('#seealso'+'<%=holder.getShortName()%>').html("<span class=\"badge\">No further informations based on GND Identifier found.</span>");
            
        }
    });
	}
};
$(document).ready(function() {
	fetchSeealso();
})
</script>
	
<div class="panel-group col-md-12" id="${holder.shortName}" style="display:none;">
	<div class="panel panel-default">
    	<div class="panel-heading">
    	
    		<h4 class="panel-title">
    		<div id='update-seealso-indicator<%=holder.getShortName()%>' style="display: none;" class="loader pull-left"></div>
    		        <a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
        		    <fmt:message key="jsp.dspace.cris.box.header.icon.${holder.shortName}"/>
        		    <spring:message code="${entity.class.simpleName}.box.${holder.shortName}.label" text="${holder.title}"></spring:message>
        		    </a> 
        		    </h4>
    		</div>
    		<!-- <div id="collapseOne${holder.shortName}" class="panel-collapse collapse<c:if test="${holder.collapsed==false}"> in</c:if>"> -->
		<div class="panel-body" resource="<%= resource %>">
		<span content="<%= gndresource %>" property="http://www.w3.org/2002/07/owl#sameAs"></span>
		<div id="seealso<%=holder.getShortName()%>" data-loaded="false"> Check for further informations based on GND Identifier.
		</div>
		</div>
</div>
</div>
