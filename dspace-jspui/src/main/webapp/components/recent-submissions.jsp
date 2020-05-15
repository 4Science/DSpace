<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%
String recentSubmissionLink = (String) request.getAttribute("recent.link");
if (submissions != null && submissions.count() > 0)
{
	String thumbTag = "";
	if(submissions.getConfiguration() != null && submissions.getConfiguration().getThumbnail() != null)
	{
		 thumbTag= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".");
	%>
	<div class="panel panel-info vertical-carousel" data-itemstoshow="12">
        <div class="panel-heading">
          <h3 class="panel-title" id="recentSubmissionTitle">
          		<fmt:message key="jsp.collection-home.recentsub"/>
          </h3>
       </div>   
	   <div class="panel-body">
    	<div class="row">
	<div class="col-md-12" style="padding-bottom: 5px;">
	<script>
		$(document).ready(function(){
			$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").addClass("hidden");
			$(".<%= thumbTag %>list").addClass("thumbnail");
			$(".<%= thumbTag %>list span").addClass("hidden");
			$(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
			$(".<%= thumbTag %> > img").addClass("center-block");
			$("#recentSubmissionTitle > div > i").addClass("hidden");
			$(".<%= thumbTag %>list").addClass("col-sm-2");
			$(".<%= thumbTag %>list > .list-group-item-heading").css("height", "300px");
			
			$("#hideThumb").click(function(){
				$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").removeClass("hidden");
				$(".<%= thumbTag %>").addClass("hidden");
				$(".<%= thumbTag %>list").removeClass("thumbnail");
				$(".<%= thumbTag %>list span").removeClass("hidden");
				$(".<%= thumbTag %>list > .list-group-item-heading").removeClass("thumbnail-heading");
				$("#hideThumb").removeClass("btn-default");
				$("#hideThumb").addClass("btn-primary");
				$("#showThumb").addClass("btn-default");
				$("#showThumb").removeClass("btn-primary");
				$(".<%= thumbTag %>list").removeClass("col-sm-2");
				$(".<%= thumbTag %>list > .list-group-item-heading").css("height", "");
	  		});
			
			$("#showThumb").click(function(){
				$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").addClass("hidden");
				$(".<%= thumbTag %>list").addClass("thumbnail");
				$(".<%= thumbTag %>list span").addClass("hidden");
				$(".<%= thumbTag %>list > .list-group-item-heading").addClass("thumbnail-heading");
				$(".<%= thumbTag %>").removeClass("hidden");
				$("#showThumb").removeClass("btn-default");
				$("#showThumb").addClass("btn-primary");
				$("#hideThumb").removeClass("btn-primary");
				$("#hideThumb").addClass("btn-default");
				$(".<%= thumbTag %>list").addClass("col-sm-2");
				$(".<%= thumbTag %>list > .list-group-item-heading").css("height", "300px");
	  		});
		});
	</script>
			<a class="btn btn-primary" role="button" id="showThumb"><i class="fa fa-th-large" title="Show as grid"></i></a>
			<a class="btn btn-default" role="button" id="hideThumb""><i class="fa fa-list" title="Show as list"></i></a>
<% 	
	} else { %>
        <div class="panel panel-info vertical-carousel" data-itemstoshow="3">
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.collection-home.recentsub"/>
          </h3>
       </div>
	   <div class="panel-body">
    	<div class="row">
    		<div class="col-md-12">
	<% }
    if(feedEnabled)
    {
    	%>
	   	<div class="pull-right small" style="padding-top: 10px;">
    	
    	<%
	    	String[] fmts = feedData.substring(feedData.indexOf(':')+1).split(",");
	    	String icon = null;
	    	int width = 0;
	    	for (int j = 0; j < fmts.length; j++)
	    	{
	%>
		<c:set var="fmtkey">jsp.recentsub.rss.<%= fmts[j] %></c:set>
	    <a href="<%= request.getContextPath() %>/feed/<%= fmts[j] %>/site"><i alt="RSS Feeds" class="fa fa-rss"></i> 
	    <sup class="small"><fmt:message key="${fmtkey}" /></sup></a>
	<%
	    	}
	    	%>
	</div>
	<%
	    }
%>
	   </div>
	</div>
	<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : submissions.getRecentSubmissions()) {
		%>
		
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= submissions.getConfiguration() %>"/>
		
		<%
		     }
		%>
		<% if (StringUtils.isNotBlank(recentSubmissionLink) && StringUtils.isNotBlank(thumbTag)) { %>
			<div class="list-group-item <%= thumbTag %>list thumbnail col-sm-2">
				<div class="list-group-item-heading thumbnail-heading text-center">
					<div class="media <%= thumbTag %> ">
						<i class="fa fa-archive" style="font-size: 8em; margin-top: 50px; color: #fff;"></i>
					</div>
					<br>
					<c:set var="recentSubmissionLink"><%=recentSubmissionLink%></c:set>
					<b><a href="<%= request.getContextPath() %>/simple-search?location=<%= recentSubmissionLink  %>&query="><fmt:message key="jsp.recent-submission.simple-search.${recentSubmissionLink}.all"/></a></b>
				</div>
			</div>
		<% } %>
		
		</div>
		  </div>
     </div>
    
<%
}
%>