<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Review file upload info
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.dspace.authorize.AuthorizeManager"%>
<%@page import="java.util.Date"%>
<%@page import="org.dspace.authorize.ResourcePolicy"%>
<%@page import="java.util.List"%>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="org.dspace.identifier.IdentifierService" %>
<%@ page import="org.dspace.utils.DSpace" %>
<%@ page import="org.dspace.identifier.Handle" %>
<%@ page import="org.dspace.identifier.DOI" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.dspace.app.webui.submit.JSPStepManager" %>
<%@ page import="org.dspace.identifier.DOIIdentifierProvider" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.dspace.handle.HandleManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    // get the item
    Item item = subInfo.getSubmissionItem().getItem();

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
        showDOIs =true;
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

    // We'd like to configure if DOIs, Handles, other identifiers or any combination of these should be shown.
    // Therefore we'll to load dois, handles and other identifiers separately.
    String doi = null;
    String handle = null;
    List<String> otherIdentifiers = new LinkedList<String>();

    // retrieve the identifierService to load available identifierss
    IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
    if (identifierService != null)
    {
        try
        {
            if (showHandles)
            {
                // load handles
                handle = identifierService.lookup(context, item, Handle.class);
            }
        }
        catch (Exception ex)
        {
            // nothing to do here
        }
        try
        {
            if (showDOIs)
            {
                // load DOIs
                doi = identifierService.lookup(context, item, DOI.class);
            }
        }
        catch (Exception ex)
        {
            // nothing to do here
        }
        try
        {
            if (showOtherIdentifiers)
            {
                for (String identifier : identifierService.lookup(context, item))
                {
                    // load everything we haven't loaded yet
                    if (! StringUtils.equals(handle, identifier) && ! StringUtils.equals(doi, identifier) && !StringUtils.contains(identifier, "doi:"))
                    {
                        otherIdentifiers.add(identifier);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            // nothing to do here
        }

        // format doi and handle, if we got any. Don't do this before we compared them to possible other identifiers.
        if (!StringUtils.isEmpty(doi))
        {
            // remove the 'doi:' prefix
            if (StringUtils.startsWithIgnoreCase(doi, DOI.SCHEME))
            {
                doi = doi.substring(DOI.SCHEME.length());
            }
            // add the doi resolver
            doi = "https://doi.org/" + doi;
        }
        if (!StringUtils.isEmpty(handle))
        {
            handle = HandleManager.getCanonicalForm(handle);
        }
    }

    // Did we found any identifiers or shall we print an error message?
    boolean foundIdentifiers = true;
    if (StringUtils.isEmpty(doi) && StringUtils.isEmpty(handle) && otherIdentifiers.isEmpty())
    {
        foundIdentifiers = false;
    }
%>


<%-- ====================================================== --%>
<%--                PERSISTENT IDENTIFIER                   --%>
<%-- ====================================================== --%>

<div class="col-md-10">
    <div class="row">
        <% if (foundIdentifiers) { %>
            <% if (!StringUtils.isEmpty(doi))
            { %>
                <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.identifiers.doi"/></span>
                <span class="metadataFieldValue col-md-8"><a href="<%= doi %>"><%= doi %></a></span>
            <%}%>

            <% if (!StringUtils.isEmpty(handle))
            { %>
                <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.identifiers.handle"/></span>
                <span class="metadataFieldValue col-md-8"><a href="<%= handle %>"><%= handle %></a></span>
            <%}%>

            <% if (otherIdentifiers != null && !otherIdentifiers.isEmpty())
            { %>
                <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.identifiers.other_identifiers"/></span>
                <span class="metadataFieldValue col-md-8">
                    <%
                        Iterator<String> identifiers = otherIdentifiers.iterator();
                        while (identifiers.hasNext())
                        {
                            out.print(identifiers.next());
                            if (identifiers.hasNext())
                            {
                                out.println(", ");
                            }
                        }
                    %>
                </span>
            <% } %>
        <% } else { %>
            <div class="col-md-12 text-center"><fmt:message key="jsp.submit.review.no_identifiers_found"/></div>
        <% } %>
    </div>
</div>