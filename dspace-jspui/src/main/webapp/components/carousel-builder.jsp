<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.List"%>
<%@page import="org.dspace.app.webui.util.CarouselNewsObject"%>

<% 
	List<CarouselNewsObject> news = (List<CarouselNewsObject>) request.getAttribute("carousel_news");
%>

<div class="col-md-12 hidden-xs">
	<div id="carousel" class="carousel slide" data-ride="carousel" style="font-weight:bold; background-color:#000;">
		<ol class="carousel-indicators">
	<%	boolean first = true;
		for(int i = 0; i < news.size(); i++)	
		{	%>
			<li data-target="#carousel" data-slide-to="<%= i %>" class="<%= first ? "active" : "" %>">
			</li>
	<%		first = false;
		}	%>
		</ol>
		<div class="carousel-inner">
		<%	first = true;
			for (CarouselNewsObject no : news)
			{	%>
			<div class="item <%= first ? "active" : "" %>">
				<img src="<%= no.getImage() %>" alt="<%= no.getName() %>" />
			<%	if (StringUtils.isNotBlank(no.getText()))
				{	%>
				<div class="carousel-caption">
				<%	if (StringUtils.isNotBlank(no.getLink()))
					{	%>
						<a href="<%= no.getLink() %>">
							<%= no.getText() %>
						</a>
				<%	}else{	%>
						<%= no.getText() %>
				<%	}	%>
				</div>
			<%	}	%>
			</div>
		<%	first = false;
			}	%>
		</div>
		<a class="left carousel-control" href="#carousel" role="button" data-slide="prev">
			<span class="glyphicon glyphicon-chevron-left">
			</span>
		</a>
		<a class="right carousel-control" href="#carousel" role="button" data-slide="next">
			<span class="glyphicon glyphicon-chevron-right">
			</span>
		</a>
	</div>
</div>