<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.NewsManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.discovery.configuration.DiscoveryViewConfiguration" %>
<%@page import="org.dspace.app.webui.components.MostViewedBean"%>
<%@page import="org.dspace.app.webui.components.MostViewedItem"%>
<%@page import="org.dspace.discovery.SearchUtils"%>
<%@page import="org.dspace.discovery.IGlobalSearchResult"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    String topNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String centralNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-central.html"));
    String sideNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
    MostViewedBean mostViewedItem = (MostViewedBean) request.getAttribute("mostViewedItem");
    MostViewedBean mostCitedItem = (MostViewedBean) request.getAttribute("mostCitedItem");
    MostViewedBean mostViewedBitstream = (MostViewedBean) request.getAttribute("mostDownloadedItem");
    boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"));
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>">
<div class="row">
	<%= topNews %>
</div>
<hr />
<div class="row"> 
	<div class="col-md-8 col-sm-12" style="min-height: 100px;">
		<%-- Fondi di nuova accessione --%>
		<%= centralNews %>
	</div>
	<div class="col-md-4">
		<form id="formsearch-top-menu" method="get" action="<%= request.getContextPath() %>/global-search" scope="search">		
		    <div class="form-group col-md-11">
    	      <input type="text" class="form-control" placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>" name="query" id="tequery"/>
    	    </div>
     	   <div class="col-md-1">
               <button type="submit" class="btn btn-primary pull-right"><span class="glyphicon glyphicon-search"></span></button>
     	   </div>
		</form>
	</div>
	<div class="col-md-4">
 		<hr />
 		<%= sideNews %>
 	</div>
</div>
<hr />
<div class="row">
	<div class="col-md-8 sm-12 pull-<%= isRtl? "right":"left" %>">
	<%
    	int discovery_panel_cols = 8;
    	int discovery_facet_cols = 4;
    	String processorSidebar = (String) request.getAttribute("processorSidebar");
    	String processorGlobal = (String) request.getAttribute("processorGlobal");
          
    if(processorGlobal!=null && processorGlobal.equals("global")) {
		%>
	<%@ include file="discovery/static-globalsearch-component-facet.jsp" %>
	<% } %>        
		  </div>
</div>
<hr />
<div class="row">
	<div class="col-md-4 pull-<%= isRtl? "right":"left" %>">
		<%@ include file="components/most-viewed.jsp" %>	
	</div>
	<div class="col-md-4">
		<%@ include file="components/most-downloaded.jsp" %>
	</div>
	<div class="col-md-4 pull-<%= isRtl? "left":"right" %>">
	    <%@ include file="components/recent-submissions.jsp" %>
	</div>
</div>
<%
if (communities != null && communities.length != 0)
{
%>
<div class="row">
	<div class="col-md-5">		
               <h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>
				<div class="list-group">
<%
	boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.home-page.logos", true);
    for (int i = 0; i < communities.length; i++)
    {
%><div class="list-group-item row">
<%  
		Bitstream logo = communities[i].getLogo();
		if (showLogos && logo != null) { %>
	<div class="col-md-3">
        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" /> 
	</div>
	<div class="col-md-9">
<% } else { %>
	<div class="col-md-12">
<% }  %>		
		<h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></a>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
		<span class="badge pull-right"><%= ic.getCount(communities[i]) %></span>
<%
        }

%>
		</h4>
		<p><%= communities[i].getMetadata("short_description") %></p>
    </div>
</div>                            
	
<%
}
}
    
    if(processorSidebar!=null && processorSidebar.equals("sidebar")) {
	%>
	<div class="col-md-7">
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
	</div>
	<% } %>	
</div>
<div class="row">
	<%@ include file="discovery/static-tagcloud-facet.jsp" %>
</div>
</dspace:layout>
