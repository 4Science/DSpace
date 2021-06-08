<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<div class="panel panel-info vertical-carousel" data-itemstoshow="<%= ConfigurationManager.getIntProperty("path-list.results.show", 12) %>">
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
					$(document).ready(function(){
						$(".<%= thumbTag %>list > .list-group-item-description").addClass("hidden");
						$(".<%= thumbTag %>list").addClass("thumbnail");
						$(".<%= thumbTag %>list span").addClass("hidden");
						$(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
						$(".<%= thumbTag %> > img").addClass("center-block");
						$(".<%= thumbTag %>list").addClass("col-sm-2");
						$(".<%= thumbTag %>-heading").addClass("<%= thumbTag %>-card");
						$(".<%= thumbTag %>-heading > .media").each(function(){
							var html = $(this).html();
							if(!html.trim())
							{
								$(this).parent().children(".text-path").addClass("path-centertext")
								$(this).remove();
							}
						});
						
						$("#hidePath").click(function(){
							$(".<%= thumbTag %>list > .list-group-item-description").removeClass("hidden");
							$(".<%= thumbTag %>").addClass("hidden");
							$(".<%= thumbTag %>list").removeClass("thumbnail");
							$(".<%= thumbTag %>list span").removeClass("hidden");
							$(".<%= thumbTag %>list > .list-group-item-heading").removeClass("thumbnail-heading");
							$("#hidePath").removeClass("btn-default");
							$("#hidePath").addClass("btn-primary");
							$("#showPath").addClass("btn-default");
							$("#showPath").removeClass("btn-primary");
							$(".<%= thumbTag %>list").removeClass("col-sm-2");
							$(".<%= thumbTag %>-heading").removeClass("<%= thumbTag %>-card");
				  		});
						
						$("#showPath").click(function(){
							$(".<%= thumbTag %>list > .list-group-item-description").addClass("hidden");
							$(".<%= thumbTag %>list").addClass("thumbnail");
							$(".<%= thumbTag %>list span").addClass("hidden");
							$(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
							$(".<%= thumbTag %>").removeClass("hidden");
							$("#showPath").removeClass("btn-default");
							$("#showPath").addClass("btn-primary");
							$("#hidePath").removeClass("btn-primary");
							$("#hidePath").addClass("btn-default");
							$(".<%= thumbTag %>list").addClass("col-sm-2");
							$(".<%= thumbTag %>-heading").addClass("<%= thumbTag %>-card");
				  		});
					});
				</script>
				<a class="btn btn-primary" role="button" id="showPath"><i class="fa fa-th-large" title="Show as grid"></i></a>
				<a class="btn btn-default" role="button" id="hidePath"><i class="fa fa-list" title="Show as list"></i></a>
			</div>
				<%	}	%>
		</div>
		<div class="list-groups">
		<%	for (IGlobalSearchResult obj : paths.getRecentSubmissions()) {	%>
		
			<dspace:discovery-artifact style="path" artifact="<%= obj %>" view="<%= paths.getConfiguration() %>"/>
		<%	}	%>
		</div>
	</div>
</div>