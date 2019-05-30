<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri = "http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.cris.model.ACrisObject"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.content.DCDate" %>

<%
    Context context = UIUtil.obtainContext(request);
    List<ACrisObject> privateEntities = (List<ACrisObject>) request.getAttribute("privateEntities");
%>

<div id="private-entities-table">
	<table id="peTable" class="table table-striped table-bordered table-condensed dataTable no-footer" align="center" summary="Table listing private projects">
		<thead>
			<tr>
				<th id="pe01" style="width: 185px;" aria-controls="wfTable" aria-sort="ascending"><fmt:message key="jsp.mydspace.cris.privateentities.type"/></th>
				<th id="pe02" aria-controls="peTable"><fmt:message key="jsp.mydspace.cris.privateentities.name"/></th>
				<th id="pe03" style="width: 185px;" aria-controls="wfTable"><fmt:message key="jsp.mydspace.cris.privateentities.lastmodified"/></th>
			</tr>
		</thead>
		<tbody id="private-entities-table-body">
		<%
			for(ACrisObject entity : privateEntities) {
				String type = I18nUtil.getMessage("jsp.search.results.cris." + entity.getTypeText(), context);
				String name = entity.getName();
				String crisID = entity.getCrisID();
				String lastModified = UIUtil.displayDate(new DCDate(entity.getTimeStampInfo().getLastModificationTime()), true, true, request);
		%>
				<tr>
					<td headers="pe01"><%= type %></td>
					<td headers="pe02"><a href="<%= request.getContextPath() %>/cris/<%= entity.getPublicPath() %>/<%= crisID %>"><%= name %></a></td>
					<td headers="pe03"><%= lastModified %></td>
				</tr>
		<%
			}
		%>
		</tbody>
	</table>
</div>