<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<div class="panel panel-info vertical-carousel" data-itemstoshow="12">
	<div class="panel-heading">
		<h3 class="panel-title" id="pathListTitle">
			<fmt:message key="jsp.components.pathlist"/>
		</h3>
	</div>   
	<div class="panel-body">
		<div class="row">
			<div class="col-md-12" style="padding-bottom: 5px;">
				<script>
					$(document).ready(function(){
						$(".pathlist > .list-group-item-description").addClass("hidden");
						$(".pathlist").addClass("thumbnail");
						$(".pathlist span").addClass("hidden");
						$(".pathlist > .list-group-item-heading").addClass("thumbnail-heading");
						$(".path > img").addClass("center-block");
						$("#pathListTitle > div > i").addClass("hidden");
						$(".pathlist").addClass("col-sm-2");
						$(".path-heading").addClass("path-card");
						
						$("#hidePath").click(function(){
							$(".pathlist > .list-group-item-description").removeClass("hidden");
							$(".path").addClass("hidden");
							$(".pathlist").removeClass("thumbnail");
							$(".pathlist span").removeClass("hidden");
							$(".pathlist > .list-group-item-heading").removeClass("thumbnail-heading");
							$("#hidePath").removeClass("btn-default");
							$("#hidePath").addClass("btn-primary");
							$("#showPath").addClass("btn-default");
							$("#showPath").removeClass("btn-primary");
							$(".pathlist").removeClass("col-sm-2");
							$(".path-heading").removeClass("path-card");
				  		});
						
						$("#showPath").click(function(){
							$(".pathlist > .list-group-item-description").addClass("hidden");
							$(".pathlist").addClass("thumbnail");
							$(".pathlist span").addClass("hidden");
							$(".pathlist > .list-group-item-heading").addClass("thumbnail-heading");
							$(".path").removeClass("hidden");
							$("#showPath").removeClass("btn-default");
							$("#showPath").addClass("btn-primary");
							$("#hidePath").removeClass("btn-primary");
							$("#hidePath").addClass("btn-default");
							$(".pathlist").addClass("col-sm-2");
							$(".path-heading").addClass("path-card");
				  		});
					});
				</script>
				<a class="btn btn-primary" role="button" id="showPath"><i class="fa fa-th-large" title="Show as grid"></i></a>
				<a class="btn btn-default" role="button" id="hidePath"><i class="fa fa-list" title="Show as list"></i></a>
			</div>
		</div>
		<div class="list-groups">
		<%	for (PathEntryObject peo : paths) {	%>
			<div class="list-group-item pathlist thumbnail col-sm-2">
				<div class="list-group-item-heading path-heading">
				<%	boolean imagePresent = StringUtils.isNotBlank(peo.getImage());
					if (imagePresent) 
					{	%>
					<div class="media path">
						<img class="media-object pull-left center-block" src="<%= peo.getImage() %>">
					</div>
				<%	}	%>
					<b <%= imagePresent ? "" : "class=\"path-centertext\"" %>><a href="<%= peo.getUrl() %>"><%= peo.getText() %></a></b>
				</div>
			</div>
		<%	}	%>
		</div>
	</div>
</div>