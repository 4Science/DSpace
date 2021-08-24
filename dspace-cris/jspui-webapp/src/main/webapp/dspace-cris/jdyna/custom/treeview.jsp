<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="org.dspace.kernel.ServiceManager"%>
<%@ page import="org.dspace.utils.DSpace"%>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.cris.discovery.tree.TreeViewConfigurator" %>
<%@ page import="org.dspace.app.cris.configuration.RelationConfiguration" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.cris.model.ACrisObject" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<%
	ACrisObject entity = (ACrisObject)request.getAttribute("entity");
	Boolean lazyload = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.lazyload", false);	
	Boolean showprofile = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.showprofile", false);
	Boolean showall = ConfigurationManager.getBooleanProperty("cris", "cris.tree.widget.showall", false);

	ServiceManager serviceManager = new DSpace().getServiceManager();
	TreeViewConfigurator configurator = serviceManager.getServiceByName(
	        TreeViewConfigurator.class.getName(), TreeViewConfigurator.class);
	String contentCss = "";
    List<RelationConfiguration> relations = configurator.getRelations().get(entity.getTypeText().toLowerCase());
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
<script type="text/javascript">
<!--
	JsTree.load('${root}/api/v1/simple/tree/${entity.crisID}<%= lazyload?"?lazy":"" %>',
			'${root}/api/v1/simple/resource', '${holder.shortName}',
			'data-tree', 'data-window', <%= lazyload %>, <%= showprofile %>, <%= showall %>, '${entity.crisID}');
-->
</script>

<div class="panel-group ${extraCSS}" id="${holder.shortName}">

	<div class="panel panel-default">
		<div class="panel-heading">
			<h4 class="panel-title">
				<a data-toggle="collapse" data-parent="#${holder.shortName}"
					href="#collapseOne${holder.shortName}"> ${holder.title} </a>
			</h4>
		</div>
		<div id="collapseOne${holder.shortName}"
			class="panel-collapse collapse in">
			<div class="panel-body">
				<div class="wrapper">
				  <div class="main col-md-7">
				  	<div id="data-tree">
				  	</div>
				  </div>
				  <div class="sidebar col-md-5">
				  	<div id="data-window">
				  	</div>
				  </div>
				</div>
			</div>
		</div>
	</div>

</div>
