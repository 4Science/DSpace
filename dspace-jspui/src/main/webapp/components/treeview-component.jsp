<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.dspace.kernel.ServiceManager"%>
<%@ page import="org.dspace.utils.DSpace"%>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.cris.discovery.tree.TreeViewConfigurator" %>
<%@ page import="org.dspace.app.cris.configuration.RelationConfiguration" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<%
	String tree = (String)request.getAttribute("tree");

	if(StringUtils.isNotBlank(tree)) {
		String contextTree = (String)request.getAttribute("contextTree");
	
		ServiceManager serviceManager = new DSpace().getServiceManager();
		TreeViewConfigurator configurator = serviceManager.getServiceByName(
		        TreeViewConfigurator.class.getName(), TreeViewConfigurator.class);
		String contentCss = "";
	    List<RelationConfiguration> relations = configurator.getRelations().get(contextTree);
		int count = 0;
		// '\f15c' " " attr(data-count-item) " " '\f1e0' " " attr(data-count-ou) " " '\f0e8' " " attr(data-count-pj);
	    for(RelationConfiguration relation : relations) {
	        String attribute = StringUtils.replace(relation.getRelationName(), ".", "-", -1);
	        String icon = configurator.getIcons().get("data-count-" + attribute);
	        if(StringUtils.isBlank(icon)) {
	            icon = "\\f1e0";
	        }
	        if(count>0) {
	            contentCss += "\" \"";
	        }
	        contentCss += "'" + icon + "' \" \" attr(data-count-" + attribute +")";
	        count++;
	    }
%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<link
	href="<%=request.getContextPath()%>/css/jstree/themes/default/style.min.css"
	type="text/css" rel="stylesheet" />
	
<script type="text/javascript"
	src="<%=request.getContextPath()%>/js/jstree/jstree.min.js"></script>
<script type="text/javascript"
	src="<%=request.getContextPath()%>/js/jstree-advanced/jstree.setup.min.js"></script>

<%
	String rootID = (String)request.getAttribute("rootID");
	Boolean lazyload = (Boolean)request.getAttribute("lazyload");
	Boolean showprofile = (Boolean)request.getAttribute("showprofile");
	Boolean showall = (Boolean)request.getAttribute("showall");
%>
	<script type="text/javascript">
	<!--
		JsTree.load('${root}/utilities/treeview/tree/<%= rootID %><%= lazyload?"?lazy":"" %>',
				'${root}/utilities/treeview/resource', '<%= tree %>',
				'data-tree', 'data-window', <%= lazyload %>, <%= showprofile %>, <%= showall %>, '<%= rootID %>');
	-->
	</script>
	<div class="panel-group" id="${tree}">
	<c:set var="titlemessagelabel">jsp.components.tree.<%= tree %></c:set>
	<div class="panel panel-primary">
		<div class="panel-heading">
			<h3 class="panel-title">
				<fmt:message key="jsp.components.tree">					
					<fmt:param><fmt:message key="${titlemessagelabel}"/></fmt:param>
				</fmt:message>
			</h3>
		</div>
		<div class="panel-body">
			<div class="wrapper row">
				<div class="main col-md-7">
					<div id="data-tree"></div>
				</div>
				<div class="sidebar col-md-5">
					<div id="data-window"></div>
				</div>
			</div>
		</div>
	</div>
	</div>
<%
}
%>