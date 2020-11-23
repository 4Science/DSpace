<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  list identifiers minted by the MintIdentifierStep. Listing persistent identifiers enables submitters to add proposals
  on how to cite the submission.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="java.util.Iterator" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    String errormessage = StringUtils.trim((String) request.getAttribute("errormessage"));
    String doi = StringUtils.trim((String) request.getAttribute("doi"));
    String handle = StringUtils.trim((String) request.getAttribute("handle"));
    List<String> otherIdentifiers = (List<String>) request.getAttribute("other_identifiers");

    String showIdentifiers = ConfigurationManager.getProperty("webui.submission.list-identifiers");
    if (StringUtils.isEmpty(showIdentifiers))
    {
        showIdentifiers = "all";
    }
    boolean showDOIs = false;
    boolean showHandles = false;
    boolean showOtherIdentifiers = false;
    if (StringUtils.containsIgnoreCase(showIdentifiers, "all"))
    {
        showDOIs = true;
        showHandles = true;
        showOtherIdentifiers = true;
    } else {
        if (StringUtils.containsIgnoreCase(showIdentifiers, "doi"))
        {
            showDOIs = true;
        }
        if (StringUtils.containsIgnoreCase(showIdentifiers, "handle"))
        {
            showHandles = true;
        }
        if (StringUtils.containsIgnoreCase(showIdentifiers, "other"))
        {
            showOtherIdentifiers = true;
        }
    }


    // store if we listed any identifiers
    boolean identifierListed = false;
%>

<dspace:layout style="submission"
               locbar="off"
               navbar="off"
               titlekey="jsp.submit.list-identifiers.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

        <h1><fmt:message key="jsp.submit.list-identifiers.title" /></h1>
        <% if (!StringUtils.isEmpty(errormessage)) { %>
            <div class="alert alert-warning"><fmt:message key="<%= errormessage%>" /></div>
        <% } %>
        <p><fmt:message key="jsp.submit.list-identifiers.info"/></p>

        <% if (showDOIs) { %>
            <div class="row">
            <% if (StringUtils.isEmpty(doi)) {%>
                    <div class="col-sm-12 alert alert-warning"><fmt:message key="jsp.submit.list-identifiers.no_doi_found"/></div>
                <% } else { %>
                    <% identifierListed = true; %>
                    <div class="col-sm-2 col-sm-offset-2"><p><fmt:message key="jsp.submit.list-identifiers.doi"/></p></div>
                    <div class="col-sm-8"><p><a href="<%= doi %>"><%= doi %></a></p></div>
                <% } %>
            </div>
        <% } %>

        <% if (showHandles) { %>
        <div class="row">
            <% if (StringUtils.isEmpty(handle)) {%>
            <div class="col-sm-12 alert alert-warning"><fmt:message key="jsp.submit.list-identifiers.no_handle_found"/></div>
            <% } else { %>
                <% identifierListed = true; %>
                <div class="col-sm-2 col-sm-offset-2"><p><fmt:message key="jsp.submit.list-identifiers.handle"/></p></div>
                <div class="col-sm-8"><p><a href="<%= handle %>"><%= handle %></a></p></div>
            <% } %>
        </div>
        <% } %>

        <%-- We show other identifiers if configured and available.
             We do not show any warning if there are no other identifiers.
             This enables us to show all identifiers by default. --%>
        <% if (showOtherIdentifiers && otherIdentifiers != null && !otherIdentifiers.isEmpty()) {%>
            <div class="row">
                <% identifierListed = true; %>
                <div class="metadataFieldLabel col-sm-2 col-sm-offset-2"><p><fmt:message key="jsp.submit.list-identifiers.other_identifiers"/></p></div>
                <div class="metadataFieldValue col-sm-8"><p><%
                    Iterator<String> identifiers = otherIdentifiers.iterator();
                    while (identifiers.hasNext())
                    {
                        out.print(identifiers.next());
                        if (identifiers.hasNext())
                        {
                            out.println(", ");
                        }
                    }
                %></p>
                </div>
            </div>
        <% } %>

        <div class="row">
            <% if (!identifierListed) { %>
                <div class="alert alert-warning"><fmt:message key="jsp.submit.list-identifiers.no_identifiers_found"/></div>
            <% } else { %>
                <div class="alert alert-info"><fmt:message key="jsp.submit.list-identifiers.info2"/></div>
            <% } %>
        </div>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%  //if not first step, show "Previous" button
            if(!SubmissionController.isFirstStep(request, subInfo))
            { %>
        <div class="col-md-6 pull-right btn-group">
            <input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
            <input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
            <input class="btn btn-primary col-md-4" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />

                    <%  } else { %>
            <div class="col-md-4 pull-right btn-group">
                <input class="btn btn-default col-md-6" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                <input class="btn btn-primary col-md-6" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                <%  }  %>
            </div>
    </form>
</dspace:layout>