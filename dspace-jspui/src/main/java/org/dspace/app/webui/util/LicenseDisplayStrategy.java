/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.webui.util.ASimpleDisplayStrategy;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

public class LicenseDisplayStrategy extends ASimpleDisplayStrategy
{

    /** log4j category */
    public static final Log log = LogFactory.getLog(LicenseDisplayStrategy.class);

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemId,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException {
        String type = (metadataArray != null && metadataArray.length > 0) ? metadataArray[0].value : null;
        try {
            Context context = UIUtil.obtainContext(hrq);
            Locale locale = context.getCurrentLocale();
            try {
                return I18nUtil.getMessage(
                        "LicenseDisplayStrategy." + type + ".title",
                        new String[] {hrq.getContextPath()}, locale, true);
            } catch (Exception e) {
                return I18nUtil.getMessage("LicenseDisplayStrategy.default.title", locale);
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}