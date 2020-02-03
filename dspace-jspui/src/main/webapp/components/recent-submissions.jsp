<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%
if (submissions != null && submissions.count() > 0)
{
%>

	<div class="col-sm-12">

        <div class="panel panel-info vertical-carousel" data-itemstoshow="12">        
        <div class="panel-heading">
          <h3 class="panel-title" id="recentSubmissionTitle">
          		<fmt:message key="jsp.collection-home.recentsub"/>
          </h3>
       </div>   
	   <div class="panel-body">
    	<div class="row">
<%
	String thumbTag = "";
	if(submissions.getConfiguration().getThumbnail() != null)
	{
		 thumbTag= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".");
	%>

	<div class="col-md-12" style="padding-bottom: 5px;">
	<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"/></script>
	<script>
		$(document).ready(function(){
			$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").addClass("hidden");
			$(".<%= thumbTag %> > img").addClass("center-block");
			$("#recentSubmissionTitle > div > i").addClass("hidden");
			//$(".<%= thumbTag %>").addClass("hidden");
			$(".<%= thumbTag %>list").addClass("col-sm-2");
			$(".<%= thumbTag %>list").css("height", "250px");
			$(".<%= thumbTag %>list").css("min-height", "250px");
			$(".<%= thumbTag %>list > div").addClass("hidable");
			$(".<%= thumbTag %>list > div").addClass("nohover");
			var isHidden = false;
			
			$("#hideThumb").click(function(){
				$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").removeClass("hidden");
				$(".<%= thumbTag %>").addClass("hidden");
				$("#hideThumb").removeClass("btn-default");
				$("#hideThumb").addClass("btn-primary");
				$("#showThumb").addClass("btn-default");
				$("#showThumb").removeClass("btn-primary");
				$(".<%= thumbTag %>list").removeClass("col-sm-2");
				$(".<%= thumbTag %>list").css("height", "");
				$(".<%= thumbTag %>list").css("min-height", "");
				$(".<%= thumbTag %>list > div").removeClass("hidable");
				$(".<%= thumbTag %>list > div").removeClass("nohover");
				isHidden = true;
	  		});
			
			$("#showThumb").click(function(){
				$(".<%= StringUtils.substringAfter(submissions.getConfiguration().getThumbnail(), ".") %>list > .list-group-item-description").addClass("hidden");
				$(".<%= thumbTag %>").removeClass("hidden");
				$("#showThumb").removeClass("btn-default");
				$("#showThumb").addClass("btn-primary");
				$("#hideThumb").removeClass("btn-primary");
				$("#hideThumb").addClass("btn-default");
				$(".<%= thumbTag %>list").addClass("col-sm-2");
				$(".<%= thumbTag %>list").css("height", "250px");
				$(".<%= thumbTag %>list").css("min-height", "250px");
				$(".<%= thumbTag %>list > div").addClass("nohover");
				$(".<%= thumbTag %>list > div").addClass("hidable");
				isHidden = false;
	  		});
			
			$(".hidable").hover(function(){
					$(this).removeClass("nohover");
					$(this).parent().css("height", "");
				}, function(){
					if(!isHidden){
						$(this).addClass("nohover");
						$(this).parent().css("height", "250px");
					}
			});
		});
	</script>
			<a class="btn btn-primary" role="button" id="showThumb"><i class="fa fa-th-large" title="Show as grid"></i></a>
			<a class="btn btn-default" role="button" id="hideThumb""><i class="fa fa-list" title="Show as list"></i></a>
	</div>
<% 	
	}
    if(feedEnabled)
    {
    	%>
	   	<div class="col-md-12">
	   	<div class="pull-right small">
    	
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
	   	</div>
	<%
	    }
%>
	   </div>
	<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : submissions.getRecentSubmissions()) {
		%>
		
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= submissions.getConfiguration() %>"/>
		
		<%
		     }
		%>
		
		<div class="list-group-item <%= thumbTag %>list">
			<div class="list-group-item-heading text-center">
				<div class="media <%= thumbTag %> ">
					<span class="fa fa-archive" style="font-size: 8em;"></span>
				</div>
				<c:set var="recentSubmissionLink"><%=recentSubmissionLink%></c:set>
				<b><a href="<%= request.getContextPath() %>/simple-search?location=<%= recentSubmissionLink  %>&query="><fmt:message key="jsp.recent-submission.simple-search.${recentSubmissionLink}.all"/></a></b>
			</div>
		</div>
		
		</div>
		  </div>
     </div>
    </div>
    
<%
}
%>