<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>


<%	if(paths != null && paths.count() > 0)
	{	%>
<div class="row">
<div class="col-md-12 path-carousel">
<div class="panel panel-info vertical-carousel" data-itemstoshow="<%= (int) request.getAttribute("paths_list_max") %>">
	<div class="panel-heading">
		<h3 class="panel-title" id="pathListTitle">
			<fmt:message key="jsp.components.pathlist"/>
		</h3>
	</div>   
	<div class="panel-body">
		<div class="row">
			<%	String thumbTag = "";
				if(paths.getConfiguration() != null && paths.getConfiguration().getThumbnail() != null)
				{
					 thumbTag= StringUtils.substringAfter(paths.getConfiguration().getThumbnail(), ".");	%>
			<div class="col-md-12" style="padding-bottom: 5px;">
				<script>
					jQuery(document).ready(function(){
						jQuery(".<%= thumbTag %>list > .list-group-item-description").addClass("hidden");
						jQuery(".<%= thumbTag %>list").addClass("thumbnail");
						jQuery(".<%= thumbTag %>list span").addClass("hidden");
						jQuery(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
						jQuery(".<%= thumbTag %> > img").addClass("center-block");
						jQuery(".<%= thumbTag %>list").addClass("col-sm-2");
						jQuery(".<%= thumbTag %>-heading").addClass("<%= thumbTag %>-card");
						jQuery(".<%= thumbTag %>-heading > .media").each(function(){
							var html = jQuery(this).html();
							if(!html.trim())
							{
								jQuery(this).parent().children(".text-path").addClass("path-centertext")
								jQuery(this).remove();
							}
						});
						
						jQuery("#hidePath").click(function(){
							jQuery(".<%= thumbTag %>list > .list-group-item-description").removeClass("hidden");
							jQuery(".<%= thumbTag %>").addClass("hidden");
							jQuery(".<%= thumbTag %>list").removeClass("thumbnail");
							jQuery(".<%= thumbTag %>list span").removeClass("hidden");
							jQuery(".<%= thumbTag %>list > .list-group-item-heading").removeClass("thumbnail-heading");
							jQuery("#hidePath").removeClass("btn-default");
							jQuery("#hidePath").addClass("btn-primary");
							jQuery("#showPath").addClass("btn-default");
							jQuery("#showPath").removeClass("btn-primary");
							jQuery(".<%= thumbTag %>list").removeClass("col-sm-2");
							jQuery(".<%= thumbTag %>-heading").removeClass("<%= thumbTag %>-card");
				  		});
						
						jQuery("#showPath").click(function(){
							jQuery(".<%= thumbTag %>list > .list-group-item-description").addClass("hidden");
							jQuery(".<%= thumbTag %>list").addClass("thumbnail");
							jQuery(".<%= thumbTag %>list span").addClass("hidden");
							jQuery(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
							jQuery(".<%= thumbTag %>").removeClass("hidden");
							jQuery("#showPath").removeClass("btn-default");
							jQuery("#showPath").addClass("btn-primary");
							jQuery("#hidePath").removeClass("btn-primary");
							jQuery("#hidePath").addClass("btn-default");
							jQuery(".<%= thumbTag %>list").addClass("col-sm-2");
							jQuery(".<%= thumbTag %>-heading").addClass("<%= thumbTag %>-card");
				  		});
					});
				</script>
				<a class="btn btn-primary" role="button" id="showPath"><i class="fa fa-th-large" title="Show as grid"></i></a>
				<a class="btn btn-default" role="button" id="hidePath"><i class="fa fa-list" title="Show as list"></i></a>
			</div>
				<%	}	%>
		</div>
		<div class="list-groups">
		<%	for (IGlobalSearchResult obj : paths.getPaths()) {	%>
		
			<dspace:discovery-artifact style="path" artifact="<%= obj %>" view="<%= paths.getConfiguration() %>"/>
		<%	}	%>
		</div>
	</div>
</div>
</div>
</div>
<%	}	%>