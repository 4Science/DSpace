/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.browse.BrowseItem;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.core.Utils;

public class CrisDisplayStrategy implements IDisplayMetadataValueStrategy
{

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem item,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
    {
        ACrisObject crisObject = (ACrisObject) ((BrowseDSpaceObject) item)
                .getBrowsableDSpaceObject();
        String metadata = "-";
        if (metadataArray.length > 0)
        {
            metadata = "<a href=\"" + hrq.getContextPath() + "/cris/"
                    + crisObject.getPublicPath() + "/"
                    + ResearcherPageUtils.getPersistentIdentifier(crisObject)
                    + "\">" + Utils.addEntities(metadataArray[0].value)
                    + "</a>";
        }
        metadata = (emph ? "<strong>" : "") + metadata
                + (emph ? "</strong>" : "");
        return metadata;
    }

    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext)
    {
        String metadata;
        // limit the number of records if this is the author field (if
        // -1, then the limit is the full list)
        boolean truncated = false;
        int loopLimit = metadataArray.length;
        if (limit != -1)
        {
            loopLimit = (limit > metadataArray.length ? metadataArray.length
                    : limit);
            truncated = (limit < metadataArray.length);
        }

        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < loopLimit; j++)
        {
			if (metadataArray[j].confidence != Choices.CF_ACCEPTED) {
				continue;
			}
            buildBrowseLink(hrq, viewFull, browseType, metadataArray,
                    disableCrossLinks, sb, j);
            buildAuthority(hrq, metadataArray, sb, j);
            if (j < (loopLimit - 1))
            {
                if (colIdx != -1) // we are showing metadata in a table row
                                  // (browse or item list)
                {
                    sb.append("; ");
                }
                else
                {
                    // we are in the item tag
                    sb.append("<br />");
                }
            }
        }
        if (truncated)
        {
            String etal = LocaleSupport.getLocalizedMessage(pageContext,
                    "itemlist.et-al");
            sb.append(", " + etal);
        }

        if (colIdx != -1) // we are showing metadata in a table row (browse or
                          // item list)
        {
            metadata = (emph ? "<strong><em>" : "<em>") + sb.toString()
                    + (emph ? "</em></strong>" : "</em>");
        }
        else
        {
            // we are in the item tag
            metadata = (emph ? "<strong>" : "") + sb.toString()
                    + (emph ? "</strong>" : "");
        }

        return metadata;
    }

    private void buildBrowseLink(HttpServletRequest hrq, boolean viewFull,
            String browseType, DCValue[] metadataArray,
            boolean disableCrossLinks, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";
        if (StringUtils.isEmpty(browseType))
        {
            browseType = "author";
        }
        String argument;
        String value;
        argument = "authority";
        String authority = metadataArray[j].authority;
        value = metadataArray[j].value;
        if (viewFull)
        {
            argument = "vfocus";
        }
        try
        {
            startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/browse?type="
                    + browseType + "&amp;" + argument + "="
                    + URLEncoder.encode(authority, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (metadataArray[j].language != null)
        {
            try
            {
                startLink = startLink + "&amp;" + argument + "_lang="
                        + URLEncoder.encode(metadataArray[j].language, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        if ("authority".equals(argument))
        {
            startLink += "\" class=\"authority " + browseType + "\">";
        }
        else
        {
            startLink = startLink + "\">";
        }
        endLink = "</a>";
        sb.append(startLink);
        sb.append(Utils.addEntities(value));
        sb.append(endLink);

    }

    private void buildAuthority(HttpServletRequest hrq,
            DCValue[] metadataArray, StringBuffer sb, int j)
    {
        String startLink = "";
        String endLink = "";

        startLink = "<a target=\"_blank\" href=\"" + hrq.getContextPath() + "/cris/rp/"
                + metadataArray[j].authority;
        startLink += "\" class=\"authority\">";
        endLink = "</a>";
        sb.append(startLink);
        sb.append(" <i class=\"fa fa-user\"></i>");
        sb.append(endLink);
    }

    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, Item item, boolean disableCrossLinks,
            boolean emph, PageContext pageContext) throws JspException
    {
        return null;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, int colIdx, String field,
            DCValue[] metadataArray, BrowseItem browseItem,
            boolean disableCrossLinks, boolean emph, PageContext pageContext)
            throws JspException
    {
        return null;
    }
}
